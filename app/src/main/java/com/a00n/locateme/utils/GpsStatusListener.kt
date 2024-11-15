package com.a00n.locateme.utils

interface GpsStatusListener {
    fun onGpsStatusChanged(isEnabled: Boolean)
}