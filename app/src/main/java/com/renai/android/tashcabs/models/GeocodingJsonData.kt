package com.renai.android.tashcabs.models

import com.google.gson.annotations.SerializedName

/*
Copyright (c) 2020 Kotlin Data Classes Generated from JSON powered by http://www.json2kotlin.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

For support, please feel free to contact me at https://www.linkedin.com/in/syedabsar */


data class GeocodingJsonData(
    @SerializedName("plus_code") val plusCode: PlusCode,
    @SerializedName("results") val results: List<Results>,
    @SerializedName("status") val status: String
)

enum class Status(val value: String) {
    OK("OK"), //indicates that no errors occurred; the address was successfully parsed and at least one geocode was returned.
    ZERO_RESULTS("ZERO_RESULTS"), //indicates that the geocode was successful but returned no results. This may occur if the geocoder was passed a non-existent address.

    //indicates any of the following:
    //The API key is missing or invalid.
    //Billing has not been enabled on your account.
    //A self-imposed usage cap has been exceeded.
    //The provided method of payment is no longer valid (for example, a credit card has expired).
    //See the Maps FAQ to learn how to fix this.
    OVER_DAILY_LIMIT("OVER_DAILY_LIMIT"),
    OVER_QUERY_LIMIT("OVER_QUERY_LIMIT"), //indicates that you are over your quota.
    REQUEST_DENIED("REQUEST_DENIED"), //indicates that your request was denied.
    INVALID_REQUEST("INVALID_REQUEST"), //generally indicates that the query (address, components or latlng) is missing.
    UNKNOWN_ERROR("INVALID_REQUEST"), //indicates that the request could not be processed due to a server error. The request may succeed if you try again.
}