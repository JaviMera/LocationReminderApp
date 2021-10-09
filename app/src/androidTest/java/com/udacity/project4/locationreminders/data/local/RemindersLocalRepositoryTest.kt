package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Class under test
    private lateinit var _remindersRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun createRepository(){
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        _remindersRepository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main)
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun zeroReminders_getReminders_returnsSuccessEmptyReminders() = runBlocking {

        // Given..no reminders are saved

        // When
        val reminders = _remindersRepository.getReminders() as Result.Success

        // Then
        assertThat(reminders.data.size, `is`(0))
    }

    @Test
    @Throws(Exception::class)
    fun oneReminders_getReminders_returnsSuccessWithOneReminders() = runBlocking {

        // Given..no reminders are saved
        val reminder = ReminderDTO("title", "descriptino", "locaiton", 10.0, 10.0)
        _remindersRepository.saveReminder(reminder)

        // When
        val reminders = _remindersRepository.getReminders() as Result.Success

        // Then
        assertThat(reminders.data.size, `is`(1))
    }

    @Test
    @Throws(Exception::class)
    fun oneReminders_getReminderById_returnsSuccessWithOneReminders() = runBlocking {

        // Given..no reminders are saved
        val reminder = ReminderDTO("title", "descriptino", "locaiton", 10.0, 10.0)
        _remindersRepository.saveReminder(reminder)

        // When
        val reminders = _remindersRepository.getReminder(reminder.id) as Result.Success

        // Then
        assertThat(reminders.data.id, `is`(reminder.id))
    }

    @Test
    @Throws(Exception::class)
    fun reminderDoesntExist_getReminderById_returnsError() = runBlocking {

        // Given..no reminders are saved
        val reminder = ReminderDTO("title", "descriptino", "locaiton", 10.0, 10.0)
        _remindersRepository.saveReminder(reminder)

        // When
        val reminders = _remindersRepository.getReminder("fake") as Result.Error

        // Then
        assertThat(reminders.message, `is`("Reminder not found!"))
    }

    @Test
    fun saveReminders_deleteReminders_getRemindersReturnsZero() = runBlocking {

        // Given..no reminders are saved
        val reminder1 = ReminderDTO("title1", "descriptino1", "locaiton", 10.0, 10.0)
        val reminder2 = ReminderDTO("title2", "descriptino2", "locaiton", 10.0, 10.0)
        _remindersRepository.saveReminder(reminder1)
        _remindersRepository.saveReminder(reminder2)
        _remindersRepository.deleteAllReminders()

        // When
        val reminders = _remindersRepository.getReminders() as Result.Success

        // Then
        assertThat(reminders.data.size, `is`(0))
    }
}