package com.example.individ_project

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.min

object UserManager {
    private const val PREF_NAME = "MiniLingoPrefs"
    private lateinit var prefs: SharedPreferences

    private const val KEY_MAX_LEVEL = "KEY_MAX_LEVEL"
    private const val KEY_XP = "KEY_XP"
    private const val KEY_NAME = "KEY_NAME"
    private const val KEY_AVATAR = "KEY_AVATAR"

    private const val KEY_WORDS_LEARNED = "KEY_WORDS_LEARNED"
    private const val KEY_WORDS_SET = "KEY_WORDS_SET"
    private const val KEY_LEVELS_PASSED = "KEY_LEVELS_PASSED"

    private const val KEY_HEARTS = "KEY_HEARTS"
    private const val KEY_LAST_HEART_LOSS = "KEY_LAST_HEART_LOSS"

    const val MAX_HEARTS = 5
    private const val HEART_RECOVER_MS = 60L * 60L * 1000L // 1 час

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        ensureDefaults()
        recoverHeartsIfNeeded()
    }

    private fun ensureDefaults() {
        if (!prefs.contains(KEY_HEARTS)) {
            prefs.edit().putInt(KEY_HEARTS, MAX_HEARTS).apply()
        }
        if (!prefs.contains(KEY_MAX_LEVEL)) {
            prefs.edit().putInt(KEY_MAX_LEVEL, 1).apply()
        }
        if (!prefs.contains(KEY_NAME)) {
            prefs.edit().putString(KEY_NAME, "Студент").apply()
        }
        if (!prefs.contains(KEY_AVATAR)) {
            // дефолтный аватар, поставь свой ресурс
            prefs.edit().putInt(KEY_AVATAR, R.drawable.avatar_1).apply()
        }
    }

    // ---------- ПРОГРЕСС УРОВНЕЙ ----------

    fun setLevelPassed(levelId: Int) {
        val currentMax = getMaxOpenedLevel()
        if (levelId >= currentMax) {
            prefs.edit().putInt(KEY_MAX_LEVEL, levelId + 1).apply()
        }
        val passed = getLevelsPassed()
        if (levelId > passed) {
            prefs.edit().putInt(KEY_LEVELS_PASSED, levelId).apply()
        }
    }

    fun getMaxOpenedLevel(): Int =
        prefs.getInt(KEY_MAX_LEVEL, 1)

    fun getLevelsPassed(): Int =
        prefs.getInt(KEY_LEVELS_PASSED, 0)

    // ---------- XP ----------

    fun addXP(amount: Int) {
        val current = getXP()
        prefs.edit().putInt(KEY_XP, current + amount).apply()
    }

    fun getXP(): Int =
        prefs.getInt(KEY_XP, 0)

    // ---------- СЛОВА (РЕАЛЬНО ВЫУЧЕННЫЕ) ----------

    fun addLearnedWords(words: List<String>) {
        if (words.isEmpty()) return
        val currentSet = prefs.getStringSet(KEY_WORDS_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.addAll(words)
        prefs.edit()
            .putStringSet(KEY_WORDS_SET, currentSet)
            .apply()
    }

    fun getWordsLearned(): Int {
        return prefs.getStringSet(KEY_WORDS_SET, emptySet())?.size ?: 0
    }

    // ---------- СЕРДЦА ----------

    fun getHearts(): Int =
        prefs.getInt(KEY_HEARTS, MAX_HEARTS)

    fun isHeartsFull(): Boolean =
        getHearts() >= MAX_HEARTS

    fun loseHeart() {
        val current = getHearts()
        if (current <= 0) return
        prefs.edit()
            .putInt(KEY_HEARTS, current - 1)
            .putLong(KEY_LAST_HEART_LOSS, System.currentTimeMillis())
            .apply()
    }

    private fun recoverHeartsIfNeeded() {
        val current = getHearts()
        if (current >= MAX_HEARTS) return

        val lastLoss = prefs.getLong(KEY_LAST_HEART_LOSS, 0L)
        if (lastLoss == 0L) return

        val now = System.currentTimeMillis()
        val diff = now - lastLoss
        if (diff <= 0) return

        val recovered = (diff / HEART_RECOVER_MS).toInt()
        if (recovered <= 0) return

        val newHearts = min(MAX_HEARTS, current + recovered)
        prefs.edit()
            .putInt(KEY_HEARTS, newHearts)
            .putLong(
                KEY_LAST_HEART_LOSS,
                if (newHearts >= MAX_HEARTS) 0L else now - (diff % HEART_RECOVER_MS)
            )
            .apply()
    }

    fun getMillisToNextHeart(): Long {
        val hearts = getHearts()
        if (hearts >= MAX_HEARTS) return 0L

        val lastLoss = prefs.getLong(KEY_LAST_HEART_LOSS, 0L)
        if (lastLoss == 0L) return HEART_RECOVER_MS

        val now = System.currentTimeMillis()
        val diff = now - lastLoss
        val remain = HEART_RECOVER_MS - diff
        return if (remain < 0L) 0L else remain
    }

    fun buyHearts(count: Int, priceXp: Int): Boolean {
        val hearts = getHearts()
        if (hearts >= MAX_HEARTS) return false

        val xp = getXP()
        if (xp < priceXp) return false

        val newHearts = min(MAX_HEARTS, hearts + count)
        prefs.edit()
            .putInt(KEY_HEARTS, newHearts)
            .putInt(KEY_XP, xp - priceXp)
            .putLong(
                KEY_LAST_HEART_LOSS,
                if (newHearts >= MAX_HEARTS) 0L else prefs.getLong(KEY_LAST_HEART_LOSS, 0L)
            )
            .apply()
        return true
    }

    // ---------- ИМЯ ----------

    fun getUserName(): String =
        prefs.getString(KEY_NAME, "Студент") ?: "Студент"

    fun setUserName(name: String) {
        val safe = name.trim().ifBlank { "Студент" }
        prefs.edit().putString(KEY_NAME, safe).apply()
    }

    // ---------- АВАТАР ----------

    fun getAvatarRes(): Int =
        prefs.getInt(KEY_AVATAR, R.drawable.avatar_1)

    fun setAvatarRes(resId: Int) {
        prefs.edit().putInt(KEY_AVATAR, resId).apply()
    }
}
