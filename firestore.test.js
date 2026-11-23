const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_osint_01";
const BOB_UID = "bob_osint_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validWebsitePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 5000);
  return {
    userId: uid,
    name: "Shodan",
    url: "https://www.shodan.io",
    domain: "shodan.io",
    description: "Search engine for Internet-connected devices and infrastructure.",
    purpose: "Infrastructure & attack surface reconnaissance",
    categories: ["OSINT", "Cybersecurity"],
    keyFeatures: ["Port scanning index", "Banner search", "API access"],
    accessType: "LIMITED_ACCESS",
    pricingModel: "Freemium",
    freeTierLimitations: "Limited query results without membership",
    registrationRequired: "OPTIONAL",
    openSource: false,
    aiPowered: false,
    skillLevel: "PROFESSIONAL",
    recommendationScore: 95,
    recommendationLabel: "Best for Infrastructure OSINT",
    whyRecommended: "Authoritative index of internet-facing services and TLS metadata.",
    advantages: ["Rich banner indexing", "Verified metadata"],
    disadvantages: ["Query credits required for deep pagination"],
    whoShouldUse: "Security analysts and threat researchers",
    verifiedFacts: "HTTP 200 OK verified on shodan.io",
    sourceType: "CURATED",
    savedByUser: false,
    notes: "",
    tags: ["recon", "network"],
    verificationStatus: "VERIFIED",
    httpStatusCode: 200,
    lastCheckedAt: "2026-11-23T02:00:00Z",
    visitCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection(`users/${ALICE_UID}/websites`).get());
  await assertFails(
    unauth
      .collection(`users/${ALICE_UID}/websites`)
      .doc("shodan_io")
      .set(validWebsitePayload(ALICE_UID))
  );
  await assertFails(unauth.collection(`users/${ALICE_UID}/collections`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/searchHistory`).get());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice resources", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/websites`)
      .doc("shodan_io")
      .set(validWebsitePayload(ALICE_UID))
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/websites`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/websites`).doc("shodan_io").get());
  await assertFails(
    bobDb
      .collection(`users/${ALICE_UID}/websites`)
      .doc("shodan_io")
      .update({ savedByUser: true, updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/websites`).doc("shodan_io").delete());
});

test("Owner website create, partial updates, and security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`users/${ALICE_UID}/websites`).doc("shodan_io");

  await assertSucceeds(ref.set(validWebsitePayload(ALICE_UID)));

  // Partial update: toggle savedByUser
  await assertSucceeds(
    ref.update({
      savedByUser: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial update: edit notes, tags, categories, description
  await assertSucceeds(
    ref.update({
      notes: "Essential for ASN enumeration",
      tags: ["asn", "certs"],
      categories: ["OSINT"],
      description: "Updated description",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial update: visit tracking with optional lastVisitedAt
  await assertSucceeds(
    ref.update({
      visitCount: 1,
      lastVisitedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow field / @DocumentId 'id' rejected
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/websites`)
      .doc("bad_doc")
      .set(validWebsitePayload(ALICE_UID, { id: "bad_doc" }))
  );

  // Future timestamp rejected
  await assertFails(
    ref.update({
      savedByUser: false,
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    ref.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid accessType rejected
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/websites`)
      .doc("bad_access")
      .set(validWebsitePayload(ALICE_UID, { accessType: "TRIAL" }))
  );
});

test("Collections, categories, searchHistory, AI conversations & preferences", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Collection
  const colRef = aliceDb.collection(`users/${ALICE_UID}/collections`).doc("col_1");
  await assertSucceeds(
    colRef.set({
      userId: ALICE_UID,
      name: "Threat Intel",
      description: "Core recon websites",
      websiteIds: ["shodan_io"],
      colorHex: "#3B82F6",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    colRef.update({
      websiteIds: ["shodan_io", "censys_io"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Custom Category
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/categories`).doc("cat_1").set({
      userId: ALICE_UID,
      name: "Maritime OSINT",
      description: "AIS & vessel tracking tools",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Search History (immutable after create)
  const histRef = aliceDb.collection(`users/${ALICE_UID}/searchHistory`).doc("hist_1");
  await assertSucceeds(
    histRef.set({
      userId: ALICE_UID,
      query: "free government dataset portals",
      filtersSummary: "FREE",
      resultWebsiteIds: ["data_gov"],
      resultCount: 1,
      searchSource: "Gemini AI + HTTP Probe",
      createdAt: now,
    })
  );
  await assertFails(histRef.update({ query: "tampered" }));

  // AI Conversation & Message subcollection
  const convRef = aliceDb.collection(`users/${ALICE_UID}/aiConversations`).doc("conv_1");
  await assertSucceeds(
    convRef.set({
      userId: ALICE_UID,
      title: "Compare OSINT Tools",
      contextLabel: "AI Search",
      messageCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    convRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      content: "Which of these tools is completely free?",
      contextSummary: "Shodan, Censys",
      createdAt: now,
    })
  );
  // Orphaned message without parent conversation fails
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/aiConversations/missing_conv/messages`)
      .doc("msg_orphan")
      .set({
        userId: ALICE_UID,
        role: "user",
        content: "Orphan",
        contextSummary: "",
        createdAt: now,
      })
  );
  // Cross-user message read fails
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/aiConversations/conv_1/messages`).doc("msg_1").get()
  );
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/aiConversations/conv_1/messages`).get()
  );

  // Preferences
  const prefRef = aliceDb.collection(`users/${ALICE_UID}/preferences`).doc("appSettings");
  await assertSucceeds(
    prefRef.set({
      userId: ALICE_UID,
      darkTheme: true,
      preferredModel: "gemini-3.8-flash",
      autoSaveDiscovered: true,
      autoVerifyLinks: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    prefRef.update({
      darkTheme: false,
      preferredModel: "gemini-3.5-flash-lite",
      autoSaveDiscovered: true,
      autoVerifyLinks: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
