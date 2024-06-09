package com.opsc.timeriseprojectmain

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: FloatingActionButton
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var timesheetEntryAdapter: TimesheetEntryAdapter2
    private lateinit var imageViewCategoryPhoto: ImageView
    private lateinit var selectedImageUri: Uri
    private lateinit var dialogImageViewCategoryPhoto: ImageView

    private lateinit var buttonSetMinHours: Button
    private lateinit var buttonSetMaxHours: Button
    private lateinit var buttonViewPerformance: Button
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private lateinit var textViewChartStatus: TextView
    private var minHours: Float = 0f
    private var maxHours: Float = 0f

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_IMAGE_PICK = 102
        private const val REQUEST_CAMERA_PERMISSION = 123
        private const val REQUEST_EXTERNAL_STORAGE_PERMISSION = 124
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        selectedImageUri = Uri.EMPTY

        recyclerView = findViewById(R.id.recycler_view_categories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(emptyList()) { category ->
            val intent = Intent(this, CategoryDetailActivity::class.java).apply {
                putExtra("CATEGORY_ID", category.id)
            }
            startActivity(intent)
        }
        recyclerView.adapter = categoryAdapter

        val timesheetRecyclerView: RecyclerView = findViewById(R.id.recycler_view_soonest_time_entries)
        timesheetRecyclerView.layoutManager = LinearLayoutManager(this)
        timesheetEntryAdapter = TimesheetEntryAdapter2(emptyList())
        timesheetRecyclerView.adapter = timesheetEntryAdapter

        addButton = findViewById<FloatingActionButton>(R.id.button_add_category)
        addButton.setOnClickListener {
            showAddCategoryDialog()
        }

        buttonSetMinHours = findViewById(R.id.button_set_min_hours)
        buttonSetMaxHours = findViewById(R.id.button_set_max_hours)
        buttonViewPerformance = findViewById(R.id.button_view_performance)

        buttonSetMinHours.setOnClickListener {
            showSetMinHoursDialog()
        }

        buttonSetMaxHours.setOnClickListener {
            showSetMaxHoursDialog()
        }

        buttonViewPerformance.setOnClickListener {
            loadUserData()
        }



        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)
        textViewChartStatus = findViewById(R.id.textViewChartStatus)

        // Set default dates
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val startDate = calendar.time

        SettingsManager.getWorkHours { min, max ->
            minHours = min
            maxHours = max
            updateChart(startDate, endDate)
        }

        loadWorkHours()
        loadUserData()

    }

    private fun loadUserData() {
        CategoryManager.loadCategoriesFromFirestore { categories ->
            categoryAdapter.categories = categories
            categoryAdapter.notifyDataSetChanged()
        }
        TimesheetManager.loadTimesheetEntriesFromFirestore { timesheetEntries ->
            timesheetEntryAdapter.updateEntries(timesheetEntries)
            updateChartForAllEntries(timesheetEntries)
            updatePieChart(timesheetEntries)
        }

    }

    private fun loadWorkHours() {
        SettingsManager.getWorkHours { min, max ->
            minHours = min
            maxHours = max
        }
    }

    private fun updateChart(startDate: Date, endDate: Date) {
        val entries = TimesheetManager.getEntriesWithinDateRange(startDate, endDate)
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

    private fun updateChartForAllEntries(entries: List<TimesheetEntry>) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val startDate = calendar.time

        val filteredEntries = entries.filter { it.date in startDate..endDate }
            .groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.date) }
            .map { (date, entries) ->
                val totalHours = entries.sumByDouble {
                    calculateHours(it.startTime, it.endTime)
                }
                Entry(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).time.toFloat(), totalHours.toFloat())
            }.sortedBy { it.x } // Sort entries by date

        val dataSet = LineDataSet(filteredEntries, "Hours Worked")
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

    private fun updatePieChart(entries: List<TimesheetEntry>) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.time

        val groupedEntries = entries.filter { it.date in startDate..endDate }
            .groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.date) }
        var totalDaysWithinRange = 0
        var totalDays = groupedEntries.size

        groupedEntries.forEach { (_, dailyEntries) ->
            val totalHours = dailyEntries.sumByDouble {
                calculateHours(it.startTime, it.endTime)
            }
            if (totalHours in minHours..maxHours) {
                totalDaysWithinRange++
            }
        }

        val percentageWithinRange = if (totalDays > 0) (totalDaysWithinRange.toFloat() / totalDays) * 100 else 0f
        val percentageOutsideRange = 100 - percentageWithinRange

        val pieEntries = listOf(
            PieEntry(percentageWithinRange, ""),
            PieEntry(percentageOutsideRange, "")
        )

        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = listOf(Color.GREEN, Color.RED)
        pieDataSet.valueTextColor = Color.TRANSPARENT // Set text color to transparent
        pieDataSet.valueTextSize = 0f // Set text size to zero
        pieDataSet.setDrawValues(false) // Explicitly disable drawing values

        val pieData = PieData(pieDataSet)

        // Custom ValueFormatter that returns an empty string
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return ""
            }
        })

        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)

        // Set the legend text color to white and labels for the legend
        val legend = pieChart.legend
        legend.textColor = Color.WHITE
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.CENTER
        legend.setCustom(listOf(
            com.github.mikephil.charting.components.LegendEntry("Within Range", com.github.mikephil.charting.components.Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, Color.GREEN),
            com.github.mikephil.charting.components.LegendEntry("Outside Range", com.github.mikephil.charting.components.Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, Color.RED)
        ))

        pieChart.invalidate() // Refresh the chart

        textViewChartStatus.text = if (percentageWithinRange < 20) "Status: Poor (No Badge)"
        else if (percentageWithinRange < 50) "Status: Poor (Effort Badge)"
        else if (percentageWithinRange < 70) "Status: Consistent  (Consistency Badge)"
        else "Status: Excellence (Excellence Badge)"
    }

    private fun calculateHours(startTime: String, endTime: String): Double {
        val start = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(startTime).time
        val end = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(endTime).time
        return (end - start) / (1000 * 60 * 60).toDouble() // Convert milliseconds to hours
    }

    private fun showAddCategoryDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val editTextCategoryName = view.findViewById<EditText>(R.id.editText_category_name)
        dialogImageViewCategoryPhoto = view.findViewById(R.id.imageView_category_photo)
        val btnAddPhoto = view.findViewById<Button>(R.id.button_add_photo)

        btnAddPhoto.setOnClickListener {
            showImagePickerDialog()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Category")
            .setView(view)
            .setPositiveButton("Add") { dialog, _ ->
                val categoryName = editTextCategoryName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    val newCategory = CategoryManager.addCategory(categoryName, selectedImageUri)
                    categoryAdapter.categories = CategoryManager.getAllCategories()
                    categoryAdapter.notifyItemInserted(categoryAdapter.categories.size - 1)
                    recyclerView.scrollToPosition(categoryAdapter.categories.size - 1)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
        dialogImageViewCategoryPhoto.setImageURI(selectedImageUri)
    }

    private fun showSetMinHoursDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.dialog_set_hours, null)
        val editTextMinHours = view.findViewById<EditText>(R.id.editText_hours)

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Minimum Hours")
            .setView(view)
            .setPositiveButton("Set") { dialog, _ ->
                minHours = editTextMinHours.text.toString().toFloatOrNull() ?: 0f
                SettingsManager.saveWorkHours(this, minHours, maxHours)
                TimesheetManager.loadTimesheetEntriesFromFirestore { timesheetEntries ->
                    updatePieChart(timesheetEntries)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showSetMaxHoursDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.dialog_set_hours, null)
        val editTextMaxHours = view.findViewById<EditText>(R.id.editText_hours)

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Maximum Hours")
            .setView(view)
            .setPositiveButton("Set") { dialog, _ ->
                maxHours = editTextMaxHours.text.toString().toFloatOrNull() ?: 24f // Default to 24 hours if input is invalid
                SettingsManager.saveWorkHours(this, minHours, maxHours)
                TimesheetManager.loadTimesheetEntriesFromFirestore { timesheetEntries ->
                    updatePieChart(timesheetEntries)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
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
        // Ensure read media images permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, open gallery
            Log.d("Gallery", "Permission granted, opening gallery")
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        } else {
            // Permission not granted, request it
            Log.d("Gallery", "Permission not granted, requesting it")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_EXTERNAL_STORAGE_PERMISSION)
        }
    }

    private fun openCamera() {
        // Ensure camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, open camera
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    selectedImageUri = getImageUri(imageBitmap)
                    // Update the ImageView with the selected image URI
                    dialogImageViewCategoryPhoto.setImageURI(selectedImageUri)
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let {
                        this.selectedImageUri = it
                        // Update the ImageView with the selected image URI
                        dialogImageViewCategoryPhoto.setImageURI(selectedImageUri)
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

    class CategoryAdapter(
        var categories: List<Category>,
        private val onClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(view: View, val onClick: (Category) -> Unit) : RecyclerView.ViewHolder(view) {
            private val nameTextView: TextView = view.findViewById(R.id.text_category_name)
            private val imageViewCategory: ImageView = view.findViewById(R.id.image_category) // ImageView for category image
            private var currentCategory: Category? = null

            init {
                view.setOnClickListener {
                    currentCategory?.let { onClick(it) }
                }
            }

            fun bind(category: Category) {
                currentCategory = category
                nameTextView.apply {
                    text = category.name
                    setTextColor(Color.WHITE) // Set text color to white
                }

                category.imageUriString.let {
                    if (it.startsWith("content://")) {
                        imageViewCategory.setImageURI(Uri.parse(it))
                    } else {
                        Glide.with(imageViewCategory.context)
                            .load(it)
                            .into(imageViewCategory)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return CategoryViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.bind(category)
        }

        override fun getItemCount() = categories.size
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadWorkHours()
    }
}
