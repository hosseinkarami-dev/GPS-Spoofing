package ir.elsadesign.fakegps.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import info.androidhive.fontawesome.FontDrawable
import ir.elsadesign.fakegps.R
import java.net.URL
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object HelperMain {
    fun thousandSeparator(number: Int): String {
        val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
        formatter.applyPattern("#,###")
        return formatter.format(number.toLong())
    }

    fun getDensity(context: Context): Int {
        return context.resources.displayMetrics.density.toInt()
    }

     fun rand(from: Int, to: Int): Int {
        return Random().nextInt(to - from) + from
    }

    fun isValidURL(urlString: String?): Boolean {
        return try {
            val url = URL(urlString)
            url.toURI()
            true
        } catch (exception: Exception) {
            false
        }
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getScreenWidthInDp(activity: Activity): Float {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = activity.resources.displayMetrics.density
        return outMetrics.widthPixels / density
    }

    fun showKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.showSoftInput(view, 0)
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun isKeyboardShowing(activity: Activity): Boolean {
        val flag = AtomicBoolean(false)
        val contentView = activity.findViewById<View>(R.id.content)
        contentView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            contentView.getWindowVisibleDisplayFrame(r)
            val screenHeight = contentView.rootView.height
            val keypadHeight = screenHeight - r.bottom
            flag.set(keypadHeight > screenHeight * 0.15)
        }
        return flag.get()
    }

    private fun getNavigationBarHeight(activity: Activity): Int {
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
        val usableHeight = metrics.heightPixels
        activity.windowManager.defaultDisplay.getRealMetrics(metrics)
        val realHeight = metrics.heightPixels
        return if (realHeight > usableHeight) realHeight - usableHeight else 0
    }

    fun getDensityName(context: Context): String {
        val density = context.resources.displayMetrics.density
        if (density >= 4.0) {
            return "xxxhdpi"
        }
        if (density >= 3.0) {
            return "xxhdpi"
        }
        if (density >= 2.0) {
            return "xhdpi"
        }
        if (density >= 1.5) {
            return "hdpi"
        }
        return if (density >= 1.0) {
            "mdpi"
        } else "ldpi"
    }

    fun setIcon(context: Context, view: Any, id: Int, color: Int) {
        val drawable = FontDrawable(
            context,
            id,
            true,
            false
        )
        drawable.setTextColor(color)
        when (view) {
            is ImageView -> view.setImageDrawable(drawable)
            is FloatingActionButton -> view.setImageDrawable(drawable)
            is MenuItem -> {
                view.icon = drawable
                drawable.textSize = 20f
            }
        }
    }
}