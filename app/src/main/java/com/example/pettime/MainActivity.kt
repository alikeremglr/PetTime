package com.example.pettime

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var btnAddPet: Button
    private lateinit var spinnerPetType: Spinner
    private lateinit var tvPets: TextView
    private lateinit var etPetName: EditText
    private var petsList: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPetName = findViewById(R.id.etPetName)
        btnAddPet = findViewById(R.id.btnAddPet)
        spinnerPetType = findViewById(R.id.spinnerPetType)
        tvPets = findViewById<TextView>(R.id.tvPets)

        val petTypes = arrayOf("Kedi", "Köpek", "Kuş")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, petTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPetType.adapter = adapter

        btnAddPet.setOnClickListener {
            val selectedPetType = spinnerPetType.selectedItem.toString()
            val petName = etPetName.text.toString()

            if (petName.isNotEmpty()) {
                val petInfo = "$selectedPetType: $petName"
                petsList.add(petInfo)

                updatePetsList()

                etPetName.text.clear()
            }
        }
    }
    private fun updatePetsList() {
        tvPets.text = petsList.joinToString("\n")

    }
}