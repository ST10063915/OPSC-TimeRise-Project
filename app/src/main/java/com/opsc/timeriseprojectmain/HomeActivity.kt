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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream

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

        buttonSetMinHours.setOnClickListener {
            showSetMinHoursDialog()
        }

        buttonSetMaxHours.setOnClickListener {
            showSetMaxHoursDialog()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadUserData()
        loadWorkHours()
    }

    private fun loadUserData() {
        CategoryManager.loadCategoriesFromFirestore { categories ->
            categoryAdapter.categories = categories
            categoryAdapter.notifyDataSetChanged()
        }
        TimesheetManager.loadTimesheetEntriesFromFirestore { timesheetEntries ->
            timesheetEntryAdapter.updateEntries(timesheetEntries)
        }
    }

    private fun loadWorkHours() {
        SettingsManager.getWorkHours { min, max ->
            minHours = min
            maxHours = max
        }
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
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
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
