package com.a00n.locateme.ui.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import com.a00n.locateme.utils.GpsStatusListener

class GpsReceiver(private val gpsStatusListener: GpsStatusListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            // GPS status has changed
            val isGPSEnabled = isGPSEnabled(context!!)
            gpsStatusListener.onGpsStatusChanged(isGPSEnabled)
//            if (isGPSEnabled) {
//                Log.i("info", "onReceive: enabled")
//            } else {
//                Log.i("info", "onReceive: disabled")
//            }
        }
    }

    private fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

}