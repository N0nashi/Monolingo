package com.example.individ_project.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.individ_project.AppNotification
import com.example.individ_project.NotificationCenter
import com.example.individ_project.QuizActivity
import com.example.individ_project.R
import com.example.individ_project.UserManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutHeartsHome: LinearLayout
    private lateinit var tvHeartsTimerHome: TextView

    private var timerJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerView = view.findViewById(R.id.recycler_levels)
        recyclerView.layoutManager = LinearLayoutManager(context)

        layoutHeartsHome = view.findViewById(R.id.layout_hearts_home)
        tvHeartsTimerHome = view.findViewById(R.id.tv_hearts_timer_home)

        // Инициализируем менеджер при создании, чтобы данные были актуальны
        UserManager.init(requireContext())

        refreshLevels()
        updateHeartsUIOnHome()
        startHeartsTimerHome()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возврате на экран (например, после проигрыша)
        UserManager.init(requireContext())
        refreshLevels()
        updateHeartsUIOnHome()
        startHeartsTimerHome()
    }

    override fun onPause() {
        super.onPause()
        timerJob?.cancel()
    }

    private fun refreshLevels() {
        val maxOpenedLevel = UserManager.getMaxOpenedLevel()

        val baseLevels: List<Level> = List(10) { index ->
            val levelId = index + 1
            val isLocked = levelId > maxOpenedLevel
            Level(id = levelId, isLocked = isLocked)
        }

        val items: List<HomeItem> = buildItemsWithActs(baseLevels)

        val adapter = LevelsAdapter(
            items,
            onLevelClick = { level ->
                if (level.isLocked) {
                    Toast.makeText(
                        context,
                        "Уровень ${level.id} пока закрыт!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Пытаемся открыть уровень (проверка сердец внутри openLevel)
                    openLevel(level.id)
                }
            },
            onActContinueClick = { actIndex ->
                onActContinue(actIndex, maxOpenedLevel)
            }
        )

        recyclerView.adapter = adapter

        while (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
        recyclerView.addItemDecoration(LevelPathDecoration(adapter))
    }

    // --- ГЛАВНОЕ ИЗМЕНЕНИЕ ЗДЕСЬ ---
    private fun openLevel(levelId: Int) {
        // Проверяем наличие сердец перед запуском
        if (UserManager.getHearts() > 0) {
            val intent = Intent(context, QuizActivity::class.java)
            intent.putExtra("LEVEL_ID", levelId)
            startActivity(intent)
        } else {
            // Если сердец нет, показываем уведомление и никуда не переходим
            Toast.makeText(
                requireContext(),
                "Нет сердец! Подождите восстановления.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun buildItemsWithActs(levels: List<Level>): List<HomeItem> {
        val result = mutableListOf<HomeItem>()
        val levelsPerAct = 5

        var actIndex = 1
        var i = 0
        while (i < levels.size) {
            result += HomeItem.ActHeader(actIndex)

            val end = minOf(i + levelsPerAct, levels.size)
            for (j in i until end) {
                result += HomeItem.LevelItem(levels[j])
            }

            i += levelsPerAct
            actIndex++
        }
        return result
    }

    private fun onActContinue(actIndex: Int, maxOpenedGlobalLevel: Int) {
        val levelsPerAct = 5
        val firstLevelInAct = (actIndex - 1) * levelsPerAct + 1
        val lastLevelInAct = actIndex * levelsPerAct

        if (maxOpenedGlobalLevel < firstLevelInAct) {
            NotificationCenter.post(
                AppNotification(
                    title = "Акт недоступен",
                    message = "Пройди предыдущие уровни, чтобы открыть ACT $actIndex"
                )
            )
            return
        }

        val lastOpenedInAct = maxOpenedGlobalLevel.coerceAtMost(lastLevelInAct)
        // Здесь тоже вызывается openLevel, поэтому проверка сердец сработает и тут
        openLevel(lastOpenedInAct)
    }

    private fun updateHeartsUIOnHome() {
        layoutHeartsHome.removeAllViews()

        val hearts = UserManager.getHearts()
        val density = resources.displayMetrics.density

        repeat(UserManager.MAX_HEARTS) { index ->
            val img = ImageView(requireContext())
            img.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index > 0) marginStart = (4 * density).toInt()
            }
            img.setImageResource(
                if (index < hearts) R.drawable.ic_heart_full
                else R.drawable.ic_heart_empty
            )
            layoutHeartsHome.addView(img)
        }
    }

    private fun startHeartsTimerHome() {
        timerJob?.cancel()
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                // UserManager инициализирован в onResume/onCreateView, но можно дернуть init для надежности чтения SP
                // UserManager.init(requireContext())

                if (UserManager.isHeartsFull()) {
                    tvHeartsTimerHome.text = "Сердец максимум"
                } else {
                    val millis = UserManager.getMillisToNextHeart()
                    val totalSeconds = millis / 1000
                    val h = totalSeconds / 3600
                    val m = (totalSeconds % 3600) / 60
                    val s = totalSeconds % 60
                    tvHeartsTimerHome.text =
                        String.format("Следующее сердце через %02d:%02d:%02d", h, m, s)
                }
                delay(1000L)
            }
        }
    }
}
