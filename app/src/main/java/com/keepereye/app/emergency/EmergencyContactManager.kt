package com.keepereye.app.emergency

import android.content.Context
import android.content.SharedPreferences

class EmergencyContactManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveEmergencyContact(phoneNumber: String) {
        prefs.edit().putString(KEY_EMERGENCY_CONTACT, phoneNumber).apply()
    }

    fun getEmergencyContact(): String? {
        return prefs.getString(KEY_EMERGENCY_CONTACT, null)
    }

    fun removeEmergencyContact() {
        prefs.edit().remove(KEY_EMERGENCY_CONTACT).apply()
    }

    companion object {
        private const val PREFS_NAME = "keeper_eye_emergency"
        private const val KEY_EMERGENCY_CONTACT = "emergency_contact"
    }
}
