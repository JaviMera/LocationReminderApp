package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminder_getReminderById() = runBlockingTest {

        // Given
        val reminderDto = ReminderDTO(
            "title",
            "description",
            "location",
            10.0,
            10.0
        )

        // When
        database.reminderDao().saveReminder(reminderDto)
        val newReminder = database.reminderDao().getReminderById(reminderDto.id)

        // Then
        assertThat<ReminderDTO>(newReminder as ReminderDTO, notNullValue())
        assertThat(newReminder.id, `is`(reminderDto.id))
        assertThat(newReminder.title, `is`(reminderDto.title))
        assertThat(newReminder.description, `is`(reminderDto.description))
        assertThat(newReminder.location, `is`(reminderDto.location))
        assertThat(newReminder.longitude, `is`(reminderDto.longitude))
        assertThat(newReminder.latitude, `is`(reminderDto.latitude))
    }

    @Test
    fun insertReminder_getReminders_returnsOneReminder() = runBlockingTest {

        // Given
        val reminderDto = ReminderDTO(
            "title",
            "description",
            "location",
            10.0,
            10.0
        )

        // When
        database.reminderDao().saveReminder(reminderDto)

        // Then
        assertThat(database.reminderDao().getReminders().size, `is`(1))
    }

    @Test
    fun insertTwoReminders_getReminders_returnsTwoReminders() = runBlockingTest {

        // Given
        val reminder1Dto = ReminderDTO(
            "title1",
            "description1",
            "location1",
            10.0,
            10.0
        )

        val reminder2Dto = ReminderDTO(
            "title2",
            "description2",
            "location2",
            10.0,
            10.0
        )

        // When
        database.reminderDao().saveReminder(reminder1Dto)
        database.reminderDao().saveReminder(reminder2Dto)

        // Then
        assertThat(database.reminderDao().getReminders().size, `is`(2))
    }

    @Test
    fun deleteReminders_getReminders_returnsZeroReminders() = runBlockingTest {

        // Given
        val reminder1Dto = ReminderDTO(
            "title1",
            "description1",
            "location1",
            10.0,
            10.0
        )

        val reminder2Dto = ReminderDTO(
            "title2",
            "description2",
            "location2",
            10.0,
            10.0
        )

        // When
        database.reminderDao().saveReminder(reminder1Dto)
        database.reminderDao().saveReminder(reminder2Dto)
        database.reminderDao().deleteAllReminders()

        // Then
        assertThat(database.reminderDao().getReminders().size, `is`(0))
    }
}