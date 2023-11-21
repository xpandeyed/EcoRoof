package com.edpub.ecoroof


import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.edpub.ecoroof.databinding.ActivityCalculateSavingsBinding
import org.json.JSONObject
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

        Log.i(TAG, "Get Raw Data")

        binding.progressBar.isIndeterminate = true
        val queue = Volley.newRequestQueue(this)
        val url = "https://archive-api.open-meteo.com/v1/archive?latitude=${latitude}&longitude=${longitude}&start_date=2022-01-01&end_date=2022-12-31&daily=temperature_2m_mean,shortwave_radiation_sum&timezone=auto"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                binding.progressBar.isIndeterminate = false
                Log.i(TAG, "Raw Data Received")
                extractData(response)
            },
            { error ->
                binding.progressBar.isIndeterminate = false

                Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
                Log.i(TAG, error.toString())
            }
        )

        queue.add(jsonObjectRequest)

    }

    private fun extractData(result: JSONObject) {
        Log.i(TAG, "Extract Data")

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

        findCost(temperatureAndRadiations)
    }

    private fun findCost(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>){
        Log.i(TAG, "Find cost")

        val totalEnergy = getTotalEnergy(temperatureAndRadiations)
        Log.i(TAG, "Total Energy: $totalEnergy")
        val iseerRatio = binding.etRating.text.toString().toDouble()

        if(iseerRatio<=0.0){
            Toast.makeText(this, "Invalid ISEER Ratio", Toast.LENGTH_SHORT).show()
            return
        }

        val electricityConsumptionReduced = totalEnergy/iseerRatio
        Log.i(TAG, "Reduced electric consumption: $electricityConsumptionReduced")
        val electricalUnits = getElectricalUnits(electricityConsumptionReduced)
        val costPerUnit = binding.etUnitCost.text.toString().toDouble()
        val savingsInCost = electricalUnits*costPerUnit

        binding.tvCostSaved.text = savingsInCost.toString()
    }

    private fun getTotalEnergy(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>): Double{
        Log.i(TAG, "Get Total Energy")
        var energy = 0.0
        energy+=getEnergyDueToTemperatureChange(temperatureAndRadiations)
        energy+=getEnergyDueToReflectedRadiation(temperatureAndRadiations)
        return energy
    }

    private fun getEnergyDueToTemperatureChange(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>): Double{
        Log.i(TAG, "Get energy due to temperature change")

        var energy = 0.0
        val area = binding.etArea.text.toString().toDouble()

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

    private fun getEnergyDueToReflectedRadiation(temperatureAndRadiations: HashMap<LocalDate, Pair<Double, Double>>): Double{
//        Log.i(TAG, "Get energy due to reflected radiation")

        var energy = 0.0
        val area = binding.etArea.text.toString().toDouble()
        Log.i(TAG, "Area: $area")

        for((key, value) in temperatureAndRadiations){
            val currDayRadiation = value.second*area
            val currDaySaving = currDayRadiation*(Utils.whitePainOnConcreteReflexivity-Utils.concreteReflexivity)
            energy+=currDaySaving
        }
        return energy
    }

    private fun getElectricalUnits(energy: Double): Double {
        return energy / (3.6 * 1e6)
    }

}