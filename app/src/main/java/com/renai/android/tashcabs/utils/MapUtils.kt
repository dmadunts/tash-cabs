package com.renai.android.tashcabs.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.renai.android.tashcabs.R
import kotlin.math.abs
import kotlin.math.atan

private const val TAG = "MapUtils"

//moves camera to given location
fun GoogleMap.moveCameraToLocation(latLng: LatLng, zoom: Float? = null, withAnimation: Boolean? = null) {
    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(latLng.latitude, latLng.longitude), zoom ?: 16.0F)
    if (withAnimation != null && withAnimation) {
        animateCamera(cameraUpdate)
    } else {
        moveCamera(cameraUpdate)
    }
}

fun getCarBitmap(context: Context): Bitmap {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
    return Bitmap.createScaledBitmap(bitmap, 75, 100, false)
}

fun getDestinationBitmap(): Bitmap {
    val height = 20
    val width = 20
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    paint.color = Color.BLACK
    paint.style = Paint.Style.FILL
    paint.isAntiAlias = true
    canvas.drawRect(0F, 0F, width.toFloat(), height.toFloat(), paint)
    return bitmap
}

fun getRotation(start: LatLng, end: LatLng): Float {
    val latDifference: Double = abs(start.latitude - end.latitude)
    val lngDifference: Double = abs(start.longitude - end.longitude)
    var rotation = -1F
    when {
        start.latitude < end.latitude && start.longitude < end.longitude -> {
            rotation = Math.toDegrees(atan(lngDifference / latDifference)).toFloat()
        }
        start.latitude >= end.latitude && start.longitude < end.longitude -> {
            rotation = (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 90).toFloat()
        }
        start.latitude >= end.latitude && start.longitude >= end.longitude -> {
            rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 180).toFloat()
        }
        start.latitude < end.latitude && start.longitude >= end.longitude -> {
            rotation =
                (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 270).toFloat()
        }
    }
    Log.d(TAG, "getRotation: $rotation")
    return rotation
}