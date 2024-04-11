package com.maary.shareas

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.*
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, context.getString(R.string.preference_file_key)))
    })

class PreferencesHelper(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val SETTINGS_HISTORY = booleanPreferencesKey("enabled_history_key")
        val SETTINGS_FINISHED = booleanPreferencesKey("SETTING_FINISHED")
        var DEVICE_HEIGHT= intPreferencesKey("device_height")
        var DEVICE_WIDTH = intPreferencesKey("device_width")
        // Add more keys here as needed
    }

    fun getSettingsFinished(): Flow<Boolean> {
        return dataStore.data
            .map { preferences ->
                Log.v("PH", preferences[SETTINGS_FINISHED].toString())

                preferences[SETTINGS_FINISHED] ?: false
            }
    }

    suspend fun setSettingsFinished(){
        dataStore.edit { settings ->
            settings[SETTINGS_FINISHED] = true
        }
    }

    suspend fun setSettingsHistory(boolean: Boolean) {
        dataStore.edit { settings ->
            settings[SETTINGS_HISTORY] = boolean
        }
    }

    private fun getSettingsHistoryFlow(): Flow<Boolean> {
        return dataStore.data
            .map { preferences ->
                preferences[SETTINGS_HISTORY] ?: false
            }
    }

    fun getSettingsHistory(): Boolean = runBlocking {
        getSettingsHistoryFlow().first()
    }

    private fun getHeightFlow(): Flow<Int> {
        return dataStore.data
            .map { preferences ->
                preferences[DEVICE_HEIGHT] ?: -1
            }
    }

    private fun getWidthFlow(): Flow<Int> {
        return dataStore.data
            .map { preferences ->
                preferences[DEVICE_WIDTH] ?: -1
            }
    }

    fun getHeight(): Int = runBlocking {
        getHeightFlow().first()
    }

    fun getWidth(): Int = runBlocking {
        getWidthFlow().first()
    }

    fun setWidthAndHeight(width: Int, height: Int) = runBlocking {
        dataStore.edit { settings ->
            settings[DEVICE_HEIGHT] = height
            settings[DEVICE_WIDTH] = width
        }
    }




}
