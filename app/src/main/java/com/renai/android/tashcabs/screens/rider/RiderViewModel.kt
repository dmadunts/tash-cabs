package com.renai.android.tashcabs.screens.rider

import android.location.Address
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.mindorks.ridesharing.simulator.WebSocket
import com.mindorks.ridesharing.simulator.WebSocketListener
import com.parse.ParseException
import com.renai.android.tashcabs.models.Status
import com.renai.android.tashcabs.network.NetworkService
import com.renai.android.tashcabs.network.RetrofitService.geocodingApi
import com.renai.android.tashcabs.utils.*
import kotlinx.coroutines.*
import org.json.JSONObject

class RiderViewModel(networkService: NetworkService) : ViewModel(), WebSocketListener {
    private val TAG = "CommonLogs"

    private val webSocket: WebSocket = networkService.createWebSocket(this)
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)


    private val _onParseException = MutableLiveData<ParseException>()
    val onParseException: LiveData<ParseException>
        get() = _onParseException

    private val _selectedLocation = MutableLiveData<String?>()
    val selectedLocation: LiveData<String?>
        get() = _selectedLocation

    private val _informCabBooked = SingleLiveEvent<Unit>()
    val informCabBooked: LiveData<Unit>
        get() = _informCabBooked

    private val _showPath = MutableLiveData<ArrayList<LatLng>>()
    val showPath: LiveData<ArrayList<LatLng>>
        get() = _showPath

    private val _showNearbyCabs = MutableLiveData<ArrayList<LatLng>>()
    val showNearbyCabs: LiveData<ArrayList<LatLng>>
        get() = _showNearbyCabs

    private val _updateCabLocation = MutableLiveData<LatLng>()
    val updateCabLocation: LiveData<LatLng>
        get() = _updateCabLocation

    private val _informCabIsArriving = SingleLiveEvent<Unit>()
    val informCabIsArriving: LiveData<Unit>
        get() = _informCabIsArriving

    private val _informCabArrived = SingleLiveEvent<Unit>()
    val informCabArrived: LiveData<Unit>
        get() = _informCabArrived

    private val _informTripStarted = SingleLiveEvent<Unit>()
    val informTripStarted: LiveData<Unit>
        get() = _informTripStarted

    private val _informTripEnded = SingleLiveEvent<Unit>()
    val informTripEnded: LiveData<Unit>
        get() = _informTripEnded

    private val _showRoutesNotAvailableError = SingleLiveEvent<Unit>()
    val showRoutesNotAvailableError: LiveData<Unit>
        get() = _showRoutesNotAvailableError

    private val _showDirectionApiFailedError = SingleLiveEvent<String>()
    val showDirectionApiFailedError: LiveData<String>
        get() = _showDirectionApiFailedError


    init {
        webSocket.connect()
        Log.d(TAG, "init: connect")
    }

    fun getGeocodingDataByLatLng(lat: Double?, lng: Double?, apiKey: String, addresses: MutableList<Address>?) {
        coroutineScope.launch {
            if (lat != null && lng != null) {
                val approximateUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&location_type=APPROXIMATE&key=$apiKey"
                Log.d(TAG, "getGeocodingDataByLatLng: $approximateUrl")
                val approximate =
                    geocodingApi.getGeocodingData(approximateUrl).await()

                try {
                    if (approximate.isSuccessful) {
                        val area = async(Dispatchers.Default) {
                            val body = approximate.body()
                            if (body?.status == Status.OK.value) {
                                if (!body.results.isNullOrEmpty()) {
                                    if (!body.results[0].addressComponents.isNullOrEmpty()) {
                                        return@async body.results[0].addressComponents[0].shortName
                                    } else return@async null
                                } else return@async null
                            } else null
                        }.await()
                        if (area != null && !addresses.isNullOrEmpty()) {
                            var numeric = false
                            addresses.forEach {
                                try {
                                    if (it.subThoroughfare.matches("-?\\d+(\\.\\d+)?".toRegex())) {
                                        numeric = true
                                        _selectedLocation.value = "${area}, ${it.subThoroughfare}"
                                    }
                                } catch (ex: Exception) {
                                    if (!numeric) {
                                        _selectedLocation.value = null
                                    }
                                }
                            }
                        } else {
                            _selectedLocation.value = null
                        }
                    } else {
                        _selectedLocation.value = null
                    }
                } catch (ex: Exception) {
                    _selectedLocation.value = null
                    ex.printStackTrace()
                }
            }
        }
    }


    override fun onCleared() {
        webSocket.disconnect()
        super.onCleared()
    }

    override fun onConnect() {
        Log.d(TAG, "onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG, "onMessage data : $data")
        val jsonObject = JSONObject(data)
        when (jsonObject.getString(TYPE)) {
            NEAR_BY_CABS -> {
                handleOnMessageNearbyCabs(jsonObject)
            }

            CAB_BOOKED -> {
                _informCabBooked.call()
            }

            PICKUP_PATH, TRIP_PATH -> {
                val jsonArray = jsonObject.getJSONArray("path")
                val pickUpPath = arrayListOf<LatLng>()
                for (i in 0 until jsonArray.length()) {
                    val lat = (jsonArray.get(i) as JSONObject).getDouble("lat")
                    val lng = (jsonArray.get(i) as JSONObject).getDouble("lng")
                    val latLng = LatLng(lat, lng)
                    pickUpPath.add(latLng)
                }
                _showPath.value = pickUpPath
            }

            LOCATION -> {
                val latCurrent = jsonObject.getDouble("lat")
                val lngCurrent = jsonObject.getDouble("lng")
                _updateCabLocation.value = LatLng(latCurrent, lngCurrent)
            }

            CAB_IS_ARRIVING -> {
                _informCabIsArriving.call()
            }

            CAB_ARRIVED -> {
                _informCabArrived.call()
            }

            TRIP_START -> {
                _informTripStarted.call()
            }

            TRIP_END -> {
                _informTripEnded.call()
            }
        }
    }

    private fun handleOnMessageNearbyCabs(jsonObject: JSONObject) {
        val nearbyCabLocations = arrayListOf<LatLng>()
        val jsonArray = jsonObject.getJSONArray(LOCATIONS)
        for (i in 0 until jsonArray.length()) {
            val lat = (jsonArray.get(i) as JSONObject).getDouble(LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(LNG)
            val latLng = LatLng(lat, lng)
            nearbyCabLocations.add(latLng)
        }
        Log.d(TAG, "handleOnMessageNearbyCabs: $nearbyCabLocations")
        _showNearbyCabs.value = nearbyCabLocations
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisconnect")
    }

    override fun onError(error: String) {
        Log.d(TAG, "onError : $error")
        val jsonObject = JSONObject(error)
        when (jsonObject.getString(TYPE)) {
            ROUTES_NOT_AVAILABLE -> {
                _showRoutesNotAvailableError.call()
            }
            DIRECTION_API_FAILED -> {
                _showDirectionApiFailedError.value = "Direction API Failed : " + jsonObject.getString(ERROR)
            }
        }
    }

    fun requestNearbyCabs(latLng: LatLng) {
        val jsonObject = JSONObject()
        jsonObject.put(TYPE, NEAR_BY_CABS)
        jsonObject.put(LAT, latLng.latitude)
        jsonObject.put(LNG, latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    fun requestCab(pickUpLatLng: LatLng, dropLatLng: LatLng) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "requestCab")
        jsonObject.put("pickUpLat", pickUpLatLng.latitude)
        jsonObject.put("pickUpLng", pickUpLatLng.longitude)
        jsonObject.put("dropLat", dropLatLng.latitude)
        jsonObject.put("dropLng", dropLatLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }
}
