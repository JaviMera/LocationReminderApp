package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import java.lang.Exception

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private val reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var _forceError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if(_forceError)
            return Result.Error("Reminders not found")

        reminders?.let {
            return Success(ArrayList(it))
        }

        return Success(ArrayList<ReminderDTO>())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        if(_forceError)
            return Result.Error("Reminders not found")
        
        try {
            val reminder = reminders?.firstOrNull{it.id == id} ?: return Result.Error("Reminder not found")
            return Result.Success(reminder)
        } catch (e: Exception) {
            return Result.Error(e.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    fun forceError(error: Boolean){
        _forceError = error
    }
}