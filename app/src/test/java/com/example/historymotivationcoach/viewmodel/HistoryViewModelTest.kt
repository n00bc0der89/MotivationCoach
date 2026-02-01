package com.example.historymotivationcoach.viewmodel

import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.data.entity.DeliveryStatus
import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.repository.MotivationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    
    private lateinit var motivationRepository: MotivationRepository
    private lateinit var viewModel: HistoryViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        motivationRepository = mock()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = HistoryViewModel(motivationRepository)
        
        val initialState = viewModel.uiState.value
        assertTrue(initialState is HistoryUiState.Loading)
    }
    
    @Test
    fun `loadHistory with no history shows Empty state`() = runTest {
        whenever(motivationRepository.getAllDateKeys()).thenReturn(emptyList())
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Empty)
    }
    
    @Test
    fun `loadHistory with history shows Success state`() = runTest {
        val today = getCurrentDateKey()
        val mockMotivation = createMockMotivationWithHistory(dateKey = today)
        
        whenever(motivationRepository.getAllDateKeys()).thenReturn(listOf(today))
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(listOf(mockMotivation))
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Success)
        assertEquals(1, state.groups.size)
        assertEquals("Today", state.groups[0].label)
        assertEquals(today, state.groups[0].dateKey)
        assertEquals(1, state.groups[0].items.size)
    }
    
    @Test
    fun `loadHistory formats today label correctly`() = runTest {
        val today = getCurrentDateKey()
        val mockMotivation = createMockMotivationWithHistory(dateKey = today)
        
        whenever(motivationRepository.getAllDateKeys()).thenReturn(listOf(today))
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(listOf(mockMotivation))
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Success)
        assertEquals("Today", state.groups[0].label)
    }
    
    @Test
    fun `loadHistory formats yesterday label correctly`() = runTest {
        val yesterday = getYesterdayDateKey()
        val mockMotivation = createMockMotivationWithHistory(dateKey = yesterday)
        
        whenever(motivationRepository.getAllDateKeys()).thenReturn(listOf(yesterday))
        whenever(motivationRepository.getHistoryByDate(yesterday)).thenReturn(listOf(mockMotivation))
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Success)
        assertEquals("Yesterday", state.groups[0].label)
    }
    
    @Test
    fun `loadHistory formats older date label correctly`() = runTest {
        val olderDate = "2024-01-15"
        val mockMotivation = createMockMotivationWithHistory(dateKey = olderDate)
        
        whenever(motivationRepository.getAllDateKeys()).thenReturn(listOf(olderDate))
        whenever(motivationRepository.getHistoryByDate(olderDate)).thenReturn(listOf(mockMotivation))
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Success)
        assertEquals("January 15, 2024", state.groups[0].label)
    }
    
    @Test
    fun `loadHistory groups multiple dates correctly`() = runTest {
        val today = getCurrentDateKey()
        val yesterday = getYesterdayDateKey()
        val olderDate = "2024-01-15"
        
        val todayMotivation = createMockMotivationWithHistory(id = 1L, dateKey = today)
        val yesterdayMotivation = createMockMotivationWithHistory(id = 2L, dateKey = yesterday)
        val olderMotivation = createMockMotivationWithHistory(id = 3L, dateKey = olderDate)
        
        whenever(motivationRepository.getAllDateKeys()).thenReturn(listOf(today, yesterday, olderDate))
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(listOf(todayMotivation))
        whenever(motivationRepository.getHistoryByDate(yesterday)).thenReturn(listOf(yesterdayMotivation))
        whenever(motivationRepository.getHistoryByDate(olderDate)).thenReturn(listOf(olderMotivation))
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Success)
        assertEquals(3, state.groups.size)
        assertEquals("Today", state.groups[0].label)
        assertEquals("Yesterday", state.groups[1].label)
        assertEquals("January 15, 2024", state.groups[2].label)
    }
    
    @Test
    fun `loadHistory with error shows Error state`() = runTest {
        whenever(motivationRepository.getAllDateKeys()).thenThrow(RuntimeException("Database error"))
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Error)
        // Error message should be user-friendly, not the raw exception message
        assertEquals("An unexpected error occurred. Please restart the app.", state.message)
    }
    
    @Test
    fun `loadHistory with multiple items per date groups correctly`() = runTest {
        val today = getCurrentDateKey()
        val motivation1 = createMockMotivationWithHistory(id = 1L, dateKey = today)
        val motivation2 = createMockMotivationWithHistory(id = 2L, dateKey = today)
        val motivation3 = createMockMotivationWithHistory(id = 3L, dateKey = today)
        
        whenever(motivationRepository.getAllDateKeys()).thenReturn(listOf(today))
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(
            listOf(motivation1, motivation2, motivation3)
        )
        
        viewModel = HistoryViewModel(motivationRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HistoryUiState.Success)
        assertEquals(1, state.groups.size)
        assertEquals(3, state.groups[0].items.size)
    }
    
    private fun getCurrentDateKey(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(Date())
    }
    
    private fun getYesterdayDateKey(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(calendar.time)
    }
    
    private fun createMockMotivationWithHistory(
        id: Long = 1L,
        dateKey: String = getCurrentDateKey()
    ): MotivationWithHistory {
        val item = MotivationItem(
            id = id,
            quote = "Test quote $id",
            author = "Test Author $id",
            context = "Test context",
            imageUri = "android.resource://test/drawable/test",
            themes = listOf("motivation", "success"),
            sourceName = "Test Source",
            sourceUrl = "https://test.com",
            license = "Public Domain"
        )
        
        return MotivationWithHistory(
            item = item,
            shownAt = System.currentTimeMillis(),
            dateKey = dateKey,
            deliveryStatus = DeliveryStatus.DELIVERED
        )
    }
}
