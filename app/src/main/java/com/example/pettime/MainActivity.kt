package com.example.pettime

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var btnAddPet: Button
    private lateinit var spinnerPetType: Spinner
    private lateinit var tvPets: TextView
    private lateinit var etPetName: EditText
    private lateinit var etVaccinationDate: EditText // Aşı tarihi için EditText
    private var petsList: MutableList<String> = mutableListOf()
    private var selectedDate: Calendar = Calendar.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPetName = findViewById(R.id.etPetName)
        etVaccinationDate = findViewById(R.id.etVaccinationDate) // Aşı tarihi EditText
        btnAddPet = findViewById(R.id.btnAddPet)
        spinnerPetType = findViewById(R.id.spinnerPetType)
        tvPets = findViewById(R.id.tvPets)

        val petTypes = arrayOf("Kedi", "Köpek", "Kuş")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, petTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPetType.adapter = adapter

        // Aşı tarihi EditText'ine tıklama eventi
        etVaccinationDate.setOnClickListener {
            showDateTimePicker()
        }

        btnAddPet.setOnClickListener {
            addPet()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun showDateTimePicker() {
        val currentDate = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)

                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDate.set(Calendar.MINUTE, minute)

                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        // Aşı tarihini EditText'e yazıyoruz
                        etVaccinationDate.setText(sdf.format(selectedDate.time))
                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    true
                )
                timePicker.show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun addPet() {
        val petName = etPetName.text.toString().trim()
        val petType = spinnerPetType.selectedItem.toString()
        val vaccinationDate = etVaccinationDate.text.toString().trim() // Aşı tarihini buradan alıyoruz

        if (petName.isNotEmpty() && vaccinationDate.isNotEmpty()) {
            val petData = hashMapOf(
                "name" to petName,
                "type" to petType,
                "vaccineDate" to vaccinationDate
            )

            db.collection("pets")
                .add(petData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Pet added successfully!", Toast.LENGTH_SHORT).show()
                    val petInfo = "$petType - $petName (Aşı: $vaccinationDate)"
                    petsList.add(petInfo)
                    updatePetsList()
                    scheduleNotification(selectedDate)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding pet: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        } else {
            Toast.makeText(this, "Lütfen bir isim ve tarih girin!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePetsList() {
        tvPets.text = petsList.joinToString("\n")
    }

    private fun scheduleNotification(date: Calendar) {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, date.timeInMillis, pendingIntent)

        Toast.makeText(this, "Aşı hatırlatıcısı ayarlandı!", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bildirim izni verildi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bildirim izni verilmedi", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
