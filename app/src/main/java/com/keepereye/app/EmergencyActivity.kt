package com.keepereye.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.keepereye.app.databinding.ActivityEmergencyBinding
import com.keepereye.app.emergency.EmergencyContactManager
import com.keepereye.app.tts.TextToSpeechManager

class EmergencyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyBinding
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var contactManager: EmergencyContactManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsManager = TextToSpeechManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        contactManager = EmergencyContactManager(this)

        setupUI()
        ttsManager.speak("Modo emergencia. Toca el botón grande para enviar una alerta con tu ubicación.")
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSendAlert.setOnClickListener {
            sendEmergencyAlert()
        }

        binding.btnConfigureContact.setOnClickListener {
            showContactInput()
        }

        val contact = contactManager.getEmergencyContact()
        if (contact != null) {
            binding.tvContactInfo.text = "Contacto: $contact"
            binding.tvContactInfo.visibility = View.VISIBLE
        } else {
            binding.tvContactInfo.text = getString(R.string.emergency_no_contacts)
            binding.tvContactInfo.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendEmergencyAlert() {
        val contact = contactManager.getEmergencyContact()
        if (contact == null) {
            ttsManager.speak(getString(R.string.emergency_no_contacts))
            binding.tvStatus.text = getString(R.string.emergency_no_contacts)
            return
        }

        binding.tvStatus.text = getString(R.string.emergency_sending)
        binding.btnSendAlert.isEnabled = false
        ttsManager.speak("Obteniendo ubicación y enviando alerta")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val cancellationToken = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location: Location? ->
                val message = if (location != null) {
                    "EMERGENCIA - KEEPER-EYE: Necesito ayuda. Mi ubicación: " +
                        "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "EMERGENCIA - KEEPER-EYE: Necesito ayuda. No se pudo obtener la ubicación."
                }
                sendSMS(contact, message)
            }.addOnFailureListener {
                val message = "EMERGENCIA - KEEPER-EYE: Necesito ayuda urgente."
                sendSMS(contact, message)
            }
        } else {
            val message = "EMERGENCIA - KEEPER-EYE: Necesito ayuda urgente."
            sendSMS(contact, message)
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val smsManager = SmsManager.getDefault()
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

                runOnUiThread {
                    binding.tvStatus.text = getString(R.string.emergency_sent)
                    binding.btnSendAlert.isEnabled = true
                    ttsManager.speak(getString(R.string.emergency_sent))
                }
            } else {
                runOnUiThread {
                    binding.tvStatus.text = getString(R.string.sms_permission_denied)
                    binding.btnSendAlert.isEnabled = true
                    ttsManager.speak(getString(R.string.sms_permission_denied))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS sending failed", e)
            runOnUiThread {
                binding.tvStatus.text = getString(R.string.emergency_failed)
                binding.btnSendAlert.isEnabled = true
                ttsManager.speak(getString(R.string.emergency_failed))
            }
        }
    }

    private fun showContactInput() {
        val editText = android.widget.EditText(this).apply {
            hint = "Número de teléfono"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            contentDescription = "Ingresa el número de teléfono de emergencia"
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Contacto de emergencia")
            .setMessage("Ingresa el número de teléfono")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val number = editText.text.toString().trim()
                if (number.isNotBlank()) {
                    contactManager.saveEmergencyContact(number)
                    binding.tvContactInfo.text = "Contacto: $number"
                    ttsManager.speak("Contacto de emergencia guardado: $number")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "EmergencyActivity"
    }
}
