package com.example.individ_project

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.individ_project.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import android.view.ViewGroup


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        // ---- Banner views ----
        val banner = findViewById<MaterialCardView>(R.id.top_banner)
        val title = findViewById<TextView>(R.id.banner_title)
        val message = findViewById<TextView>(R.id.banner_message)
        val close = findViewById<ImageView>(R.id.banner_close)

        // Слушаем события и показываем баннер
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationCenter.events.collect { n ->
                    showTopBanner(banner, title, message, close, n.title, n.message)
                }
            }
        }
    }

    private fun showTopBanner(
        banner: View,
        titleView: TextView,
        messageView: TextView,
        closeView: View,
        t: String,
        m: String
    ) {
        titleView.text = t
        messageView.text = m

        banner.visibility = View.VISIBLE

        // Сдвиг баннера ниже статус-бара
        val density = banner.resources.displayMetrics.density
        val topMarginPx = (density * 72).toInt()   // меняй 72 → 88/96, если нужно ещё ниже
        val sideMarginPx = (density * 16).toInt()

        val lp = banner.layoutParams
        if (lp is ViewGroup.MarginLayoutParams) {
            lp.topMargin = topMarginPx
            lp.marginStart = sideMarginPx
            lp.marginEnd = sideMarginPx
            banner.layoutParams = lp
        }

        banner.post {
            banner.translationY = -banner.height.toFloat()
            banner.alpha = 0f
            banner.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(220)
                .start()
        }

        val hide = {
            banner.animate()
                .translationY(-banner.height.toFloat())
                .alpha(0f)
                .setDuration(180)
                .withEndAction { banner.visibility = View.GONE }
                .start()
        }

        closeView.setOnClickListener { hide() }
        banner.postDelayed({ hide() }, 2200)
    }
}
