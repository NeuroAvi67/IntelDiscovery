package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AiConversation
import com.example.data.model.AiMessage
import com.example.data.model.CustomCategory
import com.example.data.model.ResearchCollection
import com.example.data.model.SearchHistoryEntry
import com.example.data.model.UserPreferences
import com.example.data.model.WebsiteResource
import com.example.data.model.domainToSlug
import com.example.data.remote.DomainProbeResult
import com.example.data.seed.CuratedDirectorySeed
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OsintRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim('_')
        return cleaned.ifBlank { "doc_${System.currentTimeMillis()}" }.take(120)
    }

    fun observeWebsites(userId: String): Flow<List<WebsiteResource>> = flow {
        val path = "users/$userId/websites"
        emitAll(
            db.collection("users").document(userId).collection("websites")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WebsiteResource::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCollections(userId: String): Flow<List<ResearchCollection>> = flow {
        val path = "users/$userId/collections"
        emitAll(
            db.collection("users").document(userId).collection("collections")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ResearchCollection::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndRethrow(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeCustomCategories(userId: String): Flow<List<CustomCategory>> = flow {
        val path = "users/$userId/categories"
        emitAll(
            db.collection("users").document(userId).collection("categories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomCategory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndRethrow(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntry>> = flow {
        val path = "users/$userId/searchHistory"
        emitAll(
            db.collection("users").document(userId).collection("searchHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SearchHistoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndRethrow(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeConversations(userId: String): Flow<List<AiConversation>> = flow {
        val path = "users/$userId/aiConversations"
        emitAll(
            db.collection("users").document(userId).collection("aiConversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiConversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndRethrow(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<AiMessage>> = flow {
        val safeId = sanitizeId(conversationId)
        val path = "users/$userId/aiConversations/$safeId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("aiConversations").document(safeId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapAndRethrow(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observePreferences(userId: String): Flow<UserPreferences> = flow {
        val path = "users/$userId/preferences/appSettings"
        emitAll(
            db.collection("users").document(userId)
                .collection("preferences").document("appSettings")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserPreferences::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserPreferences(id = "appSettings", userId = userId)
                }
                .catch { error ->
                    unwrapAndRethrow(error, OperationType.GET, path, userId)
                }
        )
    }

    suspend fun seedCuratedDirectoryIfNeeded(userId: String): Result<Unit> {
        val path = "users/$userId/websites"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val col = db.collection("users").document(uid).collection("websites")
            val existing = col.limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                CuratedDirectorySeed.initialWebsites(uid).forEach { site ->
                    val docId = sanitizeId(site.id.ifBlank { domainToSlug(site.domain) })
                    batch.set(col.document(docId), websiteCreatePayload(uid, site))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertDiscoveredWebsites(
        userId: String,
        websites: List<WebsiteResource>,
        autoSave: Boolean
    ): Result<List<WebsiteResource>> {
        val path = "users/$userId/websites"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val col = db.collection("users").document(uid).collection("websites")
            val savedList = mutableListOf<WebsiteResource>()
            for (site in websites) {
                val docId = sanitizeId(domainToSlug(site.domain.ifBlank { site.url }))
                val docRef = col.document(docId)
                val existingSnap = docRef.get().await()
                if (!existingSnap.exists()) {
                    val toCreate = site.copy(id = docId, userId = uid)
                    if (autoSave) {
                        docRef.set(websiteCreatePayload(uid, toCreate)).await()
                    }
                    savedList.add(toCreate)
                } else {
                    // Preserve user's existing savedByUser, notes, tags, visitCount, createdAt
                    val existingSite = existingSnap.toObject(
                        WebsiteResource::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (autoSave) {
                        docRef.update(
                            mapOf(
                                "name" to site.name.take(140).ifBlank { existingSite?.name ?: site.domain },
                                "description" to site.description.take(1500),
                                "purpose" to site.purpose.take(500),
                                "categories" to site.categories.take(10),
                                "keyFeatures" to site.keyFeatures.take(15),
                                "accessType" to site.accessEnum.wireValue,
                                "pricingModel" to site.pricingModel.take(300),
                                "freeTierLimitations" to site.freeTierLimitations.take(500),
                                "registrationRequired" to site.registrationEnum.wireValue,
                                "openSource" to site.openSource,
                                "aiPowered" to site.aiPowered,
                                "skillLevel" to site.skillEnum.wireValue,
                                "recommendationScore" to site.recommendationScore.coerceIn(0, 100),
                                "recommendationLabel" to site.recommendationLabel.take(120),
                                "whyRecommended" to site.whyRecommended.take(1000),
                                "advantages" to site.advantages.take(10),
                                "disadvantages" to site.disadvantages.take(10),
                                "whoShouldUse" to site.whoShouldUse.take(500),
                                "verifiedFacts" to site.verifiedFacts.take(600),
                                "verificationStatus" to site.verificationEnum.wireValue,
                                "httpStatusCode" to site.httpStatusCode.coerceIn(0, 999),
                                "lastCheckedAt" to site.lastCheckedAt.take(64),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    savedList.add(
                        site.copy(
                            id = docId,
                            userId = uid,
                            savedByUser = existingSite?.savedByUser ?: site.savedByUser,
                            notes = existingSite?.notes ?: site.notes,
                            tags = existingSite?.tags ?: site.tags,
                            visitCount = existingSite?.visitCount ?: site.visitCount,
                            createdAt = existingSite?.createdAt
                        )
                    )
                }
            }
            Result.success(savedList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSavedWebsite(userId: String, website: WebsiteResource): Result<Boolean> {
        val docId = sanitizeId(website.id.ifBlank { domainToSlug(website.domain) })
        val path = "users/$userId/websites/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("websites").document(docId)
            val newSaved = !website.savedByUser
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "savedByUser" to newSaved,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(websiteCreatePayload(uid, website.copy(savedByUser = newSaved))).await()
            }
            Result.success(newSaved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWebsiteMetadata(
        userId: String,
        websiteId: String,
        notes: String,
        tags: List<String>,
        categories: List<String>,
        description: String
    ): Result<Unit> {
        val docId = sanitizeId(websiteId)
        val path = "users/$userId/websites/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("websites").document(docId)
                .update(
                    mapOf(
                        "notes" to notes.take(2000),
                        "tags" to tags.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(15),
                        "categories" to categories.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(10),
                        "description" to description.take(1500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWebsiteVerification(
        userId: String,
        website: WebsiteResource,
        probe: DomainProbeResult
    ): Result<Unit> {
        val docId = sanitizeId(website.id.ifBlank { domainToSlug(website.domain) })
        val path = "users/$userId/websites/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("websites").document(docId)
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "verificationStatus" to probe.status.wireValue,
                        "httpStatusCode" to probe.httpStatusCode.coerceIn(0, 999),
                        "lastCheckedAt" to probe.checkedAtIso.take(64),
                        "verifiedFacts" to probe.factSummary.take(600),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordWebsiteVisit(userId: String, website: WebsiteResource): Result<Unit> {
        val docId = sanitizeId(website.id.ifBlank { domainToSlug(website.domain) })
        val path = "users/$userId/websites/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("websites").document(docId)
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "visitCount" to FieldValue.increment(1L),
                        "lastVisitedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(websiteCreatePayload(uid, website.copy(visitCount = 1))).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWebsite(userId: String, websiteId: String): Result<Unit> {
        val docId = sanitizeId(websiteId)
        val path = "users/$userId/websites/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("websites").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createCollection(
        userId: String,
        name: String,
        description: String,
        colorHex: String,
        initialWebsiteIds: List<String> = emptyList()
    ): Result<String> {
        val path = "users/$userId/collections"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(name.trim().isNotEmpty()) { "Collection name cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("collections").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to name.trim().take(100),
                    "description" to description.trim().take(500),
                    "websiteIds" to initialWebsiteIds.distinct().take(100),
                    "colorHex" to colorHex.take(20),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCollectionWebsites(
        userId: String,
        collectionId: String,
        websiteIds: List<String>
    ): Result<Unit> {
        val docId = sanitizeId(collectionId)
        val path = "users/$userId/collections/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("collections").document(docId)
                .update(
                    mapOf(
                        "websiteIds" to websiteIds.distinct().take(100),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCollection(userId: String, collectionId: String): Result<Unit> {
        val docId = sanitizeId(collectionId)
        val path = "users/$userId/collections/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("collections").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createCustomCategory(
        userId: String,
        name: String,
        description: String
    ): Result<Unit> {
        val path = "users/$userId/categories"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val trimmed = name.trim().take(60)
            require(trimmed.isNotEmpty()) { "Category name cannot be empty" }
            val docId = sanitizeId(trimmed.lowercase())
            db.collection("users").document(uid).collection("categories").document(docId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "name" to trimmed,
                        "description" to description.trim().take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomCategory(userId: String, categoryId: String): Result<Unit> {
        val docId = sanitizeId(categoryId)
        val path = "users/$userId/categories/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("categories").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordSearchHistory(
        userId: String,
        query: String,
        filtersSummary: String,
        resultWebsiteIds: List<String>,
        searchSource: String
    ): Result<Unit> {
        val path = "users/$userId/searchHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val trimmed = query.trim().take(500)
            require(trimmed.isNotEmpty())
            val docRef = db.collection("users").document(uid).collection("searchHistory").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "query" to trimmed,
                    "filtersSummary" to filtersSummary.take(300),
                    "resultWebsiteIds" to resultWebsiteIds.take(30),
                    "resultCount" to resultWebsiteIds.size.coerceIn(0, 100),
                    "searchSource" to searchSource.take(80).ifBlank { "Gemini AI" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSearchHistoryEntry(userId: String, historyId: String): Result<Unit> {
        val docId = sanitizeId(historyId)
        val path = "users/$userId/searchHistory/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("searchHistory").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearSearchHistory(userId: String): Result<Unit> {
        val path = "users/$userId/searchHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docs = db.collection("users").document(uid).collection("searchHistory").get().await()
            val batch = db.batch()
            docs.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureConversationAndAppendMessage(
        userId: String,
        existingConversationId: String?,
        title: String,
        contextLabel: String,
        role: String,
        content: String,
        contextSummary: String
    ): Result<String> {
        val path = "users/$userId/aiConversations"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val convCol = db.collection("users").document(uid).collection("aiConversations")
            val convId = if (existingConversationId.isNullOrBlank()) {
                val newRef = convCol.document()
                newRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to title.trim().take(150).ifBlank { "AI Research Session" },
                        "contextLabel" to contextLabel.take(200),
                        "messageCount" to 1,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                newRef.id
            } else {
                val safeConvId = sanitizeId(existingConversationId)
                val convRef = convCol.document(safeConvId)
                if (!convRef.get().await().exists()) {
                    convRef.set(
                        mapOf(
                            "userId" to uid,
                            "title" to title.trim().take(150).ifBlank { "AI Research Session" },
                            "contextLabel" to contextLabel.take(200),
                            "messageCount" to 1,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    convRef.update(
                        mapOf(
                            "contextLabel" to contextLabel.take(200),
                            "messageCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                safeConvId
            }

            val msgRef = convCol.document(convId).collection("messages").document()
            msgRef.set(
                mapOf(
                    "userId" to uid,
                    "role" to if (role == "model") "model" else "user",
                    "content" to content.take(20000).ifBlank { "(empty)" },
                    "contextSummary" to contextSummary.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(userId: String, conversationId: String): Result<Unit> {
        val docId = sanitizeId(conversationId)
        val path = "users/$userId/aiConversations/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val convRef = db.collection("users").document(uid).collection("aiConversations").document(docId)
            val msgs = convRef.collection("messages").get().await()
            val batch = db.batch()
            msgs.documents.forEach { batch.delete(it.reference) }
            batch.delete(convRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun savePreferences(userId: String, prefs: UserPreferences): Result<Unit> {
        val path = "users/$userId/preferences/appSettings"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("preferences").document("appSettings")
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "darkTheme" to prefs.darkTheme,
                        "preferredModel" to prefs.preferredModel.take(60).ifBlank { "gemini-3.8-flash" },
                        "autoSaveDiscovered" to prefs.autoSaveDiscovered,
                        "autoVerifyLinks" to prefs.autoVerifyLinks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "darkTheme" to prefs.darkTheme,
                        "preferredModel" to prefs.preferredModel.take(60).ifBlank { "gemini-3.8-flash" },
                        "autoSaveDiscovered" to prefs.autoSaveDiscovered,
                        "autoVerifyLinks" to prefs.autoVerifyLinks,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun websiteCreatePayload(uid: String, site: WebsiteResource): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "name" to site.name.take(140).ifBlank { site.domain.ifBlank { "Website" } },
            "url" to site.url.take(500).ifBlank { "https://example.org" },
            "domain" to site.domain.take(200).ifBlank { "example.org" },
            "description" to site.description.take(1500),
            "purpose" to site.purpose.take(500),
            "categories" to site.categories.take(10),
            "keyFeatures" to site.keyFeatures.take(15),
            "accessType" to site.accessEnum.wireValue,
            "pricingModel" to site.pricingModel.take(300),
            "freeTierLimitations" to site.freeTierLimitations.take(500),
            "registrationRequired" to site.registrationEnum.wireValue,
            "openSource" to site.openSource,
            "aiPowered" to site.aiPowered,
            "skillLevel" to site.skillEnum.wireValue,
            "recommendationScore" to site.recommendationScore.coerceIn(0, 100),
            "recommendationLabel" to site.recommendationLabel.take(120),
            "whyRecommended" to site.whyRecommended.take(1000),
            "advantages" to site.advantages.take(10),
            "disadvantages" to site.disadvantages.take(10),
            "whoShouldUse" to site.whoShouldUse.take(500),
            "verifiedFacts" to site.verifiedFacts.take(600),
            "sourceType" to site.sourceEnum.wireValue,
            "savedByUser" to site.savedByUser,
            "notes" to site.notes.take(2000),
            "tags" to site.tags.take(15),
            "verificationStatus" to site.verificationEnum.wireValue,
            "httpStatusCode" to site.httpStatusCode.coerceIn(0, 999),
            "lastCheckedAt" to site.lastCheckedAt.take(64),
            "visitCount" to site.visitCount.coerceIn(0, 1000000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.unwrapAndRethrow(
        error: Throwable,
        operationType: OperationType,
        path: String,
        userId: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
