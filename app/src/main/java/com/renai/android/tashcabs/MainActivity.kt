package com.renai.android.tashcabs

import android.content.Context
import android.net.*
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.google.android.material.navigation.NavigationView
import com.renai.android.tashcabs.parse.currentUser
import com.renai.android.tashcabs.utils.gone
import com.renai.android.tashcabs.utils.visible
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.request_view.*
import kotlinx.android.synthetic.main.toolbar.*


@Suppress("DEPRECATION") class MainActivity : AppCompatActivity() {
    private val TAG = "CommonLogs"
    private val cm by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val imm by lazy {
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val hasConnection by lazy { hasConnection() }
    private lateinit var toolbar: Toolbar

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    private val navController by lazy { findNavController(R.id.nav_host) }
    private val appBarConfiguration by lazy { AppBarConfiguration(setOf(R.id.rider_dest), drawer_layout) }
    private lateinit var currentDest: NavDestination

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)

        if (!hasConnection) {
            navController.navigate(R.id.no_connection_dest)
        }

        if (hasConnection && currentUser == null) {
            navController.navigate(R.id.login_dest)
        }

        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.getHeaderView(0).setOnClickListener {
            navController.navigate(R.id.account_dest)
        }

        setupViews()

        setupConnectionListener()

        setupDestChangeListener()
    }

    private fun setupConnectionListener() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                if (currentDest.id == R.id.no_connection_dest) {
                    runOnUiThread {
                        onSupportNavigateUp()
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                runOnUiThread {
                    findNavController(R.id.nav_host).navigate(R.id.no_connection_dest)
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                runOnUiThread {
                    findNavController(R.id.nav_host).navigate(R.id.no_connection_dest)
                }
            }
        }
    }

    private fun hasConnection(): Boolean {
        val isConnected: Boolean?

        isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            activeNetwork?.isConnectedOrConnecting == true
        }

        return !(isConnected == null || !isConnected)
    }

    private fun setupDestChangeListener() {
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            currentDest = destination
            coordinateViews(destination)
        }
    }

    private fun setupViews() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupActionBar(navController, appBarConfiguration)
        setupNavigationMenu(navController)
    }

    private fun setupNavigationMenu(navController: NavController) {
        val sideNavView = findViewById<NavigationView>(R.id.nav_view)
        sideNavView?.setupWithNavController(navController)
        sideNavView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.order_history_dest -> navController.navigate(R.id.placeholder_dest)
                R.id.my_addresses_dest -> navController.navigate(R.id.placeholder_dest)
                R.id.payment_dest -> navController.navigate(R.id.placeholder_dest)
                R.id.settings_dest -> navController.navigate(R.id.placeholder_dest)
                else -> {
                    NavigationUI.onNavDestinationSelected(it, navController)
                }
            }
            drawer_layout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupActionBar(navController: NavController, appBarConfiguration: AppBarConfiguration) {
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration)


    override fun onBackPressed() {
        when (currentDest.id) {
            R.id.no_connection_dest -> finish()
            R.id.login_dest -> finish()
            else -> super.onBackPressed()
        }
    }

    private fun coordinateViews(destination: NavDestination) {
        window.statusBarColor = this.resources.getColor(R.color.colorPrimaryDark)
        toolbar.visible()
        selected_location_host.gone()
        toolbar.title = null
        hideKeyboard()
        enableNavDrawer()
        request_view.gone()
        when (destination.id) {
            R.id.login_dest -> {
                toolbar.gone()
                disableNavDrawer()
            }
            R.id.no_connection_dest -> {
                window.statusBarColor = this.resources.getColor(R.color.colorAccent)
                disableNavDrawer()
            }
            R.id.placeholder_dest -> {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
            R.id.account_dest -> {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
            R.id.rider_dest -> {
                selected_location_host.visible()
            }
        }
    }

    private fun enableNavDrawer() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun disableNavDrawer() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onStart() {
        super.onStart()
        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onStop() {
        super.onStop()
        cm.unregisterNetworkCallback(networkCallback)
    }

    private fun hideKeyboard() {
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}




