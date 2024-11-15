package com.a00n.locateme.data.entities

import java.util.Date

data class Position(
    var id: Int,
    val latitude: Double,
    val longitude: Double,
    val date: String,
    private val imei: String
)
