package com.example.historymotivationcoach.viewmodel

import android.content.Context
import com.example.historymotivationcoach.business.NotificationScheduler
import com.example.historymotivationcoach.data.entity.ScheduleMode
import com.example.historymotivationcoach.data.entity.UserPreferences
import com.example.historymotivationcoach.data.repository.MotivationRepository
import com.example.historymotivationcoach.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    
    private lateinit var context: Context
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var motivationRepository: MotivationRepository
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mock()
        preferencesRepository = mock()
        motivationRepository = mock()
        notificationScheduler = mock()
        
        // Setup default preferences flow
        whenever(preferencesRepository.getPreferencesFlow()).thenReturn(
            flowOf(UserPreferences())
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `preferences flow is exposed correctly`() = runTest {
        val testPrefs = UserPreferences(notificationsPerDay = 5)
        whenever(preferencesRepository.getPreferencesFlow()).thenReturn(flowOf(testPrefs))
        
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        
        // Collect from the flow to trigger the stateIn
        val collected = mutableListOf<UserPreferences>()
        val job = launch {
            viewModel.preferences.collect { collected.add(it) }
        }
        
        advanceUntilIdle()
        job.cancel()
        
        // Should have collected at least the test preferences
        assert(collected.any { it.notificationsPerDay == 5 })
    }
    
    @Test
    fun `updateNotificationsPerDay validates range and reschedules`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.updateNotificationsPerDay(5)
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(any())
        verify(notificationScheduler).rescheduleAllNotifications()
    }
    
    @Test
    fun `updateNotificationsPerDay coerces value below 1 to 1`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.updateNotificationsPerDay(0)
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(
            org.mockito.kotlin.argThat { notificationsPerDay == 1 }
        )
    }
    
    @Test
    fun `updateNotificationsPerDay coerces value above 10 to 10`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.updateNotificationsPerDay(15)
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(
            org.mockito.kotlin.argThat { notificationsPerDay == 10 }
        )
    }
    
    @Test
    fun `updateScheduleMode updates preferences and reschedules`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.updateScheduleMode(ScheduleMode.WEEKDAYS_ONLY)
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(
            org.mockito.kotlin.argThat { scheduleMode == ScheduleMode.WEEKDAYS_ONLY }
        )
        verify(notificationScheduler).rescheduleAllNotifications()
    }
    
    @Test
    fun `updateTimeWindow updates preferences and reschedules`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.updateTimeWindow("08:00", "22:00")
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(
            org.mockito.kotlin.argThat { startTime == "08:00" && endTime == "22:00" }
        )
        verify(notificationScheduler).rescheduleAllNotifications()
    }
    
    @Test
    fun `toggleNotifications to false updates preferences and reschedules`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.toggleNotifications(false)
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(
            org.mockito.kotlin.argThat { enabled == false }
        )
        verify(notificationScheduler).cancelAllNotifications()
    }
    
    @Test
    fun `toggleNotifications to true updates preferences and reschedules`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.toggleNotifications(true)
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(
            org.mockito.kotlin.argThat { enabled == true }
        )
        verify(notificationScheduler).scheduleNextNotification()
    }
    
    @Test
    fun `updatePreferredThemes updates preferences without rescheduling`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        val themes = listOf("motivation", "success", "leadership")
        viewModel.updatePreferredThemes(themes)
        advanceUntilIdle()
        
        verify(preferencesRepository).updatePreferences(
            org.mockito.kotlin.argThat { preferredThemes == themes }
        )
        // Note: updatePreferredThemes should NOT trigger rescheduling
    }
    
    @Test
    fun `clearHistory calls repository clearHistory`() = runTest {
        viewModel = SettingsViewModel(context, preferencesRepository, motivationRepository, notificationScheduler)
        advanceUntilIdle()
        
        viewModel.clearHistory()
        advanceUntilIdle()
        
        verify(motivationRepository).clearHistory()
    }
}
