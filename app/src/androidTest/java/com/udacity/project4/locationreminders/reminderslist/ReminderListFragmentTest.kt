package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.FakeDataSource
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before

import org.junit.Rule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
@RunWith(AndroidJUnit4::class)
class ReminderListFragmentTest : KoinTest {

    private val _dataSource:ReminderDataSource by inject()
    private lateinit var _database: RemindersDatabase

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val activityRule = ActivityTestRule(RemindersActivity::class.java)

    @get:Rule
    var runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)

    @Before
    fun setupFragment() {
        stopKoin()
        startKoin {
            androidContext(getApplicationContext())
            modules(
                module{
                    viewModel {
                        RemindersListViewModel(
                            get(),
                            get() as FakeDataSource
                        )
                    }
                    single {
                        Room.inMemoryDatabaseBuilder(
                            getApplicationContext(),
                            RemindersDatabase::class.java).allowMainThreadQueries().build() as RemindersDao
                    }

                    single {
                        RemindersLocalRepository(get())
                    }

                    single {
                        FakeDataSource() as ReminderDataSource
                    }
                }
            )
        }
    }

    @Test
    fun saveReminder_displayedOnUi() = runBlockingTest {

        // Given
        val reminder = ReminderDTO("title", "description", "location", 10.0, 10.0)
        _dataSource.saveReminder(reminder)

        // When
        launchFragmentInContainer<ReminderListFragment>()

        // Then
        onView(withText("title")).check(matches(isDisplayed()))
    }
}