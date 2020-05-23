package com.renai.android.tashcabs.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {

    fun Fragment.openApplicationSettings() {
        val openAppSettingsIntent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${requireActivity().packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityForResult(openAppSettingsIntent, PERMISSION_LOCATION_REQUEST_CODE)
    }

    val Fragment.hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            requireContext(),
            ACCESS_FINE_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED


    fun Fragment.requestLocationPermissions() {
        this.requestPermissions(arrayOf(ACCESS_FINE_PERMISSION), PERMISSION_LOCATION_REQUEST_CODE)
    }

    //TODO: implement later
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //TODO: implement later
    fun showGPSNotEnabledDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Enable GPS")
            .setMessage("Required for this app")
            .setCancelable(false)
            .setPositiveButton("Enable now") { _, _ ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .show()
    }
}
