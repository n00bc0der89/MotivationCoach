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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ContentSelector business logic component.
 * 
 * Tests verify:
 * - Content selection with and without theme preferences
 * - Content exhaustion detection
 * - Proper delegation to repositories
 */
class ContentSelectorTest {
    
    private lateinit var motivationRepository: MotivationRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var contentSelector: ContentSelector
    
    @Before
    fun setup() {
        motivationRepository = mock()
        preferencesRepository = mock()
        contentSelector = ContentSelector(motivationRepository, preferencesRepository)
    }
    
    @Test
    fun `selectNextMotivation returns item when content available`() = runTest {
        // Given: preferences with no themes and an available motivation item
        val prefs = UserPreferences(preferredThemes = emptyList())
        val expectedItem = createMotivationItem(id = 1, quote = "Test quote")
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        whenever(motivationRepository.selectRandomUnseen(emptyList())).thenReturn(expectedItem)
        
        // When: selecting next motivation
        val result = contentSelector.selectNextMotivation()
        
        // Then: should return the expected item
        assertNotNull(result)
        assertEquals(expectedItem, result)
        verify(preferencesRepository).getPreferences()
        verify(motivationRepository).selectRandomUnseen(emptyList())
    }
    
    @Test
    fun `selectNextMotivation returns null when content exhausted`() = runTest {
        // Given: preferences and no available content
        val prefs = UserPreferences(preferredThemes = emptyList())
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        whenever(motivationRepository.selectRandomUnseen(emptyList())).thenReturn(null)
        
        // When: selecting next motivation
        val result = contentSelector.selectNextMotivation()
        
        // Then: should return null
        assertNull(result)
        verify(preferencesRepository).getPreferences()
        verify(motivationRepository).selectRandomUnseen(emptyList())
    }
    
    @Test
    fun `selectNextMotivation passes theme preferences to repository`() = runTest {
        // Given: preferences with specific themes
        val themes = listOf("motivation", "success", "perseverance")
        val prefs = UserPreferences(preferredThemes = themes)
        val expectedItem = createMotivationItem(id = 2, themes = themes)
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        whenever(motivationRepository.selectRandomUnseen(themes)).thenReturn(expectedItem)
        
        // When: selecting next motivation
        val result = contentSelector.selectNextMotivation()
        
        // Then: should pass themes to repository and return themed item
        assertNotNull(result)
        assertEquals(expectedItem, result)
        verify(preferencesRepository).getPreferences()
        verify(motivationRepository).selectRandomUnseen(themes)
    }
    
    @Test
    fun `isContentExhausted returns true when unseen count is zero`() = runTest {
        // Given: no unseen content
        whenever(motivationRepository.getUnseenCount()).thenReturn(0)
        
        // When: checking if content is exhausted
        val result = contentSelector.isContentExhausted()
        
        // Then: should return true
        assertTrue(result)
        verify(motivationRepository).getUnseenCount()
    }
    
    @Test
    fun `isContentExhausted returns false when unseen content exists`() = runTest {
        // Given: some unseen content available
        whenever(motivationRepository.getUnseenCount()).thenReturn(42)
        
        // When: checking if content is exhausted
        val result = contentSelector.isContentExhausted()
        
        // Then: should return false
        assertFalse(result)
        verify(motivationRepository).getUnseenCount()
    }
    
    @Test
    fun `isContentExhausted returns false when single item remains`() = runTest {
        // Given: exactly one unseen item (edge case)
        whenever(motivationRepository.getUnseenCount()).thenReturn(1)
        
        // When: checking if content is exhausted
        val result = contentSelector.isContentExhausted()
        
        // Then: should return false (content still available)
        assertFalse(result)
        verify(motivationRepository).getUnseenCount()
    }
    
    @Test
    fun `selectNextMotivation handles empty theme preferences`() = runTest {
        // Given: preferences with explicitly empty theme list
        val prefs = UserPreferences(preferredThemes = emptyList())
        val expectedItem = createMotivationItem(id = 3)
        
        whenever(preferencesRepository.getPreferences()).thenReturn(prefs)
        whenever(motivationRepository.selectRandomUnseen(emptyList())).thenReturn(expectedItem)
        
        // When: selecting next motivation
        val result = contentSelector.selectNextMotivation()
        
        // Then: should work correctly with empty themes
        assertNotNull(result)
        assertEquals(expectedItem, result)
        verify(motivationRepository).selectRandomUnseen(emptyList())
    }
    
    // Helper function to create test motivation items
    private fun createMotivationItem(
        id: Long = 1,
        quote: String = "Default quote",
        author: String = "Default Author",
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
}
