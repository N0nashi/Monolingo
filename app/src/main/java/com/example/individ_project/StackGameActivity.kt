package com.example.individ_project

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StackGameActivity : AppCompatActivity() {

    // Ссылки на элементы уведомления
    private lateinit var layoutXpNotification: LinearLayout
    private lateinit var tvXpAmount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_stack_game)

        val gameView = findViewById<StackGameView>(R.id.game_view)
        val tvScore = findViewById<TextView>(R.id.tv_score)
        val layoutGameOver = findViewById<LinearLayout>(R.id.layout_game_over)
        val tvGameOverScore = findViewById<TextView>(R.id.tv_game_over_score)
        val tvBestScore = findViewById<TextView>(R.id.tv_best_score)
        val btnRestart = findViewById<Button>(R.id.btn_restart)
        val btnClose = findViewById<View>(R.id.btn_close)

        // Инициализируем новые view для уведомления
        layoutXpNotification = findViewById(R.id.layout_xp_notification)
        tvXpAmount = findViewById(R.id.tv_xp_amount)

        val prefs = getSharedPreferences("StackGame", Context.MODE_PRIVATE)
        var bestScore = prefs.getInt("BestScore", 0)

        // Обновление счета во время игры
        gameView.onScoreUpdate = { score ->
            tvScore.text = score.toString()
        }

        // Конец игры
        gameView.onGameOver = {
            runOnUiThread {
                val currentScore = gameView.score

                // --- XP + Анимация ---
                if (currentScore > 0) {
                    try {
                        UserManager.init(this)
                        UserManager.addXP(currentScore)
                        // Запускаем красивое современное уведомление
                        showXPNotification(currentScore)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (currentScore > bestScore) {
                    bestScore = currentScore
                    prefs.edit().putInt("BestScore", bestScore).apply()
                }

                // UI Проигрыша
                tvScore.visibility = View.INVISIBLE

                // Красивые цифры результата
                tvGameOverScore.text = currentScore.toString()
                tvBestScore.text = "BEST: $bestScore"

                layoutGameOver.visibility = View.VISIBLE
                layoutGameOver.alpha = 0f
                layoutGameOver.animate().alpha(1f).setDuration(500).start()
            }
        }

        btnRestart.setOnClickListener {
            layoutGameOver.visibility = View.GONE
            tvScore.visibility = View.VISIBLE
            tvScore.text = "0"
            gameView.resetGame()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    // Метод для анимации XP (Современный стиль)
    private fun showXPNotification(amount: Int) {
        // Устанавливаем текст
        tvXpAmount.text = "+$amount XP"

        // 1. Подготовка: ставим выше экрана и делаем видимым, но прозрачным
        layoutXpNotification.translationY = -200f
        layoutXpNotification.visibility = View.VISIBLE
        layoutXpNotification.alpha = 0f

        // Добавляем эффект масштабирования (scale) для динамики
        layoutXpNotification.scaleX = 0.8f
        layoutXpNotification.scaleY = 0.8f

        // 2. Анимация появления: Вылет вниз + Увеличение + Появление
        layoutXpNotification.animate()
            .translationY(0f) // Возвращаем на место (margin в XML задает конечную точку)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f)) // Эффект "пружинки" (перелет и возврат)
            .setListener(null)
            .withEndAction {
                // 3. Ждем и уезжаем обратно вверх
                layoutXpNotification.animate()
                    .translationY(-200f) // Улетаем вверх
                    .alpha(0f) // Исчезаем
                    .setStartDelay(2500) // Висим 2.5 секунды
                    .setDuration(350)
                    .setInterpolator(android.view.animation.AccelerateInterpolator()) // Разгон при уходе
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            layoutXpNotification.visibility = View.INVISIBLE
                            // Сброс всех параметров для следующего раза
                            layoutXpNotification.animate().setStartDelay(0)
                            layoutXpNotification.scaleX = 1f
                            layoutXpNotification.scaleY = 1f
                        }
                    })
                    .start()
            }
            .start()
    }
}
