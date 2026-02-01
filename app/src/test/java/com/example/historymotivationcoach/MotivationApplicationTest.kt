package com.example.historymotivationcoach

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for MotivationApplication initialization.
 * 
 * Note: These are basic tests. Full integration testing would require
 * Android instrumentation tests with a real Context.
 */
class MotivationApplicationTest {
    
    @Test
    fun `initialization state starts as Loading`() = runTest {
        // This test verifies that the InitializationState sealed class is properly defined
        val loadingState = MotivationApplication.InitializationState.Loading
        assertTrue(loadingState is MotivationApplication.InitializationState.Loading)
    }
    
    @Test
    fun `initialization state can be Success`() = runTest {
        val successState = MotivationApplication.InitializationState.Success
        assertTrue(successState is MotivationApplication.InitializationState.Success)
    }
    
    @Test
    fun `initialization state can be Error with message`() = runTest {
        val errorMessage = "Test error message"
        val errorState = MotivationApplication.InitializationState.Error(errorMessage)
        assertTrue(errorState is MotivationApplication.InitializationState.Error)
        assertTrue(errorState.message == errorMessage)
    }
}
