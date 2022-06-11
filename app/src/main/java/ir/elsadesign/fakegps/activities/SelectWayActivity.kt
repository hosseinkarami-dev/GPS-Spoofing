package ir.elsadesign.fakegps.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import ir.elsadesign.fakegps.R
import ir.elsadesign.fakegps.adapters.SelectWayListAdapter
import ir.elsadesign.fakegps.databinding.ActivitySelectWayBinding
import ir.elsadesign.fakegps.helpers.MockLocationCheck
import ir.elsadesign.fakegps.service.DynamicPointService
import ir.elsadesign.fakegps.service.LocationService
import ir.elsadesign.fakegps.service.looper.MockLocationProvider
import ir.elsadesign.fakegps.service.looper.MockLocationProviderManager

class SelectWayActivity : BaseActivity() {
    private lateinit var binding: ActivitySelectWayBinding
    private lateinit var adapter: SelectWayListAdapter
    private val items = ArrayList<SelectWayListAdapter.Item>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectWayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        //   supportActionBar.setHomeAsUpIndicator(R.drawable.ic_back_gray)
        setSupportActionBar(binding.toolbar)
        binding.toolbarTitle.text = getString(R.string.select_way_activity_title)
        supportActionBar!!.setHomeButtonEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        registerReceiver(broadcastReceiver, startedIntentFilter)
        registerReceiver(broadcastReceiver, stoppedIntentFilter)

        items.addAll(
            listOf(
                SelectWayListAdapter.Item(
                    FIXED_POSITION,
                    getString(R.string.fixed_position),
                    R.string.fa_map_marker_solid
                ),
                SelectWayListAdapter.Item(
                    TRIP_SIMULATION,
                    getString(R.string.fake_trip),
                    R.string.fa_map_solid
                ),
                SelectWayListAdapter.Item(
                    DYNAMIC_POINT,
                    getString(R.string.dynamic_point),
                    R.string.fa_map_marked_alt_solid
                ),
                SelectWayListAdapter.Item(
                    HELP,
                    getString(R.string.help),
                    R.string.fa_question_solid
                ),
            )
        )

        adapter = SelectWayListAdapter(this, items)
        binding.listView.adapter = adapter

        binding.listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                FIXED_POSITION -> startActivity(Intent(this, FixedPositionActivity::class.java))
                TRIP_SIMULATION -> startActivity(Intent(this, TripSimulationActivity::class.java))
                DYNAMIC_POINT -> startActivity(Intent(this, DynamicPositionActivity::class.java))
                HELP -> startActivity(Intent(this, HelpActivity::class.java))
            }
        }

        binding.actionLayout.visibility =
            if (LocationService.isStarted()) View.VISIBLE else View.GONE

        binding.actionLayout.setOnClickListener {
            LocationService.doStop(this, true)
            DynamicPointService.doStop(this, true)
            it.visibility = View.GONE
        }

    }

    override fun onResume() {
        super.onResume()

        when {
            Settings.Secure.getInt(
                this.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) == 0 -> {
                binding.messageLayout.visibility = View.VISIBLE
                binding.messageText.text =
                    getString(R.string.you_have_not_enabled_developer_options)
                binding.messageButton.setOnClickListener { startActivity(Intent(Settings.ACTION_SETTINGS)); }
            }

            MockLocationCheck.isMockLocationOn(this) -> {
                binding.messageLayout.visibility = View.VISIBLE
                binding.messageText.text =
                    getString(R.string.select_this_app_as_your_mock_location_app)
                binding.messageButton.setOnClickListener { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)); }
            }

            else -> binding.messageLayout.visibility = View.GONE
        }
    }

    private val startedIntentFilter = IntentFilter("service_started")
    private val stoppedIntentFilter = IntentFilter("service_stopped")
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, intent: Intent) {
            when (intent.action) {
                "service_started" -> binding.actionLayout.visibility = View.VISIBLE
                "service_stopped" -> binding.actionLayout.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val FIXED_POSITION = 0
        private const val TRIP_SIMULATION = 1
        private const val DYNAMIC_POINT = 2
        private const val HELP = 3
    }
}