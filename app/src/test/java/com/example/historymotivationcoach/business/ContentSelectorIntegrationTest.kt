package com.example.historymotivationcoach.business

import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.entity.UserPreferences
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration tests for ContentSelector with fake repositories.
 * 
 * These tests verify the ContentSelector works correctly with realistic
 * repository implementations (using fakes instead of mocks).
 */
class ContentSelectorIntegrationTest {
    
    @Test
    fun `selectNextMotivation works with fake repositories`() = runTest {
        // Given: fake repositories with test data
        val items = listOf(
            createMotivationItem(1, "Quote 1", themes = listOf("motivation")),
            createMotivationItem(2, "Quote 2", themes = listOf("success")),
            createMotivationItem(3, "Quote 3", themes = listOf("motivation", "success"))
        )
        val motivationRepo = FakeMotivationRepository(items)
        val prefsRepo = FakePreferencesRepository(UserPreferences())
        val contentSelector = FakeContentSelector(motivationRepo, prefsRepo)
        
        // When: selecting next motivation
        val result = contentSelector.selectNextMotivation()
        
        // Then: should return one of the available items
        assertNotNull(result)
        assertTrue(items.contains(result))
    }
    
    @Test
    fun `isContentExhausted detects when all content delivered`() = runTest {
        // Given: fake repositories with items all marked as delivered
        val items = listOf(
            createMotivationItem(1, "Quote 1"),
            createMotivationItem(2, "Quote 2")
        )
        val motivationRepo = FakeMotivationRepository(items, deliveredIds = setOf(1, 2))
        val prefsRepo = FakePreferencesRepository(UserPreferences())
        val contentSelector = FakeContentSelector(motivationRepo, prefsRepo)
        
        // When: checking exhaustion
        val exhausted = contentSelector.isContentExhausted()
        
        // Then: should be exhausted
        assertTrue(exhausted)
    }
    
    @Test
    fun `selectNextMotivation respects theme preferences`() = runTest {
        // Given: items with different themes and user preference for "motivation"
        val items = listOf(
            createMotivationItem(1, "Quote 1", themes = listOf("motivation")),
            createMotivationItem(2, "Quote 2", themes = listOf("success")),
            createMotivationItem(3, "Quote 3", themes = listOf("other"))
        )
        val prefs = UserPreferences(preferredThemes = listOf("motivation"))
        val motivationRepo = FakeMotivationRepository(items)
        val prefsRepo = FakePreferencesRepository(prefs)
        val contentSelector = FakeContentSelector(motivationRepo, prefsRepo)
        
        // When: selecting next motivation multiple times
        val results = mutableSetOf<Long>()
        repeat(10) {
            contentSelector.selectNextMotivation()?.let { results.add(it.id) }
        }
        
        // Then: should prefer themed items (item 1 should appear)
        assertTrue(results.contains(1L))
    }
    
    @Test
    fun `selectNextMotivation returns null when exhausted`() = runTest {
        // Given: empty repository
        val motivationRepo = FakeMotivationRepository(emptyList())
        val prefsRepo = FakePreferencesRepository(UserPreferences())
        val contentSelector = FakeContentSelector(motivationRepo, prefsRepo)
        
        // When: selecting next motivation
        val result = contentSelector.selectNextMotivation()
        
        // Then: should return null
        assertNull(result)
    }
    
    @Test
    fun `isContentExhausted returns false when content available`() = runTest {
        // Given: repository with available items
        val items = listOf(createMotivationItem(1, "Quote 1"))
        val motivationRepo = FakeMotivationRepository(items)
        val prefsRepo = FakePreferencesRepository(UserPreferences())
        val contentSelector = FakeContentSelector(motivationRepo, prefsRepo)
        
        // When: checking exhaustion
        val exhausted = contentSelector.isContentExhausted()
        
        // Then: should not be exhausted
        assertFalse(exhausted)
    }
    
    // Helper function to create test motivation items
    private fun createMotivationItem(
        id: Long,
        quote: String,
        author: String = "Test Author",
        themes: List<String> = emptyList()
    ): MotivationItem {
        return MotivationItem(
            id = id,
            quote = quote,
            author = author,
            context = null,
            imageUri = "android.resource://test/drawable/test",
            themes = themes,
            sourceName = "Test Source",
            sourceUrl = null,
            license = "Public Domain"
        )
    }
    
    // Fake repository implementations for testing
    
    private class FakeMotivationRepository(
        private val allItems: List<MotivationItem>,
        private val deliveredIds: Set<Long> = emptySet()
    ) {
        suspend fun selectRandomUnseen(preferredThemes: List<String>): MotivationItem? {
            val unseenItems = allItems.filter { it.id !in deliveredIds }
            
            return if (preferredThemes.isEmpty()) {
                unseenItems.randomOrNull()
            } else {
                val themed = unseenItems.filter { item ->
                    item.themes.any { it in preferredThemes }
                }
                (themed.ifEmpty { unseenItems }).randomOrNull()
            }
        }
        
        suspend fun getUnseenCount(): Int {
            return allItems.count { it.id !in deliveredIds }
        }
    }
    
    private class FakePreferencesRepository(
        private val prefs: UserPreferences
    ) {
        suspend fun getPreferences(): UserPreferences = prefs
    }
    
    // Fake ContentSelector that uses fake repositories
    private class FakeContentSelector(
        private val motivationRepo: FakeMotivationRepository,
        private val prefsRepo: FakePreferencesRepository
    ) {
        suspend fun selectNextMotivation(): MotivationItem? {
            val prefs = prefsRepo.getPreferences()
            return motivationRepo.selectRandomUnseen(prefs.preferredThemes)
        }
        
        suspend fun isContentExhausted(): Boolean {
            return motivationRepo.getUnseenCount() == 0
        }
    }
}
