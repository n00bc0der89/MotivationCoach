package com.example.historymotivationcoach.business

import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository

/**
 * Business logic component for selecting motivational content.
 * 
 * Implements the content selection algorithm that ensures non-repeating content
 * delivery while respecting user theme preferences.
 * 
 * Requirements:
 * - 4.1: Select only from unseen pool (non-repeating)
 * - 4.4: Detect content exhaustion
 * - 16.2: Random selection from unseen items
 * - 16.3: Theme preference filtering
 */
class ContentSelector(
    private val motivationRepository: MotivationRepository,
    private val preferencesRepository: PreferencesRepository
) {
    
    /**
     * Select the next motivation item to deliver.
     * 
     * This method implements the core content selection algorithm:
     * 1. Retrieves user's preferred themes from preferences
     * 2. Delegates to repository to select a random unseen item
     * 3. Repository handles theme-biased selection (prefers themed items if available)
     * 
     * @return A MotivationItem to deliver, or null if content is exhausted
     */
    suspend fun selectNextMotivation(): MotivationItem? {
        val prefs = preferencesRepository.getPreferences()
        return motivationRepository.selectRandomUnseen(prefs.preferredThemes)
    }
    
    /**
     * Check if all content has been exhausted.
     * 
     * Content is considered exhausted when there are no unseen items remaining.
     * This is used to:
     * - Stop scheduling new notifications
     * - Display exhaustion message to user
     * - Enable "Replay Classics" option
     * 
     * @return true if no unseen content remains, false otherwise
     */
    suspend fun isContentExhausted(): Boolean {
        return motivationRepository.getUnseenCount() == 0
    }
}
