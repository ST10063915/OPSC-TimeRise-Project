package com.opsc.timeriseprojectmain

import android.net.Uri

data class Category(
    var id: Int = 0,
    var name: String = "",
    var imageUriString: String = "" // Store as String in Firestore
) {
    val imageUri: Uri
        get() = Uri.parse(imageUriString)
}
