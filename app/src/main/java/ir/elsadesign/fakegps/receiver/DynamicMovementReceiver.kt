package ir.elsadesign.fakegps.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import ir.elsadesign.fakegps.models.LocPoint
import ir.elsadesign.fakegps.models.SharedPrefs
import ir.elsadesign.fakegps.service.LocationService
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DynamicMovementReceiver : BroadcastReceiver() {
    private lateinit var originPosition: LocPoint
    private lateinit var destinationPosition: LocPoint
    private var radius = 2000
    private var speed = 4.9f
    private var originalTripDuration = 100
    override fun onReceive(context: Context, intent: Intent) {

        Log.d("DynamicPosition", "Movement Receiver Called.")

        if (!LocationService.isStarted())
            return

        Log.d("DynamicPosition", "Location Service Working perfectly.")

        if (!LocationService.tripIsEnded()) {
            Log.d("DynamicPosition", "Movement Receiver Stopped due to previous working thread.")
            return
        }

        Log.d("DynamicPosition", "Movement Receiver Started.")

        originPosition =
            LocPoint(SharedPrefs.getTripOriginLat(context), SharedPrefs.getTripOriginLon(context))

        val origin = Location("").apply {
            latitude = originPosition.latitude
            longitude = originPosition.longitude
        }

        radius = intent.getIntExtra("radius", 2000)
        speed =
            (intent.getFloatExtra("speed", 4.9f) / 3.6).toFloat() //default is walking speed in m/s.

        val mainOrigin = Location("").apply {
            latitude = intent.getDoubleExtra("latitude", 0.0)
            longitude = intent.getDoubleExtra("longitude", 0.0)
        }

        val destination = getLocationInLatLngRad(radius.toDouble(), mainOrigin)

        val distance = origin.distanceTo(destination)

        originalTripDuration = (distance / speed).toInt()
        destinationPosition = LocPoint("${destination.latitude},${destination.longitude}")

        Log.d(
            "DynamicPosition",
            "Origin Position: ${originPosition.latitude},${originPosition.longitude}"
        )

        Log.d(
            "DynamicPosition",
            "New destination selected: ${destination.latitude},${destination.longitude}"
        )

        Log.d("DynamicPosition", "Distance: $distance meter")

        Log.d("DynamicPosition", "Time Needed to Reach Destination: $originalTripDuration")

        doStart(context)
        SharedPrefs.putTripOrigin(context, destinationPosition)
    }

    private fun doStart(context: Context) {
        val modifiedLocOrigin = LocPoint(originPosition.latitude, originPosition.longitude)
        val modifiedLocDestination =
            LocPoint(destinationPosition.latitude, destinationPosition.longitude)
        val modifiedTripDuration = originalTripDuration

        Log.d("DynamicPosition", "Fake Walking started.")
        LocationService.doStop(context, true)
        LocationService.doStart(
            context,
            true,
            modifiedLocOrigin,
            modifiedLocDestination,
            modifiedTripDuration
        )
    }

    private fun getLocationInLatLngRad(
        radiusInMeters: Double,
        currentLocation: Location
    ): Location {
        val x0 = currentLocation.longitude
        val y0 = currentLocation.latitude
        val random = Random()

        // Convert radius from meters to degrees.
        val radiusInDegrees = radiusInMeters / 111320f

        // Get a random distance and a random angle.
        val u = random.nextDouble()
        val v = random.nextDouble()
        val w = radiusInDegrees * sqrt(u)
        val t = 2 * Math.PI * v
        // Get the x and y delta values.
        val x = w * cos(t)
        val y = w * sin(t)

        // Compensate the x value.
        val newX = x / cos(Math.toRadians(y0))
        val foundLatitude = y0 + y
        val foundLongitude = x0 + newX
        val copy = Location(currentLocation)
        copy.latitude = foundLatitude
        copy.longitude = foundLongitude
        return copy
    }
}