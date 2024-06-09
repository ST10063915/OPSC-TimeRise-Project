package com.opsc.timeriseprojectmain

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.ByteArrayOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CategoryDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var timesheetAdapter: TimesheetEntryAdapter2
    private lateinit var addButton: Button
    private lateinit var startDateInput: EditText
    private lateinit var endDateInput: EditText
    private lateinit var totalHoursText: TextView
    private lateinit var lineChart: LineChart
    private var categoryId: Int = -1
    private var minHours: Float = 0f
    private var maxHours: Float = 0f
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_IMAGE_PICK = 102
        private const val REQUEST_CAMERA_PERMISSION = 123
        private const val REQUEST_EXTERNAL_STORAGE_PERMISSION = 124
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        categoryId = intent.getIntExtra("CATEGORY_ID", -1)
        recyclerView = findViewById(R.id.recycler_view_timesheet_entries)
        recyclerView.layoutManager = LinearLayoutManager(this)
        timesheetAdapter = TimesheetEntryAdapter2(TimesheetManager.getEntriesByCategory(categoryId))
        recyclerView.adapter = timesheetAdapter

        addButton = findViewById(R.id.button_add_time_entry)
        addButton.setOnClickListener {
            showAddTimeEntryDialog(categoryId)
        }

        startDateInput = findViewById(R.id.startDateInput)
        endDateInput = findViewById(R.id.endDateInput)
        totalHoursText = findViewById(R.id.totalHoursText)
        lineChart = findViewById(R.id.lineChart)

        startDateInput.setOnClickListener {
            showDatePickerDialog(startDateInput)
        }

        endDateInput.setOnClickListener {
            showDatePickerDialog(endDateInput)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        SettingsManager.getWorkHours { min, max ->
            minHours = min
            maxHours = max
            updateTotalHours()
        }

        // Set default dates
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.time

        startDateInput.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(startDate))
        endDateInput.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(endDate))
        updateTotalHours()
    }

    private fun showDatePickerDialog(dateInput: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            dateInput.setText(dateFormatter.format(selectedDate.time))
            updateTotalHours()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateTotalHours() {
        val startDateStr = startDateInput.text.toString()
        val endDateStr = endDateInput.text.toString()

        if (startDateStr.isNotEmpty() && endDateStr.isNotEmpty()) {
            try {
                val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startDateStr)
                val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(endDateStr)
                if (startDate != null && endDate != null && !startDate.after(endDate)) {
                    val totalHours = TimesheetManager.calculateTotalHours(startDate, endDate, categoryId)
                    totalHoursText.text = "Total Hours: $totalHours"
                    updateChart(startDate, endDate)
                } else {
                    totalHoursText.text = "Invalid date range"
                }
            } catch (e: ParseException) {
                totalHoursText.text = "Error parsing dates"
            }
        } else {
            totalHoursText.text = "Please select both start and end dates"
        }
    }

    private fun updateChart(startDate: Date, endDate: Date) {
        val entries = TimesheetManager.getEntriesByCategory(categoryId)
            .filter { it.date in startDate..endDate }
            .groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.date) }
            .map { (date, entries) ->
                val totalHours = entries.sumByDouble {
                    calculateHours(it.startTime, it.endTime)
                }
                Entry(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).time.toFloat(), totalHours.toFloat())
            }.sortedBy { it.x } // Sort entries by date

        val dataSet = LineDataSet(entries, "Hours Worked")
        dataSet.color = Color.BLUE
        dataSet.setCircleColor(Color.BLUE)
        dataSet.valueTextColor = Color.WHITE // Set value text color to white

        val minEntries = listOf(
            Entry(startDate.time.toFloat(), minHours),
            Entry(endDate.time.toFloat(), minHours)
        )

        val maxEntries = listOf(
            Entry(startDate.time.toFloat(), maxHours),
            Entry(endDate.time.toFloat(), maxHours)
        )

        val minDataSet = LineDataSet(minEntries, "Min Hours")
        minDataSet.color = Color.GREEN
        minDataSet.setCircleColor(Color.GREEN)
        minDataSet.axisDependency = YAxis.AxisDependency.LEFT
        minDataSet.valueTextColor = Color.WHITE // Set value text color to white

        val maxDataSet = LineDataSet(maxEntries, "Max Hours")
        maxDataSet.color = Color.RED
        maxDataSet.setCircleColor(Color.RED)
        maxDataSet.axisDependency = YAxis.AxisDependency.LEFT
        maxDataSet.valueTextColor = Color.WHITE // Set value text color to white

        val lineData = LineData(dataSet, minDataSet, maxDataSet)
        lineChart.data = lineData
        lineChart.setNoDataTextColor(Color.WHITE) // Set "no data" text color to white
        lineChart.description.textColor = Color.WHITE // Set description text color to white
        lineChart.legend.textColor = Color.WHITE // Set legend text color to white

        val xAxis = lineChart.xAxis
        xAxis.textColor = Color.WHITE // Set x-axis text color to white
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            override fun getFormattedValue(value: Float): String {
                return dateFormatter.format(Date(value.toLong()))
            }
        }

        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.WHITE // Set left y-axis text color to white

        val rightAxis = lineChart.axisRight
        rightAxis.textColor = Color.WHITE // Set right y-axis text color to white

        lineChart.invalidate() // Refresh the chart
    }

    private fun calculateHours(startTime: String, endTime: String): Double {
        val start = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(startTime).time
        val end = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(endTime).time
        return (end - start) / (1000 * 60 * 60).toDouble() // Convert milliseconds to hours
    }

    private fun showAddTimeEntryDialog(categoryId: Int) {
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_time_entry, null)
        val editTextDescription = view.findViewById<EditText>(R.id.editText_description)
        val editTextDate = view.findViewById<EditText>(R.id.editText_date)
        val editTextStartTime = view.findViewById<EditText>(R.id.editText_start_time)
        val editTextEndTime = view.findViewById<EditText>(R.id.editText_end_time)
        val imageViewEntryPhoto = view.findViewById<ImageView>(R.id.imageView_entry_photo)
        val buttonAddPhoto = view.findViewById<Button>(R.id.button_add_photo)

        buttonAddPhoto.setOnClickListener {
            showImagePickerDialog()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Time Entry")
            .setView(view)
            .setPositiveButton("Add") { dialog, _ ->
                val description = editTextDescription.text.toString().trim()
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(editTextDate.text.toString())
                if (date != null && description.isNotEmpty()) {
                    TimesheetManager.addTimesheetEntry(date, editTextStartTime.text.toString(), editTextEndTime.text.toString(), description, CategoryManager.getCategory(categoryId)!!, selectedImageUri?.toString())
                    timesheetAdapter.updateEntries(TimesheetManager.getEntriesByCategory(categoryId))
                    updateTotalHours()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()

        imageViewEntryPhoto.setImageURI(selectedImageUri)
    }

    private fun showImagePickerDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Image")
            .setMessage("Choose an option to select an image")
            .setPositiveButton("Gallery") { dialog, _ ->
                openGallery()
                dialog.dismiss()
            }
            .setNegativeButton("Camera") { dialog, _ ->
                openCamera()
                dialog.dismiss()
            }
            .show()
    }

    private fun openGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_EXTERNAL_STORAGE_PERMISSION)
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    selectedImageUri = getImageUri(imageBitmap)
                    // Refresh the dialog with the new image
                    showAddTimeEntryDialog(categoryId)
                }
                REQUEST_IMAGE_PICK -> {
                    val uri = data?.data
                    if (uri != null) {
                        selectedImageUri = uri
                        // Refresh the dialog with the new image
                        showAddTimeEntryDialog(categoryId)
                    }
                }
            }
        }
    }

    private fun getImageUri(inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }
}
