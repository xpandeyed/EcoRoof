package com.edpub.ecoroof

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONObject

import java.io.ByteArrayOutputStream
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.time.LocalDate


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
    const val tempChange = 3.5 //temperature change on white paint application

    const val concreteReflexivity = 0.35
    const val whitePaintReflexivity = 0.80

    const val concreteEmittance = 0.93
    const val whitePaintEmittance = 0.91


    fun getUriFromBitmap(bitmap: Bitmap, contentResolver: ContentResolver): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, System.currentTimeMillis().toString(), null)
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

    fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,
            100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun jsonToMap(result: JSONObject): HashMap<LocalDate, Pair<Double, Double>>{
        val dataArrays = result.optJSONObject("daily")
        val dateArray = dataArrays.optJSONArray("time")
        val temperatureArray = dataArrays.optJSONArray("temperature_2m_mean")
        val radiationArray = dataArrays.optJSONArray("shortwave_radiation_sum")

        val temperatureAndRadiations = HashMap<LocalDate, Pair<Double, Double>>()

        for (i in 0 until dateArray.length()) {
            val currDate = LocalDate.parse(dateArray.optString(i))
            val currTemp = temperatureArray.optDouble(i)
            val currRadiation = radiationArray.optDouble(i)

            temperatureAndRadiations[currDate] = Pair(currTemp, currRadiation)

        }

        return temperatureAndRadiations
    }

    fun savingsForOneDay(radiation: Double): Double{
        val energy = radiation * (whitePaintEmittance
                + whitePaintReflexivity
                - concreteReflexivity
                - concreteEmittance
                + concreteEmittance* concreteReflexivity
                - whitePaintReflexivity* whitePaintEmittance
                )
        return energy

    }


}