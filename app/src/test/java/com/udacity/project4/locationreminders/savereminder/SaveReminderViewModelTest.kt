package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import com.udacity.project4.locationreminders.data.dto.Result

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var _saveReminderViewModel: SaveReminderViewModel
    private lateinit var _fakeDataSource: FakeDataSource
    private lateinit var _context: Application

    // Needed to test code with LiveData. If we do not use this, we will get the
    // RuntimeException related to Looper in Android.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        stopKoin()
        _fakeDataSource = FakeDataSource()
        _context = ApplicationProvider.getApplicationContext()
        _saveReminderViewModel = SaveReminderViewModel(
            _context,
            _fakeDataSource
        )
    }

    @Test
    fun addNewReminder_showLoadingIsTrue() = mainCoroutineRule.runBlockingTest{

        mainCoroutineRule.pauseDispatcher()

        val reminder = ReminderDataItem("title", "description", "location", 10.0, 10.0, "1")
        _saveReminderViewModel.saveReminder(reminder)

        var showLoading = _saveReminderViewModel.showLoading.getOrAwaitValue()

        assertThat(showLoading, `is`(true))
    }

    @Test
    fun addNewReminderCompletes_showLoadingIsFalse() = mainCoroutineRule.runBlockingTest {

        mainCoroutineRule.pauseDispatcher()

        val reminder = ReminderDataItem("title", "description", "location", 10.0, 10.0, "1")
        _saveReminderViewModel.saveReminder(reminder)

        mainCoroutineRule.resumeDispatcher()
        val showLoading = _saveReminderViewModel.showLoading.getOrAwaitValue()

        assertThat(showLoading, `is`(false))
    }

    @Test
    fun addNewReminder_showToastHasReminderSavedMessage() = mainCoroutineRule.runBlockingTest {

        val reminder = ReminderDataItem("title", "description", "location", 10.0, 10.0, "1")
        _saveReminderViewModel.saveReminder(reminder)

        val showToast = _saveReminderViewModel.showToast.getOrAwaitValue()

        assertThat(showToast, `is`(_context.getString(R.string.reminder_saved)))
    }

    @Test
    fun clearData_clearsAllLiveDataVariables() = mainCoroutineRule.runBlockingTest {

        // Given
        _saveReminderViewModel.reminderTitle.value = "title"
        _saveReminderViewModel.reminderDescription.value = "description"
        _saveReminderViewModel.reminderSelectedLocationStr.value = "location"
        _saveReminderViewModel.latitude.value = 10.0
        _saveReminderViewModel.longitude.value = 10.0
        _saveReminderViewModel.selectedPOI.value = PointOfInterest(LatLng(10.0, 10.0), "", "")

        // When
        _saveReminderViewModel.onClear()

        // Then
        assertThat(_saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(_saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(_saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
        assertThat(_saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(_saveReminderViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(_saveReminderViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }

    @Test
    fun validateEnteredData_NullOrEmptyTitle_ReturnsFalseAndShowSnackbarError(){

        // Given
        val reminder = ReminderDataItem("", "", "", 10.0, 10.0)

        // When
        val result = _saveReminderViewModel.validateEnteredData(reminder)
        val showSnackbar = _saveReminderViewModel.showSnackBarInt.getOrAwaitValue()

        // Then
        assertThat(result, `is`(false))
        assertThat(showSnackbar, `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_NullOrEmptyLocation_ReturnsFalseAndShowSnackbarError(){

        // Given
        val reminder = ReminderDataItem("title", "", "", 10.0, 10.0)

        // When
        val result = _saveReminderViewModel.validateEnteredData(reminder)
        val showSnackbar = _saveReminderViewModel.showSnackBarInt.getOrAwaitValue()

        // Then
        assertThat(result, `is`(false))
        assertThat(showSnackbar, `is`(R.string.err_select_location))
    }

    @Test
    fun validateEnteredData_ValidTitleAndLocation_ReturnsTrue(){

        // Given
        val reminder = ReminderDataItem("title", "", "location", 10.0, 10.0)

        // When
        val result = _saveReminderViewModel.validateEnteredData(reminder)
        // Then
        assertThat(result, `is`(true))
    }

    @Test
    fun saveReminder_DataSourceContainsNewReminder() = mainCoroutineRule.runBlockingTest {

        // Given
        val reminder = ReminderDataItem("title", "description", "location", 10.0, 10.0, "1")

        // When
        _saveReminderViewModel.validateAndSaveReminder(reminder)
        val newReminder = _fakeDataSource.getReminder("1") as Result.Success<ReminderDTO>

        // Then
        assertThat(newReminder.data.id, `is`("1"))
        assertThat(newReminder.data.description, `is`("description"))
        assertThat(newReminder.data.location, `is`("location"))
        assertThat(newReminder.data.longitude, `is`(10.0))
        assertThat(newReminder.data.latitude, `is`(10.0))
    }

    @After
    fun clearViewModel() = mainCoroutineRule.runBlockingTest {
        _fakeDataSource.deleteAllReminders()
    }
}