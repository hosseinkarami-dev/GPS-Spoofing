package ir.elsadesign.fakegps.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import ir.elsadesign.fakegps.activities.RuntimePermissionsActivity
import ir.elsadesign.fakegps.helpers.HelperMain
import ir.elsadesign.fakegps.models.LocPoint
import ir.elsadesign.fakegps.models.SharedPrefs
import ir.elsadesign.fakegps.receiver.DynamicMovementReceiver
import java.util.*

class DynamicPointService : Service() {
    private var locationBinder: IBinder? = null

    class LocationBinder : Binder()

    override fun onCreate() {
        super.onCreate()
        locationBinder = LocationBinder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return locationBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onStart(intent!!, startId)
        return START_STICKY
    }

    override fun onStart(intent: Intent, startId: Int) {
        processIntent(intent)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        dayCountDownTimer?.cancel()
    }

    private var radius = 2000
    private var numberOfTrips = 3
    private var speed = 4.9f
    private val randomIntegers = ArrayList<Int>()
    private var latitude = 0.0
    private var longitude = 0.0

    private fun processIntent(intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            ACTION_START -> {
                running = true
                radius = intent.getIntExtra("radius", 2000)
                numberOfTrips = intent.getIntExtra("number_of_trips", 3)
                speed = intent.getFloatExtra("speed", 4.9f)
                latitude = intent.getDoubleExtra("latitude", 0.0)
                longitude = intent.getDoubleExtra("longitude", 0.0)

                if (numberOfTrips == 0)
                    return

                val modifiedLoc = LocPoint("$latitude,$longitude")
                SharedPrefs.putTripOrigin(this, modifiedLoc)
                LocationService.doStart(this, true, modifiedLoc, null, 0)

                Log.d("DynamicPosition", "radius: $radius, Number of trips: $numberOfTrips")

                start()
            }
            ACTION_STOP -> {
                running = false
                stopSelf()
            }
        }
    }

    private fun start() {
        val secondsRemainedToMidnight = secondsRemained()

        randomIntegers.clear()
        for (i in 0 until numberOfTrips)
            randomIntegers.add(HelperMain.rand(0, secondsRemainedToMidnight))

        randomIntegers.sortWith { a, b ->
            a - b
        }

        clock(extractMin())

        dayCountDownTimer =
            object : CountDownTimer(((secondsRemainedToMidnight + 5) * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    Log.d("DynamicPosition", "Day finished.")
                    dayCountDownTimer?.cancel()
                    countDownTimer?.cancel()
                    start()
                }
            }.apply {
                start()
            }
    }

    private fun secondsRemained(): Int {
        val c = Calendar.getInstance()
        val now: Long = c.timeInMillis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val passed = now - c.timeInMillis
        val secondsPassed = passed / 1000
        return (86400 - secondsPassed).toInt()
    }

    private fun extractMin(): Int {
        val min = randomIntegers[0]

        randomIntegers.remove(min)

        for (i in 0 until randomIntegers.size)
            randomIntegers[i] -= min

        return min
    }

    private var countDownTimer: CountDownTimer? = null
    private var dayCountDownTimer: CountDownTimer? = null

    private fun clock(next: Int) {
        countDownTimer = object : CountDownTimer((next * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d("DynamicPosition", "Countdown: " + (millisUntilFinished / 1000).toString())
            }

            override fun onFinish() {
                buildAlarm()

                if (randomIntegers.size == 0) {
                    countDownTimer?.cancel()
                    return
                }

                clock(extractMin())
            }
        }.apply {
            start()
        }
    }

    fun isStarted() = running

    private fun buildAlarm() {
        val intent = Intent(this, DynamicMovementReceiver::class.java).apply {
            putExtra("radius", radius)
            putExtra("speed", speed)
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            0
        )

        val alarmManager =
            getSystemService(RuntimePermissionsActivity.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent
        )
    }


    companion object {
        fun doStop(context: Context, broadcast: Boolean) {
            LocationService.doStop(context, broadcast)
            if (!running) return
            val intent = Intent(context, DynamicPointService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }

        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        var running = false
    }
}