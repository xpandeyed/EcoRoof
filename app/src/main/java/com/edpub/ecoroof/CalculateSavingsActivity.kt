package com.edpub.ecoroof


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.edpub.ecoroof.databinding.ActivityCalculateSavingsBinding
import org.json.JSONObject
import java.lang.Double.max
import java.lang.Double.min
import java.time.LocalDate

class CalculateSavingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculateSavingsBinding
    private val TAG = "CalculateSavingsActivityTag"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalculateSavingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        binding.cvGetSavings.setOnClickListener {
            getRawData(latitude, longitude)
        }
    }

    private fun getRawData(latitude: Double, longitude: Double) {

        val queue = Volley.newRequestQueue(this)

        val url =
            "https://archive-api.open-meteo.com/v1/archive?latitude=${latitude}&longitude=${longitude}&start_date=2022-01-01&end_date=2022-01-31&daily=temperature_2m_mean,shortwave_radiation_sum&timezone=auto"


        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                extractData(response)
            },
            { error ->
                Log.i(TAG, error.toString())
            }
        )

        queue.add(jsonObjectRequest)


    }

    private fun extractData(result: JSONObject) {
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

        val area = binding.etArea.text.toString().toDouble()
        val iseerRating = binding.etRating.text.toString().toDouble()

        getTotalEnergy(temperatureAndRadiations, area, iseerRating)
    }

    private fun getTotalEnergy(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>, area: Double, iseerRating: Double): Double{
        var energy = 0.0
        energy+=getEnergyDueToTemperatureChange(temperatureAndRadiations, area, iseerRating)
        energy+=getEnergyDueToReflectedRadiation(temperatureAndRadiations, area, iseerRating)
        return energy
    }

    private fun energySavingForOneDay(temperature: Double, radiation: Double){
        var energy = 0.0

    }


    private fun getEnergyDueToTemperatureChange(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>, area: Double, iseerRating: Double): Double{
        var energy = 0.0

        for((key, value) in temperatureAndRadiations){
            val currDayTemperature = value.first
            if(currDayTemperature<=Utils.max_temp) continue;

            val difference = Utils.max_temp - currDayTemperature
            val temperatureDrop = min(difference, Utils.tempChange)
            val volume = area*Utils.roomHeight
            val airMassInRoom = volume*Utils.airDensity


            val energySavingsForDay = airMassInRoom * Utils.specificHeat * temperatureDrop //energy = mass * specificHeat * temperatureChange

            energy+=energySavingsForDay

        }

        return energy

    }

    private fun getEnergyDueToReflectedRadiation(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>, area: Double, iseerRating: Double): Double{
        var energy = 0.0

        for((key, value) in temperatureAndRadiations){
            val currDayRadiation = value.second*area

        }


        return energy
    }

    private fun getElectricalUnits(): Double{
        var energy = 0.0
        return energy
    }

}