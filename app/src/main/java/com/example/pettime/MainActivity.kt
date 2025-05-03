package com.example.pettime

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.aware.PublishDiscoverySession
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.firebase.firestore.FieldValue
import org.checkerframework.common.returnsreceiver.qual.This

class MainActivity : AppCompatActivity() {
    private lateinit var btnAddPet: Button
    private lateinit var etPetType: EditText
    private lateinit var tvPets: TextView
    private lateinit var etPetName: EditText
    private lateinit var etVaccinationDate: EditText
    private lateinit var btnDeletePet: Button
    private var selectedDate: Calendar = Calendar.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val FIRESTORE_COLLECTION_KEY = "Users"
    private val FIRESTORE_PET_INFO_KEY = "petInfo"
    private var loadingDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPetName = findViewById(R.id.etPetName)
        etVaccinationDate = findViewById(R.id.etVaccinationDate)
        btnAddPet = findViewById(R.id.btnAddPet)
        etPetType = findViewById(R.id.etPetType)
        tvPets = findViewById(R.id.tvPetList)
        btnDeletePet = findViewById(R.id.btnDeletePet)

        etPetType.setOnClickListener {
            showPetTypePicker()
        }

        etVaccinationDate.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v)
                true
            } else {
                false
            }
        }

        etVaccinationDate.setOnClickListener {
            showDateTimePicker()
        }

        btnAddPet.setOnClickListener {
            addPet()
        }

        btnDeletePet.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hayvan Sil")

            val inputEditText = EditText(this)
            inputEditText.hint = "Silinecek hayvanın adını girin"
            builder.setView(inputEditText)

            builder.setPositiveButton("Sil") { dialog, _ ->
                val silinecekIsim = inputEditText.text.toString().trim()
                if (silinecekIsim.isNotEmpty()) {
                    val mevcutListe = tvPets.text.toString()
                    val satirlar = mevcutListe.lines()

                    val guncelSatirlar = satirlar.filter { satir ->
                        !satir.contains("- $silinecekIsim ", ignoreCase = true)
                    }
                    val yeniMetin = guncelSatirlar.joinToString("\n")

                    showLoading()
                    val guncelMap = hashMapOf(FIRESTORE_PET_INFO_KEY to yeniMetin)

                    db.collection(FIRESTORE_COLLECTION_KEY)
                        .document(getDeviceIdForFirebase())
                        .set(guncelMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "$silinecekIsim silindi", Toast.LENGTH_SHORT).show()
                            updatePetsList(yeniMetin)
                            hideLoading()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Silme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                            hideLoading()
                        }
                } else {
                    Toast.makeText(this, "Lütfen silinecek hayvan adını girin", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("İptal") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getPetInfo()
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
        val petType = etPetType.text.toString()
        val vaccinationDate = etVaccinationDate.text.toString().trim()

        if (petName.isNotEmpty() && petType.isNotEmpty() && vaccinationDate.isNotEmpty()) {
            val petData = "$petType - $petName (Aşı: $vaccinationDate)"
            val currentPetInfo = findViewById<TextView>(R.id.tvPetList).text.toString()
            val updatedPetInfo = if (currentPetInfo.isNotEmpty()) {
                "$currentPetInfo\n$petData"
            } else {
                petData
            }

            val updatedMap = hashMapOf(FIRESTORE_PET_INFO_KEY to updatedPetInfo)

            showLoading()
            db.collection(FIRESTORE_COLLECTION_KEY)
                .document(getDeviceIdForFirebase())
                .set(updatedMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "$petName için yeni aşı zamanı eklendi!", Toast.LENGTH_SHORT).show()
                    updatePetsList(updatedPetInfo)
                    scheduleNotification(selectedDate)
                    hideLoading()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ekleme yapılırken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                    hideLoading()
                }
        } else {
            Toast.makeText(this, "Lütfen bir isim ve tarih girin!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPetTypePicker() {
        val petTypes = resources.getStringArray(R.array.pet_types)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Evcil Hayvan Türünü Seç")

        var selectedType = petTypes[0]

        builder.setSingleChoiceItems(petTypes, -1) { _, which ->
            selectedType = petTypes[which]
        }

        builder.setPositiveButton("Seç") { _, _ ->
            findViewById<EditText>(R.id.etPetType).setText(selectedType)
        }

        builder.setNegativeButton("İptal", null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun updatePetsList(updatedText: String) {
        tvPets.text = updatedText
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

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getDeviceIdForFirebase(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }
    private fun getPetInfo() {
        val deviceId = getDeviceIdForFirebase()
        val documentRef = db.collection("Users").document(deviceId)

        showLoading()
        documentRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    tvPets.text = documentSnapshot.getString("petInfo")
                } else {
                    tvPets.text = null
                    Toast.makeText(this, "Veri bulunamadı", Toast.LENGTH_SHORT).show()
                }
                hideLoading()
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Veri alınırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun showLoading() {
        val dialogView = layoutInflater.inflate(R.layout.layout_loading, null)
        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }
}
