package ir.elsadesign.fakegps.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMoveListener
import com.mapbox.mapboxsdk.maps.Style
import ir.elsadesign.fakegps.CurrentLocation
import ir.elsadesign.fakegps.R
import ir.elsadesign.fakegps.models.LocPoint
import ir.elsadesign.fakegps.models.SharedPrefs
import ir.elsadesign.fakegps.databinding.ActivityFixedPositionBinding
import ir.elsadesign.fakegps.security_model.RuntimePermissions
import ir.elsadesign.fakegps.service.LocationService


class FixedPositionActivity: RuntimePermissionsActivity() {
    private var mapView: MapView? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private lateinit var binding: ActivityFixedPositionBinding

    companion object {
        const val REQUEST_CHECK_SETTINGS = 0x1
        const val LOCATION_PERMISSION = 146
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            getString(R.string.mapbox_access_token)
        )
        binding = ActivityFixedPositionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        //  mapView.onCreate(savedInstanceState);
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        //   supportActionBar.setHomeAsUpIndicator(R.drawable.ic_back_gray)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setHomeButtonEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        reset()
        checkLocation()

        binding.buttonToggleState.setOnClickListener {
            try {
                if (LocationService.isStarted()) {
                    LocationService.doStop(this@FixedPositionActivity, true)
                    binding.buttonToggleStateText.setText(R.string.label_button_start)
                    binding.marker.visibility = View.VISIBLE
                } else {
                    doStart()
                    binding.marker.visibility = View.GONE
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun reset() {
        if (LocationService.isStarted()) binding.buttonToggleStateText.setText(R.string.label_button_stop)
        binding.marker.visibility = if(LocationService.isStarted()) View.GONE else View.VISIBLE
    }


    private fun doStart() {
        val latitude = mapBoxMap?.cameraPosition?.target?.latitude!!
        val longitude = mapBoxMap?.cameraPosition?.target?.longitude!!
        val modifiedLoc = LocPoint("$latitude,$longitude")
        LocationService.doStart(this@FixedPositionActivity, true, modifiedLoc, null, 0)
        SharedPrefs.putTripOrigin(this@FixedPositionActivity, modifiedLoc)
        binding.buttonToggleStateText.setText(R.string.label_button_stop)
    }

    override fun onPermissionsGranted() {
        doStart()
    }

    override fun onPermissionsDenied(permissions: Array<String?>?) {
        val text = "دسترسی های زیر به اپلیکیشن داده نشده است:\n  " + TextUtils.join(
            "\n  ",
            permissions!!
        )
        Toast.makeText(this@FixedPositionActivity, text, Toast.LENGTH_LONG).show()
    }

    override fun onPause() {
        super.onPause()
        if (mapView != null) mapView!!.onPause()
        if (LocationService.isStarted() && !isFinishing) finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapView != null) mapView!!.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (mapView != null) mapView!!.onStart()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (mapView != null) mapView!!.onLowMemory()
    }

    override fun onStop() {
        super.onStop()
        if (mapView != null) mapView!!.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (mapView != null) mapView!!.onResume()
    }

    private var mapInit = false

    private fun initMap() {
        mapInit = true
        mapView!!.setBackgroundColor(Color.TRANSPARENT)
        mapView!!.getMapAsync { mapboxMap: MapboxMap ->
            mapboxMap.setStyle(
                Style.MAPBOX_STREETS
            ) { style: Style? ->
                this.mapBoxMap = mapboxMap
                if (style != null) {
                    getMapAsync(mapBoxMap!!, style)
                }
            }
        }
    }

    private fun getMapAsync(mapBoxMap: MapboxMap, style: Style) {
        binding.fab.setOnClickListener {
            if (style.isFullyLoaded) checkLocation() else Toast.makeText(
                baseContext,
                "لطفا تا بارگذاری کامل نقشه صبر کنید.",
                Toast.LENGTH_LONG
            ).show()
        }
        enableLocationComponent(style)
        val marker = binding.marker
        mapBoxMap.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                marker.animate().scaleX(.8f).scaleY(.8f).setDuration(500).start()
            }

            override fun onMove(detector: MoveGestureDetector) {
                marker.scaleX = .8f
                marker.scaleY = .8f
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
                marker.animate().scaleX(1f).scaleY(1f).setDuration(500).start()
            }
        })
        mapBoxMap.uiSettings.isLogoEnabled = false
        mapBoxMap.uiSettings.isAttributionEnabled = false
        if (position != null) mapBoxMap.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(position!!.latitude, position!!.longitude))
                    .tilt(0.0)
                    .zoom(16.0)
                    .build()
            )
        )
    }

    private var isUserLocationAnimated = false

    @SuppressLint("MissingPermission")
    private fun animateUserLocation(style: Style) {
        if (isUserLocationAnimated) return
        isUserLocationAnimated = true
        val locationComponentOptions = LocationComponentOptions.builder(this).build()
        val locationComponentActivationOptions = LocationComponentActivationOptions
            .builder(this, style)
            .locationComponentOptions(locationComponentOptions)
            .build()
        val locationComponent = mapBoxMap!!.locationComponent

        // Activate with a built LocationComponentActivationOptions object
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(
                this,
                style
            ).build()
        )

        // Enable to make component visible
        locationComponent.isLocationComponentEnabled = true

        // Set the component's camera mode
        locationComponent.cameraMode = CameraMode.TRACKING

        // Set the component's render mode
        locationComponent.renderMode = RenderMode.COMPASS
        locationComponent.isLocationComponentEnabled = true
        locationComponent.activateLocationComponent(locationComponentActivationOptions!!)
    }


    private fun enableGPS() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    override fun onConnected(bundle: Bundle?) {}
                    override fun onConnectionSuspended(i: Int) {
                        mGoogleApiClient?.connect()
                    }
                })
                .addOnConnectionFailedListener { connectionResult: ConnectionResult ->
                    Log.d(
                        "Location error",
                        "Location error " + connectionResult.errorCode
                    )
                }.build()
            mGoogleApiClient?.connect()
        }
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (30 * 1000).toLong()
        locationRequest.fastestInterval = (5 * 1000).toLong()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient!!, builder.build())
        result.setResultCallback { result1: LocationSettingsResult ->
            val status = result1.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS, LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                }
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (ignored: SendIntentException) {
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            val locationComponent: LocationComponent = mapBoxMap!!.locationComponent

            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle).build()
            )

            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        }
    }

    private fun getLocation() {
        if (mapBoxMap != null) animateUserLocation(mapBoxMap!!.style!!)
        val currentLocation = CurrentLocation(this)
        if (!currentLocation.getLocation(this, locationResult)) {
            enableGPS()
        }
        if (!mapInit) initMap()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) getLocation()
        }
        RuntimePermissions.onRequestPermissionsResult(
            this@FixedPositionActivity,
            requestCode,
            permissions,
            grantResults
        )
    }

    private fun checkLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                baseContext,
                "لطفا دسترسی مکان خود را به نرم افزار بدهید.",
                Toast.LENGTH_LONG
            ).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION
            )
            return
        }
        getLocation()
    }

    private val locationResult: CurrentLocation.LocationResult = object : CurrentLocation.LocationResult() {
        override fun gotLocation(location: Location) {
            runOnUiThread {
                mapBoxMap?.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .tilt(0.0)
                            .zoom(16.0)
                            .build()
                    )
                )
            }
        }
    }

    private var mapBoxMap: MapboxMap? = null
    private var position: LatLng? = null
}