package com.edpub.ecoroof

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.edpub.ecoroof.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import java.io.IOException


class MapsFragment : Fragment() {

    private val TAG = "MapsFragmentTag"

    private val LOCATION_REQUEST_CODE = 1
    private val DEFAULT_ZOOM = 18

    private lateinit var map: GoogleMap
    private var lastLatitude: Double = 28.56172208815701
    private var lastLongitude: Double = 77.28194072328036
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng(28.56172208815701, 77.28194072328036)

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var binding: FragmentMapsBinding

    private lateinit var mapFragment: SupportMapFragment

    private var callback: OnMapReadyCallback

    init {
        Log.i(TAG, "init block")
        callback = OnMapReadyCallback { googleMap ->

            Log.i(TAG, "callback")

            map = googleMap
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(requireActivity())
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            googleMap.uiSettings.apply {
                isCompassEnabled = false
                isMapToolbarEnabled = false
                isIndoorLevelPickerEnabled = false
            }

            map.setOnMapClickListener { point ->
                map.clear()

                lastLatitude = point.latitude
                lastLongitude = point.longitude

                val marker = MarkerOptions().position(LatLng(point.latitude, point.longitude))
                    .title("Your chosen location")
                map.addMarker(marker)
            }

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "oncreteview")
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onviewcreated")
        mapFragment = childFragmentManager.findFragmentById(R.id.f_map) as SupportMapFragment
        mapFragment.getMapAsync(callback)
        checkPermission()
        initView()
    }

    private fun initView() {
        Log.i(TAG, "initview")

        binding.svLocation.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {

                val location: String = binding.svLocation.query.toString()

                var addressList: List<Address>? = null

                if (location != null || location == "") {
                    val geocoder = Geocoder(requireContext())
                    try {
                        addressList = geocoder.getFromLocationName(location, 1)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    if (addressList!!.isEmpty()) {
                        Snackbar.make(
                            requireContext(),
                            binding.cvZoomIn,
                            "Location cannot be found",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {

                        val address = addressList[0]
                        val latLng = LatLng(address.latitude, address.longitude)

                        map.clear()

                        map.addMarker(MarkerOptions().position(latLng).title(location))

                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                    }


                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        binding.cvZoomIn.setOnClickListener {
            zoomIn()
        }

        binding.cvZoomOut.setOnClickListener {
            zoomOut()
        }

        binding.cvTakeSs.setOnClickListener {
            sendScreenShot()
        }

        binding.cvLocateMe.setOnClickListener {
            locateMe()
        }

    }


    private fun zoomIn() {
        map.animateCamera(CameraUpdateFactory.zoomIn())
    }

    private fun zoomOut() {
        map.animateCamera(CameraUpdateFactory.zoomOut())
    }

    private fun sendScreenShot() {
        map.clear()
        val snapshotReadyCallback: GoogleMap.SnapshotReadyCallback =
            GoogleMap.SnapshotReadyCallback { selectedScreenShot ->
                val intent = Intent(requireActivity(), CalculateSavingsActivity::class.java)
                val uri = Utils.getUriFromBitmap(selectedScreenShot!!, requireActivity().contentResolver).toString()
                intent.putExtra("uri", uri)
                Log.i(TAG, uri)
                intent.putExtra("latitude", lastLatitude)
                intent.putExtra("longitude", lastLongitude)
                startActivity(intent)
            }
        val onMapLoadedCallback: GoogleMap.OnMapLoadedCallback = GoogleMap.OnMapLoadedCallback {
            map.snapshot(snapshotReadyCallback)
        }
        map.setOnMapLoadedCallback(onMapLoadedCallback)
    }


    private fun locateMe() {
        Log.i(TAG, "locateme")
        try {
            if (locationPermissionGranted) {
                Log.i(TAG, "location permission is granted")
                checkPermission()
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {

                        Log.i(TAG, "task is successfull")

                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {

                            Log.i(TAG, "last location is not null")

                            lastLatitude = lastKnownLocation!!.latitude
                            lastLongitude = lastKnownLocation!!.longitude

                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )

                            map.addMarker(
                                MarkerOptions().position(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    )
                                ).title("Your Current Location")
                            )

                        }else{
                            Log.i(TAG, "last location is null")
                        }
                    } else {

                        Log.i(TAG, "moving camera to default location")

                        map.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        map.addMarker(
                            MarkerOptions().position(
                                defaultLocation
                            ).title("Jamia Central Library")
                        )
                    }
                }
            }
            else{
                Log.i(TAG, "location permission is not granted")
            }
        } catch (e: Exception) {
            Log.i(TAG, e.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onresume")
        if (lastKnownLocation != null) {
            map.addMarker(
                MarkerOptions().position(
                    LatLng(
                        lastKnownLocation!!.latitude,
                        lastKnownLocation!!.longitude
                    )
                ).title("Your Current Location")
            )
        }
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }
    }

}