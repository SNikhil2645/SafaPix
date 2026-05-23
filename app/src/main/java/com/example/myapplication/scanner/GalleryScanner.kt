package com.example.myapplication.scanner

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import com.example.myapplication.model.ImageHash
import com.example.myapplication.model.Photo

object GalleryScanner {

    fun getAllPhotos(context: Context): List<Photo> {

        val photoList = mutableListOf<Photo>()

        val collection =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val cursor = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            null
        )

        cursor?.use {

            val idColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Images.Media._ID
                )

            val nameColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Images.Media.DISPLAY_NAME
                )

            val sizeColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Images.Media.SIZE
                )

            val widthColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Images.Media.WIDTH
                )

            val heightColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Images.Media.HEIGHT
                )

            while (it.moveToNext()) {

                val id = it.getLong(idColumn)

                val name = it.getString(nameColumn)

                val size = it.getLong(sizeColumn)

                val width = it.getInt(widthColumn)

                val height = it.getInt(heightColumn)

                val uri = ContentUris.withAppendedId(
                    collection,
                    id
                )

                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(
                        context.contentResolver,
                        uri
                    )

                val hash =
                    ImageHash.averageHash(bitmap)

                photoList.add(
                    Photo(
                        uri = uri,
                        name = name,
                        size = size,
                        width = width,
                        height = height,
                        hash = hash
                    )
                )
            }
        }

        return photoList
    }
}