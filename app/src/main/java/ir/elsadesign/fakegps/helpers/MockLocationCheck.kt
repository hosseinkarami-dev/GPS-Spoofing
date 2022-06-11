package ir.elsadesign.fakegps.helpers

import android.content.Context
import android.provider.Settings


object MockLocationCheck {
    fun isMockLocationOn(context: Context): Boolean {
        return !Settings.Secure.getString(context.contentResolver,
            Settings.Secure.ALLOW_MOCK_LOCATION).equals("0")
    }
}