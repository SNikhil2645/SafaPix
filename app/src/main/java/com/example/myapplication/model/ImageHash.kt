package com.example.myapplication.model

import android.graphics.Bitmap
import android.graphics.Color

object ImageHash {

    fun averageHash(bitmap: Bitmap): String {

        val resized =
            Bitmap.createScaledBitmap(bitmap, 8, 8, true)

        val pixels = mutableListOf<Int>()

        var total = 0L

        for (x in 0 until 8) {
            for (y in 0 until 8) {

                val pixel = resized.getPixel(x, y)

                val gray =
                    (
                            Color.red(pixel) +
                                    Color.green(pixel) +
                                    Color.blue(pixel)
                            ) / 3

                pixels.add(gray)

                total += gray
            }
        }

        val average = total / pixels.size
        val tolerance = 25
        val hash = StringBuilder()

        for (pixel in pixels) {

            if (pixel >= average - tolerance){
                hash.append("1")
            } else {
                hash.append("0")
            }
        }

        return hash.toString()
    }
}