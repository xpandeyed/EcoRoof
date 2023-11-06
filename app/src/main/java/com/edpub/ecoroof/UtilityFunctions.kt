package com.edpub.ecoroof

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.ByteArrayOutputStream
import java.util.Date


object UtilityFunctions {

    private val TAG = "UtilityFunctionsTag"

    val acTonnage= mutableListOf<String>("Choose your AC tonnage", "1", "2")

    val temperatureApi = "https://archive-api.open-meteo.com/v1/archive?latitude=52.52&longitude=13.41&start_date=2023-01-31&end_date=2023-01-31&daily=temperature_2m_mean"

    //all temps in degree celcius
    const val min_temp = 18
    const val max_temp = 26
    //if avg temp of day lies in this range: no AC required

    const val roomHeight = 2.7432 //in meters (9 feet height)
    const val airDensity = 1.2 //kg per cubic metres
    const val specificHeat = 1.005 // kJ per kg kelvin at const pressure
    const val tempChange = 8 //temperature change on white paint application

    fun getTotalSavings(latitude: Double, longitude: Double, startDate: Date, endDate: Date, acTonnage: Double){
        val temperatureAndIntensity = HashMap<Date, Pair<Double, Double>>()
//        getRawData(latitude, longitude, startDate, endDate)
    }




    fun getUriFromBitmap(bitmap: Bitmap, contentResolver: ContentResolver): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
            ?: return null
        return Uri.parse(path)
    }

    fun energyDueToTempChange(roofArea: Double): Double{
        val mass = airDensity*(roofArea* roomHeight)
        return mass* specificHeat* tempChange
    }

}