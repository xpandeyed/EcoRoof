package com.edpub.ecoroof

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore

import java.io.ByteArrayOutputStream
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.util.Date


object Utils {

    private val TAG = "UtilityFunctionsTag"

    //all temps in degree Celsius
    //if avg temp of day lies in this range: no AC required
    const val min_temp = 18.0
    const val max_temp = 26.0

    const val averageIseerRating = 4.5

    const val roomHeight = 2.7432 //in meters (9 feet height)
    const val airDensity = 1.2 //kg per cubic metres
    const val specificHeat = 1.005 // kJ per kg kelvin at const pressure
    const val tempChange = 8.0 //temperature change on white paint application

    const val concreteReflexivity = 0.5
    const val whitePainOnConcreteReflexivity = 0.8

    fun getUriFromBitmap(bitmap: Bitmap, contentResolver: ContentResolver): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
            ?: return null
        return Uri.parse(path)
    }

    fun squareFeetToSquareMetre(area: Double) = area/10.764

    fun squareMetreToSquareFeet(area: Double) = area*10.764

    fun getAcTonnage(areaInFeet: Double) = (areaInFeet*25.0)/12000.0

    fun getPowerConsumption(acTonnage: Double) = acTonnage*1000

    fun calculateBoundedArea(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distance = earthRadius * c

        // Assuming a rectangular area for simplicity
        val width = distance
        val height = haversine(lat1, lon1, lat1, lon2)
        val area = width * height

        return area
    }

    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }


}