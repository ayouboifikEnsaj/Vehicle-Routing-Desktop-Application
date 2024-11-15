package com.a00n.locateme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.a00n.locateme.data.entities.Position
import com.a00n.locateme.databinding.ActivityMainBinding
import com.a00n.locateme.ui.broadcasts.GpsReceiver
import com.a00n.locateme.ui.viewmodels.MainViewModel
import com.a00n.locateme.utils.GpsStatusListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import java.util.Date

class MainActivity : AppCompatActivity(), GpsStatusListener, OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private lateinit var viewModel: MainViewModel

    private var counter: Int = 1

    private var isSetting: Boolean = false
    private lateinit var mMap: GoogleMap

    private val gpsReceiver: GpsReceiver by lazy { GpsReceiver(this) }

    private val telephonyManager: TelephonyManager by lazy {
        getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val latitude = location.latitude
            val longitude = location.longitude

            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_PHONE_STATE), 101)
            }
            Log.i("info", "onLocationChanged: Latitude: $latitude, Longitude: $longitude ")
            val deviceId = android.provider.Settings.Secure.getString(this@MainActivity.contentResolver,android.provider.Settings.Secure.ANDROID_ID);
            Log.i("info", "onCreate: $deviceId")
            val position = Position(0,latitude,longitude,viewModel.formatDateToString(Date()),deviceId)
            Snackbar.make(binding.root,"Latitude: $latitude, Longitude: $longitude",Snackbar.LENGTH_LONG).show()
            viewModel.addPosition(position)
            val newPosition = LatLng(latitude,longitude)
            mMap.addMarker(MarkerOptions().position(newPosition).title("Position ${position.id} at ${position.date}"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(newPosition))
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            var newStatus = ""
            when (status) {
                LocationProvider.OUT_OF_SERVICE -> newStatus = "OUT_OF_SERVICE"
                LocationProvider.TEMPORARILY_UNAVAILABLE -> newStatus = "TEMPORARILY_UNAVAILABLE"
                LocationProvider.AVAILABLE -> newStatus = "AVAILABLE"
            }
            Log.i("info", "onStatusChanged: $newStatus")
        }

        override fun onProviderDisabled(provider: String) {
            super.onProviderDisabled(provider)
            Log.i("info", "onProviderDisabled: GPS provider is disabled")
        }

        override fun onProviderEnabled(provider: String) {
            super.onProviderEnabled(provider)
            Log.i("info", "onProviderDisabled: GPS provider is enabled")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("info", "onResume: hello from resume")
        if (isSetting) {
            if (checkLocationPermission()) {
                Log.i("info", "onResume: has permi a00n======")
                if (isGpsEnabled()) {
                    startLocationUpdates()
                    toggleViews(false)
                } else {
                    toggleViews(true, "Please enable gps location", true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.materialToolbar)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.grantButton.setOnClickListener {
            if (counter == 2) {
                isSetting = true
                openAppSettings(this)
            } else {
                counter += 1
                requestLocationPermission()
            }
        }
        binding.gpsButton.setOnClickListener {
            requestGpsEnable()
        }

        startFetchingLocation()


        registerReceiver(gpsReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))


    }

    private fun openAppSettings(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }

    private fun startFetchingLocation() {
        if (checkLocationPermission()) {
            if (isGpsEnabled()) {
                startLocationUpdates()
                toggleViews(false)
            } else {
                requestGpsEnable()
                toggleViews(true, "Please enable gps location", true)
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun toggleViews(visible: Boolean, txt: String? = null, isGpsError: Boolean = false) {
        binding.errorLinearLayout.visibility = if (visible) View.VISIBLE else View.GONE
        binding.mapView.visibility =if (visible) View.GONE else View.VISIBLE
        txt.let {
            binding.errorTextView.text = it
        }
        binding.grantButton.visibility = if (isGpsError) View.GONE else View.VISIBLE
        binding.gpsButton.visibility = if (isGpsError) View.VISIBLE else View.GONE
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    private fun requestGpsEnable() {
        // Prompt the user to enable GPS
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            10f,
            locationListener
        )
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("info", "onRequestPermissionsResult: user grant permission")
                if (isGpsEnabled()) {
                    startLocationUpdates()
                    toggleViews(false)
                } else {
                    requestGpsEnable()
                    toggleViews(true, "Please enable gps location", true)
                }
            } else {
                Log.i("info", "onRequestPermissionsResult: user did not grant permission")
                toggleViews(true, resources.getString(R.string.grant_permission_first))
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(gpsReceiver)
    }

    override fun onGpsStatusChanged(isEnabled: Boolean) {
        if (isEnabled) {
            Log.i("info", "onGpsStatusChanged: enabled")
            startLocationUpdates()
            toggleViews(false)
        } else {
            Log.i("info", "onGpsStatusChanged: disabled")
            toggleViews(true, "Please enable gps location", true)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        viewModel.getPositionsList().observe(this){positions ->
            if(positions!=null){
                Log.i("info", "startLocationUpdates: $positions")
                positions.forEach {position->
                    val newPosition = LatLng(position.latitude,position.longitude)
                    mMap.addMarker(MarkerOptions().position(newPosition).title("Position ${position.id} at ${position.date}"))
                }
                val position = positions.last()
                val newPosition = LatLng(position.latitude,position.longitude)
                mMap.addMarker(MarkerOptions().position(newPosition).title("Position ${position.id} at ${position.date}"))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(newPosition))
            }
        }
        viewModel.fetchPositions()
    }

}