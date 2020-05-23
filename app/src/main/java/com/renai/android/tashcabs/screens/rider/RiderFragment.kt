package com.renai.android.tashcabs.screens.rider

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mindorks.ridesharing.utils.AnimationUtils
import com.renai.android.tashcabs.R
import com.renai.android.tashcabs.databinding.FragmentRiderBinding
import com.renai.android.tashcabs.network.NetworkService
import com.renai.android.tashcabs.parse.handleParseError
import com.renai.android.tashcabs.utils.*
import com.renai.android.tashcabs.utils.PermissionUtils.hasPermission
import com.renai.android.tashcabs.utils.PermissionUtils.openApplicationSettings
import com.renai.android.tashcabs.utils.PermissionUtils.requestLocationPermissions
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class RiderFragment : Fragment(), OnMapReadyCallback {
    private val TAG = "CommonLogs"

    //views
    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var toolbar: Toolbar

    //map
    private lateinit var map: GoogleMap
    private val fusedLocationClient by lazy { getFusedLocationProviderClient() }
    private val locationRequest by lazy { setupLocationRequest() }
    private val locationCallback by lazy { setupLocationCallback() }
    private var currentLatLng: LatLng? = null
    private var pickUpLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null
    private val nearbyCabMarkerList = arrayListOf<Marker>()
    private var destinationMarker: Marker? = null
    private var originMarker: Marker? = null
    private var grayPolyLine: Polyline? = null
    private var blackPolyline: Polyline? = null
    private var previousLatLngFromServer: LatLng? = null
    private var currentLatLngFromServer: LatLng? = null
    private var movingCabMarker: Marker? = null
    private var isOnTrip = false

    //coroutines
    private val job = Job()
    private val scope = CoroutineScope(job + Main)

    private val viewModel: RiderViewModel by lazy {
        ViewModelProvider(this, RiderViewModelFactory(NetworkService())).get(RiderViewModel::class.java)
    }

    private lateinit var binding: FragmentRiderBinding
    private lateinit var requestView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRiderBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.rider_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupViews()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.onParseException.observe(viewLifecycleOwner, Observer {
            it?.let {
                handleParseError(it)
            }
        })

        viewModel.showNearbyCabs.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                showNearbyCabs(it)
            }
        })

        viewModel.selectedLocation.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                showPreciseLocationUI(it)
            } else {
                showApproximateLocationUI()
            }
        })

        viewModel.informCabBooked.observe(viewLifecycleOwner, Observer {
            informCabBooked()
        })


        viewModel.informCabIsArriving.observe(viewLifecycleOwner, Observer {
            binding.bottomSheet.statusText.text = getString(R.string.your_cab_is_arriving)
        })

        viewModel.informCabArrived.observe(viewLifecycleOwner, Observer {
            infromCabArrived()
        })

        viewModel.informTripStarted.observe(viewLifecycleOwner, Observer {
            informTripStarted()
        })

        viewModel.informTripEnded.observe(viewLifecycleOwner, Observer {
            informTripEnded()
        })

        viewModel.updateCabLocation.observe(viewLifecycleOwner, Observer { latLng ->
            updateCabLocation(latLng)
        })

        viewModel.showDirectionApiFailedError.observe(viewLifecycleOwner, Observer {
            showToast(it)
        })

        viewModel.showRoutesNotAvailableError.observe(viewLifecycleOwner, Observer {
            showToast(getString(R.string.route_not_available))
        })

        viewModel.showPath.observe(viewLifecycleOwner, Observer { latLngList ->
            showPath(latLngList)
        })
    }

    private fun informCabBooked() {
        nearbyCabMarkerList.forEach { it.remove() }
        nearbyCabMarkerList.clear()
        binding.bottomSheet.requestBtn.gone()
        binding.bottomSheet.statusText.visible()
        binding.bottomSheet.statusText.text = getString(R.string.your_cab_is_booked)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun infromCabArrived() {
        binding.bottomSheet.statusText.text = getString(R.string.your_cab_has_arrived)
        grayPolyLine?.remove()
        blackPolyline?.remove()
        originMarker?.remove()
        destinationMarker?.remove()
    }

    private fun informTripStarted() {
        binding.bottomSheet.statusText.text = getString(R.string.your_are_on_trip)
        previousLatLngFromServer = null
    }

    private fun informTripEnded() {
        binding.bottomSheet.statusText.text = getString(R.string.trip_ended)
        binding.bottomSheet.requestBtn.text = getString(R.string.take_a_next_ride)
        isOnTrip = true
        binding.bottomSheet.requestBtn.visible()
        grayPolyLine?.remove()
        blackPolyline?.remove()
        originMarker?.remove()
        destinationMarker?.remove()
    }

    private fun updateCabLocation(latLng: LatLng) {
        scope.launch {
            if (movingCabMarker == null) {
                movingCabMarker = addCarMarkerAndGet(latLng)
            }
            if (previousLatLngFromServer == null) {
                currentLatLngFromServer = latLng
                previousLatLngFromServer = currentLatLngFromServer
                movingCabMarker?.position = currentLatLngFromServer
                movingCabMarker?.setAnchor(0.5f, 0.5f)
//                map.moveCameraToLocation(currentLatLngFromServer!!, null, true)
            } else {
                previousLatLngFromServer = currentLatLngFromServer
                currentLatLngFromServer = latLng
                val valueAnimator = AnimationUtils.cabAnimator()
                valueAnimator.addUpdateListener { va ->
                    if (currentLatLngFromServer != null && previousLatLngFromServer != null) {
                        val multiplier = va.animatedFraction
                        val nextLocation = LatLng(
                            multiplier * currentLatLngFromServer!!.latitude + (1 - multiplier) * previousLatLngFromServer!!.latitude,
                            multiplier * currentLatLngFromServer!!.longitude + (1 - multiplier) * previousLatLngFromServer!!.longitude
                        )
                        movingCabMarker?.position = nextLocation
                        movingCabMarker?.setAnchor(0.5f, 0.5f)
                        val rotation = getRotation(previousLatLngFromServer!!, nextLocation)
                        if (!rotation.isNaN()) {
                            movingCabMarker?.rotation = rotation
                        }
//                        map.moveCameraToLocation(nextLocation, null, true)
                    }
                }
                valueAnimator.start()
            }
        }
    }

    private fun showPath(latLngList: ArrayList<LatLng>) {
        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))
        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(5f)
        polylineOptions.addAll(latLngList)
        grayPolyLine = map.addPolyline(polylineOptions)

        val blackPolylineOptions = PolylineOptions()
        blackPolylineOptions.width(5f)
        blackPolylineOptions.color(Color.BLACK)
        blackPolyline = map.addPolyline(blackPolylineOptions)

        originMarker = addOriginDestinationMarkerAndGet(latLngList[0])
        originMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker = addOriginDestinationMarkerAndGet(latLngList[latLngList.size - 1])
        destinationMarker?.setAnchor(0.5f, 0.5f)

        val polylineAnimator = AnimationUtils.polyLineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (grayPolyLine?.points!!.size * (percentValue / 100.0f)).toInt()
            blackPolyline?.points = grayPolyLine?.points!!.subList(0, index)
        }
        polylineAnimator.start()
    }

    private fun setupViews() {
        requestView = requireActivity().findViewById(R.id.request_view)
        bottomSheet = requireActivity().findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        toolbar = requireActivity().findViewById(R.id.toolbar)

        requestView.setOnClickListener {
            openApplicationSettings()
        }

        binding.currentLocationBtn.setOnClickListener {
            moveCameraToCurrentLocation()
        }

        binding.bottomSheet.requestBtn.setOnClickListener {
            if (isOnTrip) {
                reset()
            } else {
                startTrip()
            }
        }

        binding.bottomSheet.pickUpText.setOnClickListener {
            if (!isOnTrip) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                launchLocationAutoCompleteActivity(PICKUP_REQUEST_CODE)
            }
        }

        binding.bottomSheet.dropText.setOnClickListener {
            if (!isOnTrip) {
                launchLocationAutoCompleteActivity(DROP_REQUEST_CODE)
            }
        }

        coordinateBtnAndInputs(binding.bottomSheet.requestBtn, binding.bottomSheet.pickUpText, binding.bottomSheet.dropText)
    }

    private fun startTrip() {
        isOnTrip = true
        viewModel.requestCab(pickUpLatLng!!, dropLatLng!!)
        binding.pin.gone()
        toolbar.selected_location_host.gone()
    }

    private fun reset() {
        isOnTrip = false
        binding.pin.visible()
        toolbar.selected_location_host.visible()
        binding.bottomSheet.statusText.gone()
        binding.bottomSheet.requestBtn.text = getString(R.string.request_cab)
        nearbyCabMarkerList.forEach { it.remove() }
        nearbyCabMarkerList.clear()
        previousLatLngFromServer = null
        currentLatLngFromServer = null
        if (currentLatLng != null) {
            map.moveCameraToLocation(currentLatLng!!, null, true)
            viewModel.requestNearbyCabs(currentLatLng!!)
        } else {
            binding.bottomSheet.pickUpText.text = getString(R.string.selected_location)
        }
        binding.bottomSheet.dropText.text = ""
        movingCabMarker?.remove()
        grayPolyLine?.remove()
        blackPolyline?.remove()
        originMarker?.remove()
        destinationMarker?.remove()
        dropLatLng = null
        grayPolyLine = null
        blackPolyline = null
        originMarker = null
        destinationMarker = null
        movingCabMarker = null
    }

    private fun launchLocationAutoCompleteActivity(requestCode: Int) {
        val fields: List<Place.Field> =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(requireContext())
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICKUP_REQUEST_CODE || requestCode == DROP_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)

                    when (requestCode) {
                        PICKUP_REQUEST_CODE -> {
                            binding.bottomSheet.pickUpText.text = place.name
                            pickUpLatLng = place.latLng.apply {
                                this?.let {
                                    map.moveCameraToLocation(it)
                                }
                            }
                        }

                        DROP_REQUEST_CODE -> {
                            binding.bottomSheet.dropText.text = place.name
                            dropLatLng = place.latLng
                        }
                    }
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status: Status = Autocomplete.getStatusFromIntent(data!!)
                    Log.d(TAG, status.statusMessage!!)
                }

                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Place Selection Canceled")
                }
            }
        }
    }

    private fun showApproximateLocationUI() {
        binding.bottomSheet.pickUpText.text = getString(R.string.selected_location)
        toolbar.selected_location_text.gone()
        toolbar.header.gone()
        toolbar.icon_host.visible()
    }

    private fun showPreciseLocationUI(location: String) {
        binding.bottomSheet.pickUpText.text = location
        toolbar.icon_host.gone()
        toolbar.header.visible()
        toolbar.selected_location_text.visible()
        toolbar.header.text = getString(R.string.selected_location)
        toolbar.selected_location_text.text = location
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestView.gone()
                } else {
                    requestView.visible()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        configureMap()

        if (hasPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                it?.let {
                    map.moveCameraToLocation(LatLng(it.latitude, it.longitude))
                }
            }
            binding.pin.visible()
            requestLocationUpdates()
        } else {
            requestLocationPermissions()
        }

        map.setOnCameraIdleListener {
            if (!isOnTrip) {
                scope.launch {
                    try {
                        val latLng = map.cameraPosition.target
                        pickUpLatLng = latLng
                        val addresses = getAddresses()
                        viewModel.getGeocodingDataByLatLng(
                            latLng.latitude,
                            latLng.longitude, resources.getString(R.string.geocoding_key), addresses
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        map.setOnCameraMoveListener {
            if (!isOnTrip) {
                scope.launch {
                    showSelectingToolbar()
                }
            }
        }
    }

    private fun showSelectingToolbar() {
        toolbar.icon_host.gone()
        toolbar.header.visible()
        toolbar.selected_location_text.visible()
        toolbar.header.text = getString(R.string.selected_location)
        toolbar.selected_location_text.text = getString(R.string.selecting)
    }

    private suspend fun getAddresses(): MutableList<Address>? =
        withContext(IO) {
            var latLng: LatLng? = null
            withContext(Main) {
                latLng = map.cameraPosition.target
            }
            val geocoder = Geocoder(requireContext(), Locale.ENGLISH)
            geocoder.getFromLocation(latLng!!.latitude, latLng!!.longitude, 10)
        }

    private fun configureMap() {
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isRotateGesturesEnabled = false
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.setOnMyLocationClickListener {
            moveCameraToCurrentLocation()
        }
    }

    private fun getFusedLocationProviderClient() =
        LocationServices.getFusedLocationProviderClient(requireContext())

    private fun setupLocationRequest(): LocationRequest =
        LocationRequest().setInterval(10_000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    private fun setupLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (currentLatLng == null) {
                    locationResult.lastLocation?.let {
                        map.moveCameraToLocation(LatLng(it.latitude, it.longitude))
                        currentLatLng = LatLng(it.latitude, it.longitude)
                        viewModel.requestNearbyCabs(LatLng(it.latitude, it.longitude))
                    }
                }
            }
        }
    }

    private fun showNearbyCabs(latLngList: List<LatLng>) {
        nearbyCabMarkerList.clear()
        for (latLng in latLngList) {
            val nearbyCabMarker = addCarMarkerAndGet(latLng)
            nearbyCabMarkerList.add(nearbyCabMarker)
        }
    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(getCarBitmap(requireContext()))
        return map.addMarker(MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor))
    }

    private fun addOriginDestinationMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(getDestinationBitmap())
        return map.addMarker(MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor))
    }

    private fun requestLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission) {
            requestView.gone()
            requestLocationUpdates()
        }
    }

    private fun moveCameraToCurrentLocation() {
        if (this::map.isInitialized) {
            currentLatLng?.let { latLng ->
                map.moveCameraToLocation(latLng, null, true)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
