package com.example.myapplication.model

import android.net.Uri

data class Photo(

    val uri: Uri,

    val name: String,

    val size: Long,

    val width: Int,

    val height: Int,

    val hash: String = ""
)