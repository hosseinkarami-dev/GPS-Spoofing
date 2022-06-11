package ir.elsadesign.fakegps.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
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
import com.mapbox.mapboxsdk.maps.Style
import ir.elsadesign.fakegps.CurrentLocation
import ir.elsadesign.fakegps.R
import ir.elsadesign.fakegps.databinding.ActivityDynamicPositionBinding
import ir.elsadesign.fakegps.helpers.HelperGPS
import ir.elsadesign.fakegps.models.LocPoint
import ir.elsadesign.fakegps.models.SharedPrefs
import ir.elsadesign.fakegps.security_model.RuntimePermissions
import ir.elsadesign.fakegps.service.DynamicPointService
import ir.elsadesign.fakegps.service.LocationService


class DynamicPositionActivity : RuntimePermissionsActivity() {
    private lateinit var originalLocOrigin: LocPoint
    private lateinit var binding: ActivityDynamicPositionBinding
    private var mapView: MapView? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            getString(R.string.mapbox_access_token)
        )
        binding = ActivityDynamicPositionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
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

        originalLocOrigin = SharedPrefs.getTripOrigin(this)
        binding.marker.visibility = if (LocationService.isStarted()) View.GONE else View.VISIBLE

        reset()
        checkLocation(false)

        binding.buttonToggleState.setOnClickListener {
            if (LocationService.isStarted()) {
                DynamicPointService.doStop(this, true)
                binding.marker.visibility = View.VISIBLE
                binding.buttonToggleState.visibility = View.GONE
                originPosition = null
            }
        }

        binding.marker.setOnClickListener {
            MaterialDialog(this)
                .title(R.string.settings)
                .message(R.string.radius_of_border)
                .show {
                    input(
                        waitForPositiveButton = true,
                        inputType = InputType.TYPE_CLASS_NUMBER
                    ) { _, text ->
                        radius = text.toString().toInt()
                        numberOfTripTimesDialog()
                    }
                    positiveButton(R.string.submit)
                    negativeButton(R.string.cancel)
                }
        }
    }

    @SuppressLint("CheckResult")
    private fun numberOfTripTimesDialog() {
        MaterialDialog(this)
            .title(R.string.settings)
            .message(R.string.number_of_trip_times_per_day)
            .show {
                input(
                    waitForPositiveButton = true,
                    inputType = InputType.TYPE_CLASS_NUMBER
                ) { _, text ->
                    numberOfTripTimes = text.toString().toInt()
                    speedDialog()
                }
                positiveButton(R.string.submit)
                negativeButton(R.string.cancel)
            }
    }

    @SuppressLint("CheckResult")
    private fun speedDialog() {
        MaterialDialog(this)
            .title(R.string.movement_speed)
            .message(R.string.movement_speed_message)
            .show {
                input(
                    waitForPositiveButton = true,
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                ) { _, text ->
                    speed = text.toString().toFloat()
                    doStart()
                }
                positiveButton(R.string.submit)
                negativeButton(R.string.cancel)
            }
    }

    private var radius = 2000
    private var numberOfTripTimes = 3
    private var speed = 4.9f

    private fun doStart() {
        binding.marker.visibility = View.GONE
        binding.buttonToggleState.visibility = View.VISIBLE

        val latitude = mapBoxMap?.cameraPosition?.target?.latitude!!
        val longitude = mapBoxMap?.cameraPosition?.target?.longitude!!
        originPosition = LatLng(
            Location("").apply {
                setLatitude(latitude)
                setLongitude(longitude)
            }
        )
        val intent = Intent(this, DynamicPointService::class.java).apply {
            putExtra("radius", radius)
            putExtra("number_of_trips", numberOfTripTimes)
            putExtra("speed", speed)
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            action = DynamicPointService.ACTION_START
        }
        startService(intent)

        if(HelperGPS.isEnabled(this))
            Toast.makeText(this, getString(R.string.turn_off_gps), Toast.LENGTH_LONG).show()
    }

    override fun onPermissionsGranted() {
        Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_LONG).show()
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

    private fun reset() {
        originPosition = null

        if (LocationService.isStarted()) {
            binding.buttonToggleState.visibility = View.VISIBLE
        }
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
            if (style.isFullyLoaded) checkLocation(true) else Toast.makeText(
                baseContext,
                "لطفا تا بارگذاری کامل نقشه صبر کنید.",
                Toast.LENGTH_LONG
            ).show()
        }

        enableLocationComponent(style)

        mapBoxMap.addOnMoveListener(object : MapboxMap.OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
            }

            override fun onMove(detector: MoveGestureDetector) {
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
            }
        })
        mapBoxMap.uiSettings.isLogoEnabled = false
        mapBoxMap.uiSettings.isAttributionEnabled = false
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
                    status.startResolutionForResult(
                        this,
                        FixedPositionActivity.REQUEST_CHECK_SETTINGS
                    )
                } catch (ignored: IntentSender.SendIntentException) {
                }
            }
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
        if (requestCode == FixedPositionActivity.LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) getLocation()
        }
        RuntimePermissions.onRequestPermissionsResult(
            this,
            requestCode,
            permissions,
            grantResults
        )
    }

    private fun checkLocation(moveToCamera: Boolean) {
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
                FixedPositionActivity.LOCATION_PERMISSION
            )
            return
        }

        onLocationFound = object : OnLocationFound {
            override fun onFound(location: Location) {
                if (moveToCamera)
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

        getLocation()
    }

    interface OnLocationFound {
        fun onFound(location: Location)
    }

    private var onLocationFound: OnLocationFound? = null

    private val locationResult: CurrentLocation.LocationResult =
        object : CurrentLocation.LocationResult() {
            override fun gotLocation(location: Location) {
                runOnUiThread {
                    onLocationFound?.onFound(location)
                }
            }
        }

    override fun onPermissionsDenied(permissions: Array<String?>?) {
        val text = "دسترسی های زیر به اپلیکیشن داده نشده است:\n  " + TextUtils.join(
            "\n  ",
            permissions!!
        )
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private var mapBoxMap: MapboxMap? = null
    private var originPosition: LatLng? = null
}