package ir.elsadesign.fakegps.helpers

import android.content.Context
import android.location.LocationManager


class HelperGPS {
    companion object {
        fun isEnabled(context: Context) =
            (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
    }
}