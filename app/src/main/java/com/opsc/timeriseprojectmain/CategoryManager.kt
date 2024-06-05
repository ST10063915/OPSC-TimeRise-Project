package com.opsc.timeriseprojectmain

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

object CategoryManager {
    private val categories = mutableListOf<Category>()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun loadCategoriesFromFirestore(callback: (List<Category>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).collection("categories")
                .get()
                .addOnSuccessListener { documents ->
                    categories.clear()
                    for (document in documents) {
                        val category = document.toObject<Category>()
                        categories.add(category)
                    }
                    Log.d("CategoryManager", "Loaded ${documents.size()} categories from Firestore.")
                    callback(categories)
                }
                .addOnFailureListener { e ->
                    Log.w("CategoryManager", "Error loading categories from Firestore", e)
                    callback(emptyList())
                }
        } else {
            Log.w("CategoryManager", "User is not authenticated.")
            callback(emptyList())
        }
    }

    fun addCategory(name: String, imageUri: Uri? = null): Category {
        val userId = auth.currentUser?.uid ?: return Category()
        val newCategory = Category(categories.size + 1, name, imageUri?.toString() ?: "")
        categories.add(newCategory)
        saveCategoryToFirestore(newCategory, userId)
        return newCategory
    }

    private fun saveCategoryToFirestore(category: Category, userId: String) {
        db.collection("users").document(userId).collection("categories").add(category)
            .addOnSuccessListener { documentReference ->
                Log.d("CategoryManager", "Category added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("CategoryManager", "Error adding category to Firestore", e)
            }
    }

    fun getCategory(id: Int) = categories.find { it.id == id }
    fun getAllCategories() = categories.toList()
}
