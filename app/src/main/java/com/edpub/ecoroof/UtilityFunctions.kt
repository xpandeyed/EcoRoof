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


object UtilityFunctions {

    val acTonnage= mutableListOf<String>("Choose your AC tonnage", "1", "2")

    //all temps in degree celcius
    const val min_temp = 18
    const val max_temp = 26
    //if avg temp of day lies in this range: no AC required

    const val roomHeight = 2.7432 //in meters (9 feet height)
    const val airDensity = 1.2 //kg per cubic metres
    const val specificHeat = 1.005 // kJ per kg kelvin at const pressure
    const val tempChange = 8 //temperature change on white paint application

    fun savingInOneDay(r: Float, e: Float) {

    }

    fun getUriFromBitmap(bitmap: Bitmap, contentResolver: ContentResolver): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
            ?: return null
        return Uri.parse(path)
    }

    fun getStringData(context: Context, latitude: String, longitude: String,callback: ResponseCallback){
        Log.i("fetchjson", "on")

        val queue = Volley.newRequestQueue(context)
        val url =
            "https://power.larc.nasa.gov/api/temporal/daily/point?start=20220101&end=20220131&latitude=$latitude&longitude=$longitude&community=ag&parameters=T2M,ALLSKY_SFC_PAR_TOT&format=json&header=true&time-standard=lst"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                Log.i("fetchjson", response.toString())
                callback.onSuccess(response.toString())
            },
            { error ->
                callback.onError(error.toString())
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun converter(myJsonObjectString: String): Map<String, Any> {
        return Gson().fromJson(
            myJsonObjectString, object : TypeToken<HashMap<String?, Any?>?>() {}.type
        )
    }

    fun extractSolarIntensity(){

    }

    fun extractTemperatures(myMap: Map<String, Any>?){

    }

    fun energyDueToTempChange(roofArea: Double): Double{
        val mass = airDensity*(roofArea* roomHeight)

        return mass* specificHeat* tempChange
    }

}

//map[properties][parameter][T2M][each day]