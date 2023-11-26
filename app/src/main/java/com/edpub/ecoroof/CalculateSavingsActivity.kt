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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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
        // Create an OkHttpClient with logging interceptor
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .connectTimeout(30, TimeUnit.SECONDS) // Adjust the timeout as needed
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Create a Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("https://ecoroofserver.onrender.com/") // Base URL of your API
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        // Create an instance of the ApiService
        val apiService = retrofit.create(ApiService::class.java)

        // Get the file from the content URI using ContentResolver
        val inputStream = contentResolver.openInputStream(contentUri)
        val file = createTempFile("image", null, cacheDir)
        file.copyInputStreamToFile(inputStream)

        // Create a request body with the image file
        val requestFile = file.asRequestBody("image/jpeg".toMediaType())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        // Make the API call
        val call = apiService.uploadImage(body)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val result = response.body()
                    Log.i(TAG, result.toString())
                    // Do something with the result
                } else {
                    Log.i(TAG, "API call failed with response code: ${response}")
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                // Log the error message
                Log.i(TAG, "API call failed: ${t.message}", t)
                // Handle failure
            }
        })
    }

    // Extension function to copy InputStream to File
    private fun File.copyInputStreamToFile(inputStream: InputStream?) {
        this.outputStream().use { fileOut ->
            inputStream?.copyTo(fileOut)
        }
    }

    interface ApiService {
        @Multipart
        @POST("area")
        fun uploadImage(@Part image: MultipartBody.Part): Call<JsonObject>
    }




}