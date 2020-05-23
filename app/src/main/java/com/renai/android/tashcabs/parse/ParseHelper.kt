package com.renai.android.tashcabs.parse

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.parse.Parse
import com.parse.ParseACL
import com.parse.ParseException
import com.parse.ParseUser
import com.renai.android.tashcabs.R

val currentUser: ParseUser?
        by lazy {
            try {
                ParseUser.getCurrentUser()
            } catch (ex: NullPointerException) {
                throw ex
            }
        }

fun parseInitialize(context: Context) {
    Parse.initialize(
        Parse.Configuration.Builder(context)
            .applicationId("84871e760de00b659f6dd3e49d17386aacd20826")
            .clientKey("9ae40e9156033db22ee0467bac46ddc6081208d4")
            .server("http://18.188.13.192:80/parse/")
            .build()
    )

    val defaultACL = ParseACL()
    defaultACL.publicReadAccess = true
    defaultACL.publicWriteAccess = true
    ParseACL.setDefaultACL(defaultACL, true)
}

fun Fragment.handleParseError(ex: ParseException) {
    when (ex.code) {
        ParseException.INVALID_SESSION_TOKEN -> handleInvalidSessionToken(this)
        else -> Toast.makeText(context, ex.localizedMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun handleInvalidSessionToken(fragment: Fragment) {
    val options = NavOptions.Builder().setPopUpTo(R.id.login_dest, false).build()
    fragment.findNavController().navigate(R.id.login_dest, null, options)
}

