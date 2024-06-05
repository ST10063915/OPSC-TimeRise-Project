package com.opsc.timeriseprojectmain

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SettingsManager {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun saveWorkHours(context: Context, minHours: Float, maxHours: Float) {
        val userId = auth.currentUser?.uid ?: return
        val userSettings = hashMapOf(
            "minHours" to minHours,
            "maxHours" to maxHours
        )

        db.collection("users").document(userId).collection("settings").document("workHours")
            .set(userSettings)
            .addOnSuccessListener {
                Log.d("SettingsManager", "Work hours successfully saved.")
            }
            .addOnFailureListener { e ->
                Log.w("SettingsManager", "Error saving work hours", e)
            }
    }

    fun getWorkHours(callback: (Float, Float) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("settings").document("workHours")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val minHours = document.getDouble("minHours")?.toFloat() ?: 0f
                    val maxHours = document.getDouble("maxHours")?.toFloat() ?: 24f
                    callback(minHours, maxHours)
                } else {
                    callback(0f, 24f)
                }
            }
            .addOnFailureListener { e ->
                Log.w("SettingsManager", "Error getting work hours", e)
                callback(0f, 24f)
            }
    }
}
