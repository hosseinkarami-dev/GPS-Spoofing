package ir.elsadesign.fakegps.models

import java.lang.NumberFormatException

class LocPoint {
    var latitude = 0.0
    var longitude = 0.0

    constructor(locPoint: LocPoint) {
        latitude = locPoint.latitude
        longitude = locPoint.longitude
    }

    constructor(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    constructor(text: String) {
        val parts = text.split(",").toTypedArray()
        if (parts.size == 2) {
            latitude = parts[0].trim { it <= ' ' }.toDouble()
            longitude = parts[1].trim { it <= ' ' }.toDouble()
        } else {
            throw NumberFormatException("expected: latitude,longitude")
        }
    }

    override fun toString(): String {
        return String.format(
            "%1\$s, %2\$s",
            latitude,
            longitude
        )
    }

    fun equals(locPoint: LocPoint): Boolean {
        val threshold = 1e-4
        return equals(locPoint, threshold)
    }

    fun equals(locPoint: LocPoint, threshold: Double): Boolean {
        val thatLat = locPoint.latitude
        val thatLon = locPoint.longitude
        return Math.abs(thatLat - latitude) < threshold && Math.abs(thatLon - longitude) < threshold // todo: fix that longitude difference doesn't work if straddling opposite sides of the 180th meridian (ex: 179.99999 and -179.99999)
    }
}
