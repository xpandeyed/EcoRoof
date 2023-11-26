package com.edpub.ecoroof

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.edpub.ecoroof.databinding.ActivityCalculateSavingsBinding
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Double.min
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class CalculateSavingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculateSavingsBinding
    private val TAG = "CalculateSavingsActivityTag"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalculateSavingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val uri = Uri.parse(intent.getStringExtra("uri"))!!

        binding.cvGetSavings.setOnClickListener {
//            getRawData(latitude, longitude)
            uploadImageToApi(uri)
            Log.i(TAG, uri.toString())
        }
    }

    private fun getRawData(latitude: Double, longitude: Double) {

//        Log.i(TAG, "Get Raw Data")

        binding.progressBar.isIndeterminate = true
        val queue = Volley.newRequestQueue(this)
        val url =
            "https://archive-api.open-meteo.com/v1/archive?latitude=${latitude}&longitude=${longitude}&start_date=2022-01-01&end_date=2022-12-31&daily=temperature_2m_mean,shortwave_radiation_sum&timezone=auto"

        Log.i(TAG, url)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                binding.progressBar.isIndeterminate = false
                Log.i(TAG, "Raw Data Received")
                val valuesMap = Utils.jsonToMap(response)
                findCost(valuesMap)
            },
            { error ->
                binding.progressBar.isIndeterminate = false

                Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
                Log.i(TAG, error.toString())
            }
        )

        queue.add(jsonObjectRequest)

    }

    private fun findCost(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>) {
        Log.i(TAG, "Find cost")

        val totalEnergy = getTotalEnergy(temperatureAndRadiations)
        Log.i(TAG, "Total Energy: $totalEnergy")
        val iseerRatio = binding.etRating.text.toString().toDouble()

        if (iseerRatio <= 0.0) {
            Toast.makeText(this, "Invalid ISEER Ratio", Toast.LENGTH_SHORT).show()
            return
        }

        val electricityConsumptionReduced = totalEnergy / iseerRatio
        Log.i(TAG, "Reduced electric consumption: $electricityConsumptionReduced")
        val electricalUnits = getElectricalUnits(electricityConsumptionReduced)
        val costPerUnit = binding.etUnitCost.text.toString().toDouble()
        val savingsInCost = electricalUnits * costPerUnit

        binding.tvCostSaved.text = savingsInCost.toString()
        binding.tvUnitsSaved.text = electricalUnits.toString()
    }

    private fun getTotalEnergy(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>): Double {
//        Log.i(TAG, "Get Total Energy")
        var energy = 0.0
        energy += getEnergyDueToTemperatureChange(temperatureAndRadiations)
        Log.i(TAG, energy.toString())
        energy += getEnergyDueToReflectedRadiation(temperatureAndRadiations)
        Log.i(TAG, energy.toString())
        return energy
    }

    private fun getEnergyDueToTemperatureChange(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>): Double {
//        Log.i(TAG, "Get energy due to temperature change")

        var energy = 0.0
        val area = binding.etArea.text.toString().toDouble()

        for ((key, value) in temperatureAndRadiations) {
            val currDayTemperature = value.first
            Log.i(TAG, currDayTemperature.toString())
            if (currDayTemperature <= Utils.max_temp) continue

            val difference = Utils.max_temp - currDayTemperature
            val temperatureDrop = min(difference, Utils.tempChange)
            val volume = area * Utils.roomHeight
            val airMassInRoom = volume * Utils.airDensity

            val energySavingsForDay =
                airMassInRoom * Utils.specificHeat * temperatureDrop //energy = mass * specificHeat * temperatureChange

            energy += energySavingsForDay

        }

        return energy

    }

    private fun getEnergyDueToReflectedRadiation(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>): Double {
//        Log.i(TAG, "Get energy due to reflected radiation")

        var energy = 0.0
        val area = binding.etArea.text.toString().toDouble()
//        Log.i(TAG, "Area: $area")

        for ((key, value) in temperatureAndRadiations) {

            val currDayTemperature = value.first
            if (currDayTemperature <= Utils.max_temp) continue

            val currDayRadiation = value.second * area * 1e6
            val currDaySaving = Utils.savingsForOneDay(currDayRadiation)
            energy += currDaySaving
        }
        return energy
    }

    private fun getElectricalUnits(energy: Double): Double {
        return energy / (3.6 * 1e6)
    }

    // Function to make the API call
    private fun uploadImageToApi(contentUri: Uri) {
        val file = File(filesDir, "image.jpeg")
        val inputStream = contentResolver.openInputStream(contentUri)
        val outputStream = FileOutputStream(file)
        inputStream!!.copyTo(outputStream)

        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())

        val part = MultipartBody.Part.createFormData("image", file.name, requestBody)

        val retrofit = Retrofit.Builder().baseUrl("https://ecoroofserver.onrender.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            val response = retrofit.uploadImage(part)
            Log.i(TAG, "Response: " + response.toString())
        }

    }

    interface ApiService {
        @Multipart
        @POST("area")
        suspend fun uploadImage(@Part image: MultipartBody.Part): JsonObject
    }

}
