package com.opsc.timeriseprojectmain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val delayMillis: Long = 2000 // 2 seconds
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Display loading text
        val loadingText: TextView = findViewById(R.id.loadingText)
        loadingText.text = "Loading..."

        // Initialize Firebase Auth and Firestore
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Ensure the sample user exists
        ensureSampleUserExists()

        // Delay and redirect to sign-in page
        Handler().postDelayed({
            val intent = Intent(this@MainActivity, SignInActivity::class.java)
            startActivity(intent)
            finish() // Close the loading page
        }, delayMillis)
    }

    private fun ensureSampleUserExists() {
        val email = "admin@gmail.com"
        val password = "123456"
        val username = "admin"
        val dateOfBirth = "01-01-2000"

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "Sample user signed in.")
                    initializeSampleData(auth.currentUser!!.uid)
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful) {
                                val user = hashMapOf(
                                    "username" to username,
                                    "email" to email,
                                    "dateOfBirth" to dateOfBirth
                                )
                                db.collection("users").document(auth.currentUser!!.uid)
                                    .set(user)
                                    .addOnSuccessListener {
                                        Log.d("MainActivity", "Sample user created.")
                                        initializeSampleData(auth.currentUser!!.uid)
                                    }
                                    .addOnFailureListener {
                                        Log.w("MainActivity", "Failed to create sample user details.")
                                    }
                            } else {
                                Log.w("MainActivity", "Failed to create sample user.")
                            }
                        }
                }
            }
    }

    private fun initializeSampleData(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        val defaultImageUrl = "https://firebasestorage.googleapis.com/v0/b/opsc-project-1d108.appspot.com/o/screenshot_2024_04_28_191523.png?alt=media&token=92368c1c-9489-4a9a-a1f6-811a7d148d45"

        val sampleCategories = listOf(
            Category(1, "Meetings", defaultImageUrl),
            Category(2, "Work", defaultImageUrl),
            Category(3, "Leisure", defaultImageUrl)
        )

        for (category in sampleCategories) {
            db.collection("users").document(userId).collection("categories")
                .whereEqualTo("id", category.id)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        db.collection("users").document(userId).collection("categories").add(category)
                            .addOnSuccessListener { documentReference ->
                                Log.d("MainActivity", "Category added with ID: ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.w("MainActivity", "Error adding category to Firestore", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("MainActivity", "Error checking category existence", e)
                }
        }

        val sampleEntries = listOf(
            TimesheetEntry(1, calendar.time, "09:00", "17:00", "OPSC Frontend Dev", sampleCategories[1]),
            TimesheetEntry(2, calendar.time, "11:00", "12:00", "OPSC Assignment", sampleCategories[1]),
            TimesheetEntry(3, calendar.time, "11:00", "12:00", "OPSC Backend Dev", sampleCategories[1]),
            TimesheetEntry(4, calendar.time, "11:00", "12:00", "Meeting with Client", sampleCategories[0]),
            TimesheetEntry(5, calendar.time, "12:00", "13:00", "Meeting with CFO", sampleCategories[0]),
            TimesheetEntry(6, calendar.time, "17:00", "19:00", "Watch Netflix", sampleCategories[2]),
            TimesheetEntry(7, calendar.time, "20:00", "22:00", "Clean up apartment", sampleCategories[2])
        )

        for (entry in sampleEntries) {
            db.collection("users").document(userId).collection("timesheetEntries")
                .whereEqualTo("id", entry.id)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        db.collection("users").document(userId).collection("timesheetEntries").add(entry)
                            .addOnSuccessListener { documentReference ->
                                Log.d("MainActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.w("MainActivity", "Error adding document to Firestore", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("MainActivity", "Error checking timesheet entry existence", e)
                }
        }
    }
}
