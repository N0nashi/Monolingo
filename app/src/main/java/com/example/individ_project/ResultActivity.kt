package com.example.individ_project

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_result)

        val mistakes = intent.getIntExtra("MISTAKES", 0)
        val totalTimeSec = intent.getLongExtra("TIME_SECONDS", 0)
        val accuracy = intent.getIntExtra("ACCURACY", 100)

        findViewById<TextView>(R.id.tv_mistakes).text = mistakes.toString()
        findViewById<TextView>(R.id.tv_accuracy).text = "$accuracy%"

        // Форматирование времени (ММ:СС)
        val minutes = totalTimeSec / 60
        val seconds = totalTimeSec % 60
        findViewById<TextView>(R.id.tv_time).text = String.format("%02d:%02d", minutes, seconds)

        findViewById<Button>(R.id.btn_finish_level).setOnClickListener {
            finish()
        }
    }
}
