package com.example.myapplication.model

data class DuplicateGroup(
    val hash: String,
    val original: ImageData,
    val duplicates: List<ImageData>
)
