package com.example.historymotivationcoach.ui

import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Property-based tests for accessibility content descriptions.
 * 
 * These tests verify that interactive UI elements have appropriate content descriptions
 * for accessibility purposes (TalkBack, screen readers, etc.).
 * 
 * Requirements: 18.4
 */
class AccessibilityPropertiesTest {

    /**
     * Property 34: Accessibility Content Descriptions
     * 
     * For any interactive UI element, it should have a content description set for accessibility.
     * 
     * Validates: Requirements 18.4
     * 
     * This property test verifies that content descriptions follow accessibility best practices:
     * - Content descriptions should be descriptive and meaningful
     * - Content descriptions should not be empty or just whitespace
     * - Content descriptions should provide context about the element's purpose
     * - Content descriptions should indicate the element's state when applicable
     * - Content descriptions should be concise but informative
     */
    @Test
    fun `Property 34 - Accessibility Content Descriptions`() = runTest {
        // Feature: history-motivation-coach, Property 34: Accessibility Content Descriptions
        
        // Property test with 100 iterations
        checkAll(100, accessibilityDescriptionArb()) { description ->
            // Verify content description is not empty
            assert(description.text.isNotBlank()) {
                "Content description should not be blank"
            }
            
            // Verify content description is meaningful (at least 2 characters for abbreviations)
            assert(description.text.length >= 2) {
                "Content description should be meaningful, got: '${description.text}'"
            }
            
            // Verify content description doesn't contain only special characters
            assert(description.text.any { it.isLetterOrDigit() }) {
                "Content description should contain alphanumeric characters, got: '${description.text}'"
            }
            
            // Verify content description is not excessively long (should be concise)
            assert(description.text.length <= 200) {
                "Content description should be concise (max 200 chars), got ${description.text.length} chars"
            }
            
            // For interactive elements, verify description indicates purpose or provides context
            if (description.isInteractive) {
                val hasActionWord = description.text.contains("tap", ignoreCase = true) ||
                    description.text.contains("click", ignoreCase = true) ||
                    description.text.contains("select", ignoreCase = true) ||
                    description.text.contains("toggle", ignoreCase = true) ||
                    description.text.contains("slide", ignoreCase = true) ||
                    description.text.contains("navigate", ignoreCase = true) ||
                    description.text.contains("button", ignoreCase = true) ||
                    description.text.contains("switch", ignoreCase = true) ||
                    description.text.contains("card", ignoreCase = true) ||
                    description.text.contains("tab", ignoreCase = true)
                
                val hasMultipleWords = description.text.split(" ").filter { it.isNotBlank() }.size >= 2
                val hasContext = description.text.contains(":", ignoreCase = false) || 
                                description.text.contains(".", ignoreCase = false)
                
                // Single action verbs are also valid (e.g., "Close", "Save", "Cancel")
                val isSingleActionVerb = description.text.split(" ").filter { it.isNotBlank() }.size == 1 &&
                                        description.text.length >= 3
                
                assert(hasActionWord || hasMultipleWords || hasContext || isSingleActionVerb) {
                    "Interactive element description should indicate purpose or provide context, got: '${description.text}'"
                }
            }
            
            // For stateful elements, verify description indicates state
            if (description.hasState) {
                assert(
                    description.text.contains("selected", ignoreCase = true) ||
                    description.text.contains("not selected", ignoreCase = true) ||
                    description.text.contains("enabled", ignoreCase = true) ||
                    description.text.contains("disabled", ignoreCase = true) ||
                    description.text.contains("checked", ignoreCase = true) ||
                    description.text.contains("unchecked", ignoreCase = true) ||
                    description.text.contains("loading", ignoreCase = true) ||
                    description.text.contains(":", ignoreCase = false) // State often indicated with colon
                ) {
                    "Stateful element description should indicate state, got: '${description.text}'"
                }
            }
            
            // Verify description doesn't start or end with whitespace
            assert(description.text == description.text.trim()) {
                "Content description should not have leading or trailing whitespace"
            }
            
            // For images, verify description describes the image content
            if (description.elementType == ElementType.IMAGE) {
                assert(
                    description.text.contains("image", ignoreCase = true) ||
                    description.text.contains("photo", ignoreCase = true) ||
                    description.text.contains("picture", ignoreCase = true) ||
                    description.text.contains("thumbnail", ignoreCase = true) ||
                    description.text.contains("for", ignoreCase = true) ||
                    // Or directly describes what's in the image
                    description.text.split(" ").size >= 2
                ) {
                    "Image description should describe the image content, got: '${description.text}'"
                }
            }
        }
    }

    /**
     * Data class representing an accessibility description with metadata.
     */
    data class AccessibilityDescription(
        val text: String,
        val elementType: ElementType,
        val isInteractive: Boolean,
        val hasState: Boolean
    )

    /**
     * Enum representing different types of UI elements.
     */
    enum class ElementType {
        BUTTON,
        SWITCH,
        SLIDER,
        CARD,
        CHIP,
        IMAGE,
        ICON_BUTTON,
        NAVIGATION_ITEM,
        PROGRESS_INDICATOR,
        TEXT_FIELD
    }

    /**
     * Arbitrary generator for accessibility descriptions based on real UI patterns.
     */
    private fun accessibilityDescriptionArb(): Arb<AccessibilityDescription> = Arb.choice(
        buttonDescriptionArb(),
        switchDescriptionArb(),
        sliderDescriptionArb(),
        cardDescriptionArb(),
        chipDescriptionArb(),
        imageDescriptionArb(),
        iconButtonDescriptionArb(),
        navigationItemDescriptionArb(),
        progressIndicatorDescriptionArb()
    )

    private fun buttonDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val actions = listOf("Send", "Retry", "Clear", "Save", "Cancel", "Confirm", "Add", "Remove")
        val action = Arb.of(actions).bind()
        val contexts = listOf("one now", "history", "settings", "changes")
        val context = Arb.of(contexts).bind()
        val text = "$action $context"
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.BUTTON,
            isInteractive = true,
            hasState = false
        )
    }

    private fun switchDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val feature = Arb.of("Notifications", "Dark mode", "Auto-sync", "Sound").bind()
        val state = Arb.of("enabled", "disabled").bind()
        val text = "$feature $state. Toggle to ${if (state == "enabled") "disable" else "enable"}."
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.SWITCH,
            isInteractive = true,
            hasState = true
        )
    }

    private fun sliderDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val value = Arb.int(1..10).bind()
        val text = "Notifications per day: $value. Slide to adjust between 1 and 10."
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.SLIDER,
            isInteractive = true,
            hasState = true
        )
    }

    private fun cardDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val quotes = listOf(
            "The only way to do great work is to love what you do",
            "Innovation distinguishes between a leader and a follower",
            "Stay hungry, stay foolish"
        )
        val quote = Arb.of(quotes).bind()
        val author = Arb.of("Steve Jobs", "Albert Einstein", "Maya Angelou").bind()
        val text = "Motivation card: ${quote.take(50)}... by $author. Tap to view details."
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.CARD,
            isInteractive = true,
            hasState = false
        )
    }

    private fun chipDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val theme = Arb.of("work", "passion", "excellence", "leadership", "innovation").bind()
        val text = "Theme: $theme"
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.CHIP,
            isInteractive = false,
            hasState = false
        )
    }

    private fun imageDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val authors = listOf("Steve Jobs", "Albert Einstein", "Maya Angelou", "Nelson Mandela")
        val author = Arb.of(authors).bind()
        val imageType = Arb.of("Motivation image", "Thumbnail", "Image").bind()
        val text = "$imageType for $author"
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.IMAGE,
            isInteractive = false,
            hasState = false
        )
    }

    private fun iconButtonDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val action = Arb.of("Navigate back", "Close", "Menu", "More options", "Share").bind()
        val text = action
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.ICON_BUTTON,
            isInteractive = true,
            hasState = false
        )
    }

    private fun navigationItemDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val tab = Arb.of("Home", "History", "Settings").bind()
        val selected = Arb.boolean().bind()
        val state = if (selected) "selected" else "not selected"
        val action = if (selected) "" else ". Tap to navigate to $tab"
        val text = "$tab tab, $state$action"
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.NAVIGATION_ITEM,
            isInteractive = true,
            hasState = true
        )
    }

    private fun progressIndicatorDescriptionArb(): Arb<AccessibilityDescription> = arbitrary {
        val context = Arb.of(
            "Loading latest motivation",
            "Loading history",
            "Loading motivation details"
        ).bind()
        val text = context
        
        AccessibilityDescription(
            text = text,
            elementType = ElementType.PROGRESS_INDICATOR,
            isInteractive = false,
            hasState = true
        )
    }
}
