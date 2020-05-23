package com.renai.android.tashcabs

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.libraries.places.api.Places
import com.google.maps.GeoApiContext
import com.mindorks.ridesharing.simulator.Simulator
import com.parse.Parse
import com.renai.android.tashcabs.parse.parseInitialize
import com.renai.android.tashcabs.utils.CHANNEL_ID
import com.renai.android.tashcabs.utils.CHANNEL_NAME

private const val TAG = "CommonLogs"


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE)

        try {
            parseInitialize(this)
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
            Simulator.geoApiContext = GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .build()
        } catch (ex: Exception) {
            return //could be network error. just return here cuz we have network error handler in mainactivity
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationsChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = this.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(notificationsChannel)
        }
    }
}