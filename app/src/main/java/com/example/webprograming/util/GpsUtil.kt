package com.example.webprograming.util

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

object GpsUtil {

    data class LatLng(val latitude: Double, val longitude: Double)

    fun extractGpsFromPhoto(photoPath: String): LatLng? {
        if (photoPath.isEmpty() || !File(photoPath).exists()) return null

        return try {
            val exif = ExifInterface(photoPath)
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                LatLng(latLong[0].toDouble(), latLong[1].toDouble())
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
