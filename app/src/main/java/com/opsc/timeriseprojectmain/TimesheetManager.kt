package com.opsc.timeriseprojectmain

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimesheetManager {
    private val timesheetEntries = mutableListOf<TimesheetEntry>()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    fun loadTimesheetEntriesFromFirestore(callback: (List<TimesheetEntry>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).collection("timesheetEntries")
                .get()
                .addOnSuccessListener { documents ->
                    timesheetEntries.clear()
                    for (document in documents) {
                        val entry = document.toObject<TimesheetEntry>()
                        timesheetEntries.add(entry)
                    }
                    Log.d("TimesheetManager", "Loaded ${documents.size()} timesheet entries from Firestore.")
                    callback(timesheetEntries)
                }
                .addOnFailureListener { e ->
                    Log.w("TimesheetManager", "Error loading timesheet entries from Firestore", e)
                    callback(emptyList())
                }
        } else {
            Log.w("TimesheetManager", "User is not authenticated.")
            callback(emptyList())
        }
    }

    fun addTimesheetEntry(date: Date, startTime: String, endTime: String, description: String, category: Category, photoPath: String? = null) {
        val userId = auth.currentUser?.uid ?: return
        val newEntry = TimesheetEntry(timesheetEntries.size + 1, date, startTime, endTime, description, category, photoPath)
        timesheetEntries.add(newEntry)
        saveEntryToFirestore(newEntry, userId)
    }

    private fun saveEntryToFirestore(entry: TimesheetEntry, userId: String) {
        db.collection("users").document(userId).collection("timesheetEntries").add(entry)
            .addOnSuccessListener { documentReference ->
                Log.d("TimesheetManager", "DocumentSnapshot added with ID: ${documentReference.id}")
                entry.imageUrl?.let {
                    val imageRef = storage.child("images/${documentReference.id}.jpg")
                    imageRef.putFile(Uri.parse(it))
                        .addOnSuccessListener {
                            imageRef.downloadUrl.addOnSuccessListener { uri ->
                                documentReference.update("imageUrl", uri.toString())
                                Log.d("TimesheetManager", "Image uploaded with URL: $uri")
                            }.addOnFailureListener { e ->
                                Log.w("TimesheetManager", "Error getting download URL", e)
                            }
                        }.addOnFailureListener { e ->
                            Log.w("TimesheetManager", "Error uploading image to Firebase Storage", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("TimesheetManager", "Error adding document to Firestore", e)
            }
    }

    fun getEntriesByCategory(categoryId: Int): List<TimesheetEntry> {
        return timesheetEntries.filter { it.category.id == categoryId }
    }

    fun getSoonestEntries(): List<TimesheetEntry> {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return timesheetEntries
            .filter { it.date.after(Date()) } // Filtering future entries
            .sortedBy {
                try {
                    dateTimeFormat.parse("${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.date)} ${it.startTime}")
                } catch (e: ParseException) {
                    null
                }
            }
            .take(25)
    }

    fun calculateTotalHours(startDate: Date, endDate: Date, categoryId: Int): Float {
        return getEntriesByCategory(categoryId).filter { it.date in startDate..endDate }
            .sumByDouble {
                val start = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(it.startTime).time
                val end = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(it.endTime).time
                (end - start) / (1000 * 60 * 60).toDouble() // Convert milliseconds to hours
            }.toFloat()
    }
}
