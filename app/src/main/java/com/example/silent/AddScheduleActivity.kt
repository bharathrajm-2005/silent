package com.example.silent

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class AddScheduleActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var startButton: Button
    private lateinit var endButton: Button
    private lateinit var everydayButton: Button
    private lateinit var customButton: Button
    private lateinit var silentButton: ImageButton
    private lateinit var vibrateButton: ImageButton
    private var startTimeCalendar: Calendar = Calendar.getInstance()
    private var endTimeCalendar: Calendar = Calendar.getInstance()
    private var isEveryday: Boolean = true // Default to everyday
    private var isVibrate: Boolean = false // Default to silent
    private var selectedDays: MutableList<Int> = mutableListOf() // Store selected days (0 = Sunday, 1 = Monday, etc.)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        nameEditText = findViewById(R.id.et_schedule_name)
        startButton = findViewById(R.id.btn_start)
        endButton = findViewById(R.id.btn_end)
        everydayButton = findViewById(R.id.btn_everyday)
        customButton = findViewById(R.id.btn_custom)
        silentButton = findViewById(R.id.btn_silent)
        vibrateButton = findViewById(R.id.btn_vibrate)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { onBackPressed() }
        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }

        startButton.setOnClickListener { showTimePickerDialog(true) }
        endButton.setOnClickListener { showTimePickerDialog(false) }

        everydayButton.setOnClickListener {
            isEveryday = true
            updateButtonStyles()
            selectedDays.clear()
        }

        customButton.setOnClickListener {
            isEveryday = false
            updateButtonStyles()
            showCustomDaysDialog()
        }

        silentButton.setOnClickListener {
            isVibrate = false
            updateModeButtonStyles()
        }

        vibrateButton.setOnClickListener {
            isVibrate = true
            updateModeButtonStyles()
        }

        findViewById<FloatingActionButton>(R.id.fab_save).setOnClickListener { saveSchedule() }
    }

    private fun updateButtonStyles() {
        val selectedColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val unselectedColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedTextColor = ContextCompat.getColor(this, android.R.color.black)

        if (isEveryday) {
            everydayButton.setBackgroundColor(selectedColor)
            everydayButton.setTextColor(selectedTextColor)
            customButton.setBackgroundColor(unselectedColor)
            customButton.setTextColor(unselectedTextColor)
        } else {
            everydayButton.setBackgroundColor(unselectedColor)
            everydayButton.setTextColor(unselectedTextColor)
            customButton.setBackgroundColor(selectedColor)
            customButton.setTextColor(selectedTextColor)
        }
    }

    private fun updateModeButtonStyles() {
        val selectedColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val unselectedColor = ContextCompat.getColor(this, android.R.color.transparent)
        val selectedIconColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedIconColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val alphaSelected = 1.0f
        val alphaUnselected = 0.5f

        if (!isVibrate) {
            silentButton.setBackgroundColor(selectedColor)
            silentButton.setColorFilter(selectedIconColor)
            silentButton.alpha = alphaSelected
            vibrateButton.setBackgroundColor(unselectedColor)
            vibrateButton.setColorFilter(unselectedIconColor)
            vibrateButton.alpha = alphaUnselected
        } else {
            silentButton.setBackgroundColor(unselectedColor)
            silentButton.setColorFilter(unselectedIconColor)
            silentButton.alpha = alphaUnselected
            vibrateButton.setBackgroundColor(selectedColor)
            vibrateButton.setColorFilter(selectedIconColor)
            vibrateButton.alpha = alphaSelected
        }
    }

    private fun showCustomDaysDialog() {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val checkedItems = BooleanArray(7) { i -> selectedDays.contains(i) }

        AlertDialog.Builder(this)
            .setTitle("Select Days")
            .setMultiChoiceItems(days, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    if (!selectedDays.contains(which)) {
                        selectedDays.add(which)
                    }
                } else {
                    selectedDays.remove(which)
                }
            }
            .setPositiveButton("OK") { _, _ -> }
            .setNegativeButton("Cancel") { _, _ ->
                if (selectedDays.isEmpty()) {
                    isEveryday = true
                    updateButtonStyles()
                }
            }
            .show()
    }

    private fun showTimePickerDialog(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                if (isStart) {
                    startTimeCalendar = selectedTime
                    startButton.text = String.format("%02d:%02d", hourOfDay, minute)
                } else {
                    endTimeCalendar = selectedTime
                    endButton.text = String.format("%02d:%02d", hourOfDay, minute)
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveSchedule() {
        val name = nameEditText.text.toString()
        if (name.isBlank()) return

        val schedule = Schedule(
            name = name,
            startTime = startTimeCalendar.timeInMillis,
            endTime = endTimeCalendar.timeInMillis,
            isEveryday = isEveryday,
            selectedDays = if (isEveryday) emptyList() else selectedDays.toList(),
            isVibrate = isVibrate // Make sure the field name in the Schedule class is 'isVibrate'
        )

        setResult(RESULT_OK, Intent().apply {
            putExtra("schedule", schedule)
        })
        finish()
    }
}