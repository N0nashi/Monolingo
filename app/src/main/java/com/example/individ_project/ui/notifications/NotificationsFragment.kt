package com.example.individ_project.ui.notifications

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.individ_project.AppNotification
import com.example.individ_project.NotificationCenter
import com.example.individ_project.R
import com.example.individ_project.StackGameActivity // Импортируем нашу игру
import com.example.individ_project.UserManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserLevel: TextView
    private lateinit var tvWordsLearned: TextView
    private lateinit var tvLevelsPassed: TextView
    private lateinit var tvXp: TextView
    private lateinit var tvHeartsTimer: TextView

    private lateinit var btnBuy1: Button
    private lateinit var btnBuy3: Button
    private lateinit var btnBuy5: Button

    // Новая кнопка для игры
    private lateinit var btnPlayGame: Button

    private var timerJob: Job? = null

    // Список аватарок
    private val avatarList = listOf(
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        ivAvatar = view.findViewById(R.id.iv_avatar)
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvUserLevel = view.findViewById(R.id.tv_user_level)
        tvWordsLearned = view.findViewById(R.id.tv_words_learned)
        tvLevelsPassed = view.findViewById(R.id.tv_levels_passed)
        tvXp = view.findViewById(R.id.tv_xp)
        tvHeartsTimer = view.findViewById(R.id.tv_hearts_timer)

        btnBuy1 = view.findViewById(R.id.btn_buy_1_heart)
        btnBuy3 = view.findViewById(R.id.btn_buy_3_hearts)
        btnBuy5 = view.findViewById(R.id.btn_buy_5_hearts)

        // Находим кнопку "Играть" (не забудьте добавить ее в макет, код ниже)
        btnPlayGame = view.findViewById(R.id.btn_play_game)

        btnBuy1.setOnClickListener { buyHearts(1, 50) }
        btnBuy3.setOnClickListener { buyHearts(3, 120) }
        btnBuy5.setOnClickListener { buyHearts(5, 180) }

        tvUserName.setOnClickListener { showEditProfileDialog() }
        ivAvatar.setOnClickListener { showEditProfileDialog() }

        // ЗАПУСК ИГРЫ STACK GAME
        btnPlayGame.setOnClickListener {
            val intent = Intent(requireContext(), StackGameActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        UserManager.init(requireContext())
        bindData()
        startHeartsTimer()
    }

    override fun onPause() {
        super.onPause()
        timerJob?.cancel()
    }

    private fun bindData() {
        val name = UserManager.getUserName()
        val words = UserManager.getWordsLearned()
        val levels = UserManager.getLevelsPassed()
        val xp = UserManager.getXP()

        tvUserName.text = name
        tvUserLevel.text = "Уровень: Новичок"
        tvWordsLearned.text = "Выучено слов: $words"
        tvLevelsPassed.text = "Пройдено уровней: $levels"
        tvXp.text = "XP: $xp"

        ivAvatar.setImageResource(UserManager.getAvatarRes())
    }

    private fun showEditProfileDialog() {
        val context = requireContext()
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_profile)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val ivPreview = dialog.findViewById<ImageView>(R.id.iv_avatar_preview)
        val btnPrev = dialog.findViewById<ImageButton>(R.id.btn_prev)
        val btnNext = dialog.findViewById<ImageButton>(R.id.btn_next)
        val etNameInput = dialog.findViewById<EditText>(R.id.et_name)
        val btnSave = dialog.findViewById<Button>(R.id.btn_save)

        etNameInput.setText(UserManager.getUserName())

        var currentRes = UserManager.getAvatarRes()
        var currentIndex = avatarList.indexOf(currentRes)
        if (currentIndex < 0) currentIndex = 0

        ivPreview.setImageResource(avatarList[currentIndex])

        btnPrev.setOnClickListener {
            currentIndex--
            if (currentIndex < 0) currentIndex = avatarList.size - 1
            ivPreview.setImageResource(avatarList[currentIndex])
        }

        btnNext.setOnClickListener {
            currentIndex++
            if (currentIndex >= avatarList.size) currentIndex = 0
            ivPreview.setImageResource(avatarList[currentIndex])
        }

        btnSave.setOnClickListener {
            val newName = etNameInput.text.toString().trim()
            if (newName.isNotEmpty()) {
                UserManager.setUserName(newName)
                UserManager.setAvatarRes(avatarList[currentIndex])
                bindData()
                dialog.dismiss()
                Toast.makeText(context, "Профиль обновлен!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun buyHearts(count: Int, price: Int) {
        val success = UserManager.buyHearts(count, price)
        if (success) {
            NotificationCenter.post(
                AppNotification(
                    title = "Сердца куплены",
                    message = "Получено сердец: $count"
                )
            )
        } else {
            NotificationCenter.post(
                AppNotification(
                    title = "Покупка недоступна",
                    message = "Недостаточно XP или сердца уже полные"
                )
            )
        }
        UserManager.init(requireContext())
        bindData()
        startHeartsTimer()
    }

    private fun startHeartsTimer() {
        timerJob?.cancel()
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (UserManager.isHeartsFull()) {
                    tvHeartsTimer.text = "Сердец максимум"
                    tvHeartsTimer.setTextColor(Color.parseColor("#BDC3C7"))
                } else {
                    val millis = UserManager.getMillisToNextHeart()
                    val totalSeconds = millis / 1000
                    val h = totalSeconds / 3600
                    val m = (totalSeconds % 3600) / 60
                    val s = totalSeconds % 60
                    tvHeartsTimer.text =
                        String.format("Следующее сердце через %02d:%02d:%02d", h, m, s)
                    tvHeartsTimer.setTextColor(Color.parseColor("#E74C3C"))
                }
                delay(1000L)
            }
        }
    }
}
