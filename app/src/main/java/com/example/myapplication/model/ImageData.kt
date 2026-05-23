package com.example.myapplication.model

import android.net.Uri

data class ImageData(
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val dateAdded: Long,
    val hash: String = ""
)
