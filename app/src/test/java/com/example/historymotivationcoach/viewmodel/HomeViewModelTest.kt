package com.example.historymotivationcoach.viewmodel

import com.example.historymotivationcoach.data.dao.MotivationWithHistory
import com.example.historymotivationcoach.data.entity.DeliveryStatus
import com.example.historymotivationcoach.data.entity.MotivationItem
import com.example.historymotivationcoach.data.entity.UserPreferences
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository
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
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    
    private lateinit var motivationRepository: MotivationRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        motivationRepository = mock()
        preferencesRepository = mock()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = HomeViewModel(motivationRepository, preferencesRepository)
        
        val initialState = viewModel.uiState.value
        assertTrue(initialState is HomeUiState.Loading)
    }
    
    @Test
    fun `loadLatestMotivation with history shows Success state`() = runTest {
        val today = getCurrentDateKey()
        val mockMotivation = createMockMotivationWithHistory()
        
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(listOf(mockMotivation))
        whenever(motivationRepository.getUnseenCount()).thenReturn(50)
        
        viewModel = HomeViewModel(motivationRepository, preferencesRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HomeUiState.Success)
        assertEquals(mockMotivation, state.latestMotivation)
        assertEquals(1, state.todayCount)
        assertEquals(50, state.unseenCount)
    }
    
    @Test
    fun `loadLatestMotivation with no history shows Empty state`() = runTest {
        val today = getCurrentDateKey()
        
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(emptyList())
        whenever(motivationRepository.getUnseenCount()).thenReturn(100)
        
        viewModel = HomeViewModel(motivationRepository, preferencesRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HomeUiState.Empty)
        assertEquals(100, state.unseenCount)
    }
    
    @Test
    fun `loadLatestMotivation with error shows Error state`() = runTest {
        val today = getCurrentDateKey()
        
        whenever(motivationRepository.getHistoryByDate(today)).thenThrow(RuntimeException("Database error"))
        
        viewModel = HomeViewModel(motivationRepository, preferencesRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HomeUiState.Error)
        // Error message should be user-friendly, not the raw exception message
        assertEquals("An unexpected error occurred. Please restart the app.", state.message)
    }
    
    @Test
    fun `loadLatestMotivation with multiple items shows most recent`() = runTest {
        val today = getCurrentDateKey()
        val motivation1 = createMockMotivationWithHistory(shownAt = 1000L)
        val motivation2 = createMockMotivationWithHistory(shownAt = 2000L)
        val motivation3 = createMockMotivationWithHistory(shownAt = 3000L)
        
        // Repository should return items in descending order (most recent first)
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(
            listOf(motivation3, motivation2, motivation1)
        )
        whenever(motivationRepository.getUnseenCount()).thenReturn(25)
        
        viewModel = HomeViewModel(motivationRepository, preferencesRepository)
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HomeUiState.Success)
        assertEquals(motivation3, state.latestMotivation)
        assertEquals(3, state.todayCount)
    }
    
    @Test
    fun `triggerManualNotification selects and records new motivation`() = runTest {
        val today = getCurrentDateKey()
        val mockMotivation = createMockMotivationWithHistory()
        val newMotivation = createMockMotivationWithHistory(id = 2L)
        
        // Setup initial state
        whenever(motivationRepository.getHistoryByDate(today)).thenReturn(listOf(mockMotivation))
        whenever(motivationRepository.getUnseenCount()).thenReturn(50)
        whenever(preferencesRepository.getPreferences()).thenReturn(UserPreferences())
        whenever(motivationRepository.selectRandomUnseen(emptyList())).thenReturn(newMotivation.item)
        
        viewModel = HomeViewModel(motivationRepository, preferencesRepository)
        advanceUntilIdle()
        
        // Trigger manual notification
        viewModel.triggerManualNotification()
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state is HomeUiState.Success)
    }
    
    private fun getCurrentDateKey(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(Date())
    }
    
    private fun createMockMotivationWithHistory(
        id: Long = 1L,
        shownAt: Long = System.currentTimeMillis()
    ): MotivationWithHistory {
        val item = MotivationItem(
            id = id,
            quote = "Test quote",
            author = "Test Author",
            context = "Test context",
            imageUri = "android.resource://test/drawable/test",
            themes = listOf("motivation", "success"),
            sourceName = "Test Source",
            sourceUrl = "https://test.com",
            license = "Public Domain"
        )
        
        return MotivationWithHistory(
            item = item,
            shownAt = shownAt,
            dateKey = getCurrentDateKey(),
            deliveryStatus = DeliveryStatus.DELIVERED
        )
    }
}
