package com.renai.android.tashcabs.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.renai.android.tashcabs.models.GeocodingJsonData
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

private const val BASE_URL = "https://maps.googleapis.com/maps/api/geocode/"

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .addConverterFactory(GsonConverterFactory.create())
    .build()

interface GeocodingApi {
    @GET
    fun getGeocodingData(@Url url: String): Deferred<Response<GeocodingJsonData>>
}

object RetrofitService {
    val geocodingApi: GeocodingApi by lazy { retrofit.create(GeocodingApi::class.java) }
}