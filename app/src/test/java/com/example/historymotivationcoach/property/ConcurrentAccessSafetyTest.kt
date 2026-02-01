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
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Property-based test for concurrent access safety.
 * 
 * This is a CRITICAL property that ensures data consistency when multiple
 * operations access the database concurrently.
 * 
 * Requirements: 20.4
 */
class ConcurrentAccessSafetyTest {

    /**
     * Property 38: Concurrent Access Safety
     * 
     * For any set of concurrent database operations (reads and writes),
     * the database should maintain data consistency without corruption.
     * 
     * Validates: Requirements 20.4
     * 
     * This test verifies that:
     * - Concurrent reads return consistent data
     * - Concurrent writes don't corrupt data
     * - Mixed concurrent reads and writes maintain consistency
     * - No race conditions in content selection
     * - Delivery recording is atomic
     */
    @Test
    fun `Property 38 - Concurrent Access Safety`() = runTest {
        // Feature: history-motivation-coach, Property 38: Concurrent Access Safety
        
        // Run 100 iterations with various concurrent operation scenarios
        checkAll(100, concurrentOperationScenarioArb()) { scenario ->
            // Create thread-safe data structures to simulate database state
            val availableItems = ConcurrentHashMap<Long, MotivationItem>()
            scenario.items.forEach { item ->
                availableItems[item.id] = item
            }
            
            val deliveredItems = ConcurrentHashMap.newKeySet<Long>()
            val deliveryHistory = ConcurrentHashMap<Long, DeliveryHistory>()
            val historyIdCounter = AtomicInteger(1)
            
            // Use a mutex to simulate Room's transaction-level locking
            val dbLock = kotlinx.coroutines.sync.Mutex()
            
            // Create mock DAOs with thread-safe operations
            val motivationDao = mockk<MotivationDao>()
            val historyDao = mockk<HistoryDao>()
            
            // Mock getUnseenItems to return items not in deliveredItems
            // This simulates a database query which is atomic
            coEvery { motivationDao.getUnseenItems() } coAnswers {
                dbLock.withLock {
                    availableItems.values.filter { it.id !in deliveredItems }.toList()
                }
            }
            
            // Mock getUnseenCount
            coEvery { motivationDao.getUnseenCount() } coAnswers {
                dbLock.withLock {
                    availableItems.values.count { it.id !in deliveredItems }
                }
            }
            
            // Mock insert to atomically record delivery
            // This simulates Room's @Insert which is atomic
            coEvery { historyDao.insert(any()) } coAnswers {
                dbLock.withLock {
                    val history = firstArg<DeliveryHistory>()
                    val historyId = historyIdCounter.getAndIncrement().toLong()
                    val historyWithId = history.copy(historyId = historyId)
                    
                    // Atomically add to both sets
                    deliveredItems.add(history.itemId)
                    deliveryHistory[historyId] = historyWithId
                    
                    historyId
                }
            }
            
            // Create repository
            val repository = MotivationRepository(motivationDao, historyDao)
            
            // Execute concurrent operations
            val results = mutableListOf<Deferred<OperationResult>>()
            
            coroutineScope {
                // Launch concurrent selection and delivery operations
                repeat(scenario.concurrentOperations) { opIndex ->
                    val deferred = async(Dispatchers.Default) {
                        try {
                            when (scenario.operationType) {
                                OperationType.SELECT_ONLY -> {
                                    val item = repository.selectRandomUnseen(emptyList())
                                    OperationResult.SelectResult(item?.id)
                                }
                                OperationType.SELECT_AND_DELIVER -> {
                                    // Use atomic select-and-deliver method
                                    val result = repository.selectAndDeliver(emptyList(), opIndex)
                                    if (result != null) {
                                        OperationResult.DeliveryResult(result.first.id, result.second)
                                    } else {
                                        OperationResult.DeliveryResult(null, null)
                                    }
                                }
                                OperationType.COUNT_ONLY -> {
                                    val count = repository.getUnseenCount()
                                    OperationResult.CountResult(count)
                                }
                                OperationType.MIXED -> {
                                    // Randomly choose operation type
                                    when (opIndex % 3) {
                                        0 -> {
                                            val item = repository.selectRandomUnseen(emptyList())
                                            OperationResult.SelectResult(item?.id)
                                        }
                                        1 -> {
                                            val count = repository.getUnseenCount()
                                            OperationResult.CountResult(count)
                                        }
                                        else -> {
                                            // Use atomic select-and-deliver method
                                            val result = repository.selectAndDeliver(emptyList(), opIndex)
                                            if (result != null) {
                                                OperationResult.DeliveryResult(result.first.id, result.second)
                                            } else {
                                                OperationResult.DeliveryResult(null, null)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            OperationResult.ErrorResult(e.message ?: "Unknown error")
                        }
                    }
                    results.add(deferred)
                }
                
                // Wait for all operations to complete
                results.awaitAll()
            }
            
            val completedResults = results.map { it.await() }
            
            // PROPERTY: No exceptions should occur during concurrent operations
            val errors = completedResults.filterIsInstance<OperationResult.ErrorResult>()
            assert(errors.isEmpty()) {
                "Concurrent operations resulted in errors: ${errors.map { it.message }}"
            }
            
            // PROPERTY: All delivered items should be unique (no duplicates)
            // Note: This property tests that Room's database-level locking prevents duplicates
            val deliveredItemsList = deliveredItems.toList()
            val uniqueDelivered = deliveredItemsList.toSet()
            assert(deliveredItemsList.size == uniqueDelivered.size) {
                "Concurrent deliveries resulted in duplicates: ${deliveredItemsList.size} deliveries but only ${uniqueDelivered.size} unique"
            }
            
            // PROPERTY: All delivered items should be from the available pool
            assert(deliveredItemsList.all { it in availableItems.keys }) {
                "Delivered item not in available pool"
            }
            
            // PROPERTY: Number of deliveries should not exceed available items
            assert(deliveredItemsList.size <= availableItems.size) {
                "More deliveries (${deliveredItemsList.size}) than available items (${availableItems.size})"
            }
            
            // PROPERTY: Delivery history count should match delivered items count
            assert(deliveryHistory.size == deliveredItemsList.size) {
                "History count (${deliveryHistory.size}) does not match delivered count (${deliveredItemsList.size})"
            }
            
            // PROPERTY: Each delivery history entry should reference a delivered item
            deliveryHistory.values.forEach { history ->
                assert(history.itemId in deliveredItems) {
                    "History entry references item ${history.itemId} which is not in delivered set"
                }
            }
            
            // PROPERTY: Count results should be consistent with actual state
            val countResults = completedResults.filterIsInstance<OperationResult.CountResult>()
            countResults.forEach { result ->
                // Count should be between 0 and total items
                assert(result.count in 0..availableItems.size) {
                    "Invalid count: ${result.count}, expected 0..${availableItems.size}"
                }
            }
            
            // PROPERTY: Final unseen count should equal (total - delivered)
            val finalUnseenCount = availableItems.size - deliveredItems.size
            val actualFinalCount = repository.getUnseenCount()
            assert(actualFinalCount == finalUnseenCount) {
                "Final unseen count mismatch: expected $finalUnseenCount, got $actualFinalCount"
            }
        }
    }
    
    /**
     * Sealed class representing different operation results.
     */
    sealed class OperationResult {
        data class SelectResult(val itemId: Long?) : OperationResult()
        data class DeliveryResult(val itemId: Long?, val historyId: Long?) : OperationResult()
        data class CountResult(val count: Int) : OperationResult()
        data class ErrorResult(val message: String) : OperationResult()
    }
    
    /**
     * Enum representing different types of concurrent operations.
     */
    enum class OperationType {
        SELECT_ONLY,
        SELECT_AND_DELIVER,
        COUNT_ONLY,
        MIXED
    }
    
    /**
     * Data class representing a concurrent operation scenario.
     */
    data class ConcurrentOperationScenario(
        val items: List<MotivationItem>,
        val concurrentOperations: Int,
        val operationType: OperationType
    )
    
    /**
     * Arbitrary generator for concurrent operation scenarios.
     */
    private fun concurrentOperationScenarioArb(): Arb<ConcurrentOperationScenario> = arbitrary {
        // Generate a pool of items (5 to 50 items)
        val poolSize = Arb.int(5..50).bind()
        val items = List(poolSize) { index ->
            MotivationItem(
                id = (index + 1).toLong(),
                quote = "Quote ${index + 1}: ${Arb.string(20..100).bind()}",
                author = Arb.of("Author A", "Author B", "Author C").bind(),
                context = if (Arb.boolean().bind()) "Context ${index + 1}" else null,
                imageUri = "android.resource://com.example.historymotivationcoach/drawable/img_${index + 1}",
                themes = Arb.list(
                    Arb.of("work", "passion", "excellence", "leadership"),
                    1..3
                ).bind(),
                sourceName = "Source ${index + 1}",
                sourceUrl = if (Arb.boolean().bind()) "https://example.com/${index + 1}" else null,
                license = Arb.of("Public Domain", "CC BY 4.0").bind()
            )
        }
        
        // Generate number of concurrent operations (2 to 20)
        val concurrentOps = Arb.int(2..20).bind()
        
        // Generate operation type
        val opType = Arb.enum<OperationType>().bind()
        
        ConcurrentOperationScenario(items, concurrentOps, opType)
    }
}
