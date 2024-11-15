package com.a00n.locateme.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.a00n.locateme.data.entities.Position
import com.a00n.locateme.data.remote.MyRequestQueue
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    var hasPermission: MutableLiveData<Boolean> = MutableLiveData()
    var isGpsEnabled: MutableLiveData<Boolean> = MutableLiveData()

    private val positionsList = MutableLiveData<List<Position>?>()


    fun getPositionsList(): MutableLiveData<List<Position>?> {
        return positionsList
    }

    private val gson: Gson = Gson()

    fun formatDateToString(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(date)
    }

    fun addPosition(position: Position) {
        val url: String = "http://192.168.43.106:8080/positions/create"
        val jsonPosition = gson.toJson(position)
        val request = JsonObjectRequest(
            Request.Method.POST, url, JSONObject(jsonPosition),
            { response ->
                val addedPosition:Position = Gson().fromJson(
                    response.toString(),
                    object : TypeToken<Position>() {}.type
                )
                position.id = addedPosition.id
                Log.i("info", "addPosition: $addedPosition")
            },
            {
                Log.i("info", "Error: ${it.message}")
            }
        )
        MyRequestQueue.getInstance(application.applicationContext).addToRequestQueue(request)
    }

    fun fetchPositions(){
        val url: String = "http://192.168.43.106:8080/positions/all"

        val stringReq = StringRequest(
            Request.Method.GET, url,
            { response ->
                val students: List<Position> = Gson().fromJson(
                    response.toString(),
                    object : TypeToken<List<Position>>() {}.type
                )
                positionsList.value = students
            },
            {
                positionsList.value = null
                Log.i("info", "fetchPositions: ${it.message}")
            }
        )
        MyRequestQueue.getInstance(application.applicationContext).addToRequestQueue(stringReq)
    }
}