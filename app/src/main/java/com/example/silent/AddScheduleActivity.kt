package com.example.silent

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    private var isEveryday: Boolean = true
    private var isVibrate: Boolean = false
    private var selectedDays: MutableList<Int> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        hideStatusBar()

        setContentView(R.layout.activity_add_schedule)

        nameEditText = findViewById(R.id.et_schedule_name)
        startButton = findViewById(R.id.btn_start)
        endButton = findViewById(R.id.btn_end)
        everydayButton = findViewById(R.id.btn_everyday)
        customButton = findViewById(R.id.btn_custom)
        silentButton = findViewById(R.id.btn_silent)
        vibrateButton = findViewById(R.id.btn_vibrate)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                onBackPressedDispatcher.onBackPressed()
            } else {
                @Suppress("DEPRECATION")
                onBackPressed()
            }
        }
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

    private fun hideStatusBar() {
        // For API 30+ (Android 11+)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // For compatibility with older versions
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
        }
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
                    startButton.text = String.format("%d:%02d %s",
                        if (hourOfDay == 0) 12 else if (hourOfDay > 12) hourOfDay - 12 else hourOfDay,
                        minute,
                        if (hourOfDay < 12) "AM" else "PM")
                } else {
                    endTimeCalendar = selectedTime
                    endButton.text = String.format("%d:%02d %s",
                        if (hourOfDay == 0) 12 else if (hourOfDay > 12) hourOfDay - 12 else hourOfDay,
                        minute,
                        if (hourOfDay < 12) "AM" else "PM")
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // Changed to false for 12-hour format
        ).show()
    }
    private fun saveSchedule() {
        val name = nameEditText.text.toString()
        if (name.isBlank()) {
            // Show error message if name is blank
            return
        }

        val schedule = Schedule(
            name = name,
            startTime = startTimeCalendar.timeInMillis,
            endTime = endTimeCalendar.timeInMillis,
            isEveryday = isEveryday,
            selectedDays = if (isEveryday) emptyList() else selectedDays.toList(),
            isVibrate = isVibrate
        )

        val resultIntent = Intent().apply {
            putExtra("schedule", schedule)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}