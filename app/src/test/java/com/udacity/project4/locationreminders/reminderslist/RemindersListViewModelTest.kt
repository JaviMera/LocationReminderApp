package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var _reminderListViewModel: RemindersListViewModel
    private lateinit var _fakeDataSource: FakeDataSource

    // Needed to test code with LiveData. If we do not use this, we will get the
    // RuntimeException related to Looper in Android.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel(){
        stopKoin()
        _fakeDataSource = FakeDataSource()
        _reminderListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            _fakeDataSource
        )
    }

    @After
    fun clearViewModel() = runBlockingTest{
        _fakeDataSource.deleteAllReminders()
    }

    @Test
    fun zeroReminders_loadReminders_returnsEmptyRemindersList() = mainCoroutineRule.runBlockingTest {

        // Given
        _fakeDataSource.deleteAllReminders()

        // When
        _reminderListViewModel.loadReminders()

        // Then
        assertThat(_reminderListViewModel.remindersList.getOrAwaitValue().size, `is`(0))
    }

    @Test
    fun oneReminder_loadReminders_returnsReminderListWithOneItem() = mainCoroutineRule.runBlockingTest {

        // Given
        _fakeDataSource.saveReminder(ReminderDTO(
            "title", "description", "location", 1.0, 1.0
        ))

        // When
        _reminderListViewModel.loadReminders()

        // Then
        assertThat(_reminderListViewModel.remindersList.getOrAwaitValue().size, `is`(1))
    }

    @Test
    fun zeroReminders_loadReminders_returnsSnackbarError() = mainCoroutineRule.runBlockingTest {

        // Given
        _fakeDataSource.deleteAllReminders()
        _fakeDataSource.forceError(true)

        // When
        _reminderListViewModel.loadReminders()

        // Then
        assertThat(_reminderListViewModel.showSnackBar.getOrAwaitValue(), `is`("Reminders not found"))
    }

    @Test
    fun oneReminders_loadReminders_showLoading() = mainCoroutineRule.runBlockingTest {

        // Given
        mainCoroutineRule.pauseDispatcher()

        _fakeDataSource.saveReminder(ReminderDTO(
            "title", "description", "location", 1.0, 1.0
        ))
        _fakeDataSource.saveReminder(ReminderDTO(
            "title", "description", "location", 1.0, 1.0
        ))

        // When
        _reminderListViewModel.loadReminders()

        // Then
        assertThat(_reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(_reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}