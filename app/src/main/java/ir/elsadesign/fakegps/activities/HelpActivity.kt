package ir.elsadesign.fakegps.activities

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import ir.elsadesign.fakegps.R
import ir.elsadesign.fakegps.adapters.HelpActivityAdapter
import ir.elsadesign.fakegps.databinding.ActivityHelpBinding

class HelpActivity : BaseActivity() {
    private lateinit var binding: ActivityHelpBinding
    private lateinit var adapter: HelpActivityAdapter
    private val items = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
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
        supportActionBar!!.setHomeButtonEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        items.apply {
            add(getString(R.string.help_1))
            add(getString(R.string.help_2))
            add(getString(R.string.help_3))
            add(getString(R.string.help_4))
            add(getString(R.string.help_5))
            add(getString(R.string.help_6))
            add(getString(R.string.help_7))
        }

        adapter = HelpActivityAdapter(this, items)
        binding.listView.adapter = adapter
    }
}