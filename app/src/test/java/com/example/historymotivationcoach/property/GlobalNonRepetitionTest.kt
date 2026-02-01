package com.example.historymotivationcoach.property

import com.example.historymotivationcoach.data.dao.HistoryDao
import com.example.historymotivationcoach.data.dao.MotivationDao
import com.example.historymotivationcoach.data.entity.DeliveryHistory
import com.example.historymotivationcoach.data.entity.DeliveryStatus
import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.repository.MotivationRepository
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Property-based test for global non-repetition guarantee.
 * 
 * This is a CRITICAL property that ensures the core value proposition of the app:
 * users never see the same motivation twice (unless they explicitly reset via Replay Classics).
 * 
 * Requirements: 20.2
 */
class GlobalNonRepetitionTest {

    /**
     * Property 37: Global Non-Repetition
     * 
     * For any sequence of motivation deliveries (excluding Replay Classics resets),
     * no motivation item should be delivered more than once.
     * 
     * Validates: Requirements 20.2
     * 
     * This test runs 1000 iterations to thoroughly verify the non-repetition guarantee
     * across various database states and selection scenarios.
     */
    @Test
    fun `Property 37 - Global Non-Repetition`() = runTest {
        // Feature: history-motivation-coach, Property 37: Global Non-Repetition
        
        // Run 1000 iterations for critical property
        checkAll(1000, motivationSequenceArb()) { sequence ->
            // Create mock DAOs
            val motivationDao = mockk<MotivationDao>()
            val historyDao = mockk<HistoryDao>()
            
            // Track delivered items
            val deliveredItems = mutableSetOf<Long>()
            val allItems = sequence.availableItems
            
            // Configure mock to return unseen items (items not yet delivered)
            coEvery { motivationDao.getUnseenItems() } answers {
                allItems.filter { it.id !in deliveredItems }
            }
            
            // Configure mock to record deliveries
            coEvery { historyDao.insert(any()) } answers {
                val history = firstArg<DeliveryHistory>()
                deliveredItems.add(history.itemId)
                history.historyId
            }
            
            // Create repository
            val repository = MotivationRepository(motivationDao, historyDao)
            
            // Simulate the delivery sequence
            val actualDeliveries = mutableListOf<Long>()
            
            for (i in 0 until sequence.deliveryCount) {
                val selected = repository.selectRandomUnseen(emptyList())
                
                if (selected != null) {
                    // Record the delivery
                    repository.recordDelivery(selected.id, i)
                    actualDeliveries.add(selected.id)
                }
            }
            
            // PROPERTY: No item should appear more than once in the delivery sequence
            val uniqueDeliveries = actualDeliveries.toSet()
            assert(actualDeliveries.size == uniqueDeliveries.size) {
                "Non-repetition violated: ${actualDeliveries.size} deliveries but only ${uniqueDeliveries.size} unique items. " +
                "Duplicates: ${actualDeliveries.groupBy { it }.filter { it.value.size > 1 }.keys}"
            }
            
            // PROPERTY: All delivered items should be from the available pool
            assert(actualDeliveries.all { it in allItems.map { item -> item.id } }) {
                "Delivered item not in available pool"
            }
            
            // PROPERTY: Number of deliveries should not exceed available items
            assert(actualDeliveries.size <= allItems.size) {
                "More deliveries (${actualDeliveries.size}) than available items (${allItems.size})"
            }
            
            // PROPERTY: After exhaustion, no more items should be selected
            if (actualDeliveries.size == allItems.size) {
                val afterExhaustion = repository.selectRandomUnseen(emptyList())
                assert(afterExhaustion == null) {
                    "Item selected after content exhaustion"
                }
            }
        }
    }

    /**
     * Data class representing a sequence of motivation deliveries to test.
     */
    data class MotivationSequence(
        val availableItems: List<MotivationItem>,
        val deliveryCount: Int
    )

    /**
     * Arbitrary generator for motivation delivery sequences.
     * Generates various scenarios with different pool sizes and delivery counts.
     */
    private fun motivationSequenceArb(): Arb<MotivationSequence> = arbitrary {
        // Generate a pool of available items (between 5 and 100 items)
        val poolSize = Arb.int(5..100).bind()
        val items = List(poolSize) { index ->
            MotivationItem(
                id = (index + 1).toLong(),
                quote = "Quote ${index + 1}: ${Arb.string(20..100).bind()}",
                author = Arb.of("Author A", "Author B", "Author C", "Author D").bind(),
                context = if (Arb.boolean().bind()) "Context ${index + 1}" else null,
                imageUri = "android.resource://com.example.historymotivationcoach/drawable/img_${index + 1}",
                themes = Arb.list(
                    Arb.of("work", "passion", "excellence", "leadership", "innovation", "creativity"),
                    1..3
                ).bind(),
                sourceName = "Source ${index + 1}",
                sourceUrl = if (Arb.boolean().bind()) "https://example.com/${index + 1}" else null,
                license = Arb.of("Public Domain", "CC BY 4.0", "CC BY-SA 4.0").bind()
            )
        }
        
        // Generate delivery count (can be less than, equal to, or more than pool size)
        // Testing scenarios: partial exhaustion, exact exhaustion, over-exhaustion
        val deliveryCount = Arb.int(1..(poolSize + 10)).bind()
        
        MotivationSequence(items, deliveryCount)
    }
}
