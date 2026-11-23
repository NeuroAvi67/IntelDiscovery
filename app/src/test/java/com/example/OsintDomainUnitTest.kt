package com.example

import com.example.data.model.AccessType
import com.example.data.model.SearchFilterState
import com.example.data.model.domainToSlug
import com.example.data.model.extractDomain
import com.example.data.seed.CuratedDirectorySeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsintDomainUnitTest {

    @Test
    fun `extractDomain normalizes urls and strips www`() {
        assertEquals("shodan.io", extractDomain("https://www.shodan.io/search?q=tls"))
        assertEquals("data.gov", extractDomain("data.gov"))
        assertEquals("web.archive.org", extractDomain("https://web.archive.org/cdx"))
    }

    @Test
    fun `domainToSlug satisfies firestore validId regex`() {
        val regex = Regex("^[a-zA-Z0-9_\\-]+$")
        val slug = domainToSlug("https://datasetsearch.research.google.com/search?query=climate")
        assertTrue(regex.matches(slug))
        assertEquals("datasetsearch_research_google_com", slug)
    }

    @Test
    fun `curated directory seed has unique domain slugs and valid bounds`() {
        val sites = CuratedDirectorySeed.initialWebsites("test_uid")
        assertTrue(sites.size >= 10)
        val ids = sites.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        sites.forEach { site ->
            assertTrue(site.name.isNotBlank())
            assertTrue(site.url.startsWith("https://"))
            assertTrue(site.recommendationScore in 0..100)
        }
    }

    @Test
    fun `filter state summary formats active filters`() {
        val filters = SearchFilterState(
            accessTypes = setOf(AccessType.FREE),
            openSourceOnly = true,
            noRegistrationOnly = true
        )
        val summary = filters.summaryText()
        assertTrue("FREE" in summary)
        assertTrue("Open Source" in summary)
        assertTrue("No Registration" in summary)
        assertEquals(3, filters.activeCount())
    }
}
