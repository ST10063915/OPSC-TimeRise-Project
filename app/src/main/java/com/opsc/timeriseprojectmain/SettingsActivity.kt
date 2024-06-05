package com.opsc.timeriseprojectmain

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var minHoursInput: EditText
    private lateinit var maxHoursInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        minHoursInput = findViewById(R.id.minHoursInput)
        maxHoursInput = findViewById(R.id.maxHoursInput)
        val saveButton = findViewById<Button>(R.id.saveGoalSettingsButton)

        saveButton.setOnClickListener {
            val minHours = minHoursInput.text.toString().toFloatOrNull() ?: 0f
            val maxHours = maxHoursInput.text.toString().toFloatOrNull() ?: 24f
            SettingsManager.saveWorkHours(this, minHours, maxHours)
        }
    }
}
