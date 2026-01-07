package com.example.individ_project

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.individ_project.cards.data.CardsRepository
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

import com.example.individ_project.R

class QuizActivity : AppCompatActivity() {

    // --- Данные уровня ---
    private var currentLevelId: Int = 1
    private var questionList: MutableList<QuestionData> = mutableListOf()
    private var currentQuestionIndex: Int = 0

    // Статистика
    private var startTime: Long = 0
    private var mistakesCount: Int = 0
    private var initialQuestionCount: Int = 0

    private var solvedCount: Int = 0

    // --- Храним исходный текст вопроса ---
    private var currentQuestionTemplate: String = ""

    // Для режима предложений
    private val wordBankList = mutableListOf<String>()
    private val answerList = mutableListOf<String>()
    private var maxSentenceLength: Int = 0

    private var isAnswerChecked = false

    // --- View ---
    private lateinit var progressBar: ProgressBar
    private lateinit var tvQuestion: TextView
    private lateinit var btnCheck: Button
    private lateinit var btnNext: Button
    private lateinit var btnClose: ImageView

    private lateinit var containerSentence: LinearLayout
    private lateinit var containerButtons: LinearLayout
    private lateinit var rvPairs: RecyclerView

    private lateinit var layoutAnswerArea: FlexboxLayout
    private lateinit var layoutWordBank: FlexboxLayout
    private lateinit var layoutHearts: LinearLayout

    private lateinit var optionsButtons: List<Button>

    // Matching pairs
    private var pairsAdapter: PairsAdapter? = null
    private var matchesFoundCount = 0
    private var totalPairsInQuestion = 0

    private enum class NoticeType { LEVEL_PASSED, LOAD_ERROR, CORRECT, WRONG, NO_HEARTS, INFO }

    data class MatchItem(
        val id: Int,
        val text: String,
        var isMatched: Boolean = false,
        var isSelected: Boolean = false
    )

    data class LearnedPair(
        val front: String,
        val back: String
    )

    private val learnedPairs = mutableListOf<LearnedPair>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_quiz)

        currentLevelId = intent.getIntExtra("LEVEL_ID", 1)
        UserManager.init(this)

        if (UserManager.getHearts() <= 0) {
            handleNoHearts()
            return
        }

        initViews()
        loadQuestions()

        startTime = System.currentTimeMillis()
        setQuestion()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progress_bar)
        tvQuestion = findViewById(R.id.tv_question)
        btnCheck = findViewById(R.id.btn_check)
        btnNext = findViewById(R.id.btn_next)
        btnClose = findViewById(R.id.btn_close)

        containerSentence = findViewById(R.id.container_sentence)
        containerButtons = findViewById(R.id.container_buttons)
        rvPairs = findViewById(R.id.rv_pairs)

        layoutAnswerArea = findViewById(R.id.layout_answer_area)
        layoutWordBank = findViewById(R.id.layout_word_bank)
        layoutHearts = findViewById(R.id.layout_hearts)

        updateHeartsUI()

        btnClose.setOnClickListener { finish() }
        btnCheck.setOnClickListener { checkSentenceAnswer() }

        optionsButtons = listOf(
            findViewById(R.id.option_1),
            findViewById(R.id.option_2),
            findViewById(R.id.option_3),
            findViewById(R.id.option_4)
        )

        optionsButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (!isAnswerChecked) checkButtonAnswer(index, button)
            }
        }
    }

    private fun loadQuestions() {
        val allLevels = AssetsReader.loadLevels(this)
        val levelData = allLevels.find { it.id == currentLevelId }

        if (levelData != null && levelData.questions.isNotEmpty()) {
            questionList = levelData.questions.shuffled().toMutableList()
            initialQuestionCount = questionList.size

            // Настройка прогресс бара
            progressBar.max = initialQuestionCount
            progressBar.progress = 0
            solvedCount = 0
        } else {
            showTopNotice(NoticeType.LOAD_ERROR, "Ошибка загрузки уровня!")
            finish()
        }
    }

    private fun incrementProgress() {
        if (solvedCount < initialQuestionCount) {
            solvedCount++
            progressBar.progress = solvedCount
        }
    }

    private fun setQuestion() {
        if (currentQuestionIndex >= questionList.size) {
            finishLevel()
            return
        }

        val question = questionList[currentQuestionIndex]

        tvQuestion.text = question.text
        tvQuestion.setTextColor(Color.WHITE)
        tvQuestion.setTypeface(null, Typeface.BOLD)


        isAnswerChecked = false
        btnNext.visibility = View.GONE
        btnCheck.visibility = View.GONE

        containerSentence.visibility = View.GONE
        containerButtons.visibility = View.GONE
        rvPairs.visibility = View.GONE

        when (question.type) {
            "sentence" -> setupSentenceMode(question)
            "matching" -> setupMatchingMode(question)
            else -> setupButtonsMode(question)
        }
    }

    // ================= КНОПКИ =================

    private fun setupButtonsMode(question: QuestionData) {
        containerButtons.visibility = View.VISIBLE
        currentQuestionTemplate = question.text
        resetButtonStyles()

        for (i in optionsButtons.indices) {
            if (i < question.options.size) {
                optionsButtons[i].text = question.options[i]
                optionsButtons[i].visibility = View.VISIBLE
            } else {
                optionsButtons[i].visibility = View.GONE
            }
        }
    }

    private fun checkButtonAnswer(selectedIndex: Int, button: Button) {
        if (isAnswerChecked) return
        isAnswerChecked = true
        val question = questionList[currentQuestionIndex]
        val selectedText = button.text.toString()

        updateQuestionTextWithSelection(selectedText)

        if (selectedIndex == question.correctIndex) {
            // ПРАВИЛЬНО
            incrementProgress()

            button.setBackgroundResource(R.drawable.bg_button_green)
            button.setTextColor(Color.WHITE)
            button.setTypeface(null, Typeface.BOLD)
            showTopNotice(NoticeType.CORRECT, "Правильно")

            if (question.type == "word") {
                val rawEng = extractWordFromQuestionText(currentQuestionTemplate)
                val rawRus = question.options[question.correctIndex]
                val (eng, rus) = when {
                    isLatin(rawEng) && isCyrillic(rawRus) -> rawEng to rawRus
                    isLatin(rawRus) && isCyrillic(rawEng) -> rawRus to rawEng
                    else -> rawEng to rawRus
                }
                if (eng.isNotBlank() && rus.isNotBlank()) learnedPairs += LearnedPair(eng, rus)

            } else if (question.type == "fill") {
                val rus = extractHintInBrackets(currentQuestionTemplate)
                val eng = question.options[question.correctIndex]
                if (eng.isNotBlank() && rus.isNotBlank()) learnedPairs += LearnedPair(eng, rus)
            }

            btnNext.visibility = View.VISIBLE
            btnNext.setOnClickListener { goToNextQuestion() }

        } else {
            // ОШИБКА
            handleMistake(question)

            button.setBackgroundResource(R.drawable.bg_button_wrong)
            button.setTextColor(Color.WHITE)
            highlightCorrectButton(question.correctIndex)

            if (UserManager.getHearts() > 0) {
                showTopNotice(NoticeType.WRONG, "Ошибка! Правильный ответ выделен")
                btnNext.visibility = View.VISIBLE
                btnNext.setOnClickListener { goToNextQuestion() }
            } else {
                handleNoHearts()
            }
        }
    }

    private fun updateQuestionTextWithSelection(insertedWord: String) {
        val regex = "_+".toRegex()
        val matchResult = regex.find(currentQuestionTemplate)

        if (matchResult != null) {
            val range = matchResult.range
            val prefix = currentQuestionTemplate.substring(0, range.first)
            val suffix = currentQuestionTemplate.substring(range.last + 1)

            val newText = prefix + insertedWord + suffix
            val spannable = SpannableString(newText)

            val startIndex = range.first
            val endIndex = startIndex + insertedWord.length

            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#F39C12")), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(UnderlineSpan(), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            tvQuestion.text = spannable
        }
    }

    private fun highlightCorrectButton(index: Int) {
        if (index in optionsButtons.indices) {
            val correctBtn = optionsButtons[index]
            correctBtn.setBackgroundResource(R.drawable.bg_button_green)
            correctBtn.setTextColor(Color.WHITE)
            correctBtn.setTypeface(null, Typeface.BOLD)
        }
    }

    private fun resetButtonStyles() {
        optionsButtons.forEach {
            it.setBackgroundResource(R.drawable.bg_button_white)
            it.setTextColor(Color.parseColor("#2C3E50"))
            it.setTypeface(null, Typeface.NORMAL)
        }
    }

    // ================= ПРЕДЛОЖЕНИЯ =================

    private fun setupSentenceMode(question: QuestionData) {
        containerSentence.visibility = View.VISIBLE
        btnCheck.visibility = View.VISIBLE

        currentQuestionTemplate = question.text

        wordBankList.clear()
        answerList.clear()
        wordBankList.addAll(question.options)
        wordBankList.shuffle()

        val correctWords = question.correctAnswer?.trim()?.split("\\s+".toRegex())
        maxSentenceLength = correctWords?.size ?: question.options.size

        updateFlexboxContainers()
        btnCheck.isEnabled = false
        btnCheck.alpha = 0.5f
    }

    private fun checkSentenceAnswer() {
        val question = questionList[currentQuestionIndex]
        val userSentence = answerList.joinToString(" ")

        if (userSentence.trim().equals(question.correctAnswer?.trim(), ignoreCase = true)) {
            // ПРАВИЛЬНО
            incrementProgress()
            showTopNotice(NoticeType.CORRECT, "Правильно")
            goToNextQuestion()
        } else {
            handleMistake(question)
            if (UserManager.getHearts() > 0) {
                showTopNotice(NoticeType.WRONG, "Правильно: ${question.correctAnswer}")
                btnCheck.text = "ДАЛЕЕ"
                btnCheck.setOnClickListener {
                    btnCheck.text = "ПРОВЕРИТЬ"
                    goToNextQuestion()
                }
            } else {
                handleNoHearts()
            }
        }
    }

    // ================= ПАРЫ СЛОВ =================

    private fun setupMatchingMode(question: QuestionData) {
        rvPairs.visibility = View.VISIBLE
        matchesFoundCount = 0

        val rawList = question.options
        val items = mutableListOf<MatchItem>()
        totalPairsInQuestion = rawList.size / 2

        for (i in 0 until totalPairsInQuestion) {
            if (i * 2 + 1 < rawList.size) {
                val word1 = rawList[i * 2]
                val word2 = rawList[i * 2 + 1]
                items.add(MatchItem(i, word1))
                items.add(MatchItem(i, word2))
            }
        }
        items.shuffle()

        pairsAdapter = PairsAdapter(items) { selectedItem, position ->
            handlePairClick(selectedItem, position)
        }
        rvPairs.adapter = pairsAdapter
    }

    private var firstSelectedItem: MatchItem? = null
    private var firstSelectedPos: Int = -1

    private fun handlePairClick(item: MatchItem, position: Int) {
        if (item.isMatched) return

        if (firstSelectedPos == position) {
            item.isSelected = false
            pairsAdapter?.notifyItemChanged(position)
            firstSelectedItem = null
            firstSelectedPos = -1
            return
        }

        if (firstSelectedItem != null) {
            item.isSelected = true
            pairsAdapter?.notifyItemChanged(position)
            val firstItem = firstSelectedItem!!
            val firstPos = firstSelectedPos

            if (firstItem.id == item.id) {
                firstItem.isMatched = true
                item.isMatched = true
                Handler(Looper.getMainLooper()).postDelayed({
                    pairsAdapter?.notifyItemChanged(firstPos)
                    pairsAdapter?.notifyItemChanged(position)
                }, 300)
                matchesFoundCount++
                checkIfAllPairsFound()
            } else {
                mistakesCount++
                UserManager.loseHeart()
                updateHeartsUI()

                if (UserManager.getHearts() <= 0) {
                    handleNoHearts()
                } else {
                    showTopNotice(NoticeType.WRONG, "Не совпадает")
                    Handler(Looper.getMainLooper()).postDelayed({
                        firstItem.isSelected = false
                        item.isSelected = false
                        pairsAdapter?.notifyItemChanged(firstPos)
                        pairsAdapter?.notifyItemChanged(position)
                    }, 500)
                }
            }
            firstSelectedItem = null
            firstSelectedPos = -1
        } else {
            item.isSelected = true
            pairsAdapter?.notifyItemChanged(position)
            firstSelectedItem = item
            firstSelectedPos = position
        }
    }

    private fun checkIfAllPairsFound() {
        if (matchesFoundCount >= totalPairsInQuestion) {
            incrementProgress()

            val q = questionList[currentQuestionIndex]
            val raw = q.options
            for (i in raw.indices step 2) {
                if (i + 1 < raw.size) {
                    val a = raw[i]
                    val b = raw[i + 1]
                    val pair = when {
                        isLatin(a) && isCyrillic(b) -> a to b
                        isLatin(b) && isCyrillic(a) -> b to a
                        else -> null
                    }
                    pair?.let { (eng, rus) ->
                        if (eng.isNotBlank() && rus.isNotBlank()) learnedPairs += LearnedPair(eng, rus)
                    }
                }
            }
            showTopNotice(NoticeType.CORRECT, "Отлично!")
            Handler(Looper.getMainLooper()).postDelayed({ goToNextQuestion() }, 800)
        }
    }

    // ================= ОБЩАЯ ЛОГИКА =================

    private fun isLatin(s: String) = s.any { it in 'A'..'Z' || it in 'a'..'z' }
    private fun isCyrillic(s: String) = s.any { it in 'А'..'я' }

    private fun handleMistake(question: QuestionData) {
        mistakesCount++
        UserManager.loseHeart()
        updateHeartsUI()
        questionList.add(question)
    }

    private fun handleNoHearts() {
        showTopNotice(NoticeType.NO_HEARTS, "Сердца закончились")
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
    }

    private fun goToNextQuestion() {
        currentQuestionIndex++
        setQuestion()
    }

    private fun finishLevel() {
        // Убеждаемся, что прогресс полон
        progressBar.progress = initialQuestionCount

        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - startTime) / 1000

        val rawAccuracy = if (initialQuestionCount > 0) {
            100 - ((mistakesCount.toFloat() / initialQuestionCount.toFloat()) * 100).toInt()
        } else 100
        val accuracy = if (rawAccuracy < 0) 0 else rawAccuracy

        val uniquePairs = learnedPairs.distinctBy { it.front.lowercase() to it.back.lowercase() }
        val justWords = uniquePairs.map { it.front.lowercase() }.distinct()

        UserManager.init(this)
        UserManager.addLearnedWords(justWords)
        UserManager.setLevelPassed(currentLevelId)
        UserManager.addXP(50)

        lifecycleScope.launch {
            val repo = CardsRepository.getInstance(this@QuizActivity)
            repo.addLearnedPairsToCards(uniquePairs.map { it.front to it.back })
        }

        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("MISTAKES", mistakesCount)
        intent.putExtra("TIME_SECONDS", durationSeconds)
        intent.putExtra("ACCURACY", accuracy)
        startActivity(intent)
        finish()
    }

    private fun updateHeartsUI() {
        layoutHearts.removeAllViews()
        val hearts = UserManager.getHearts()
        repeat(UserManager.MAX_HEARTS) { index ->
            val img = ImageView(this)
            img.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { if (index > 0) marginStart = dp(4) }
            img.setImageResource(if (index < hearts) R.drawable.ic_heart_full else R.drawable.ic_heart_empty)
            layoutHearts.addView(img)
        }
    }

    private fun updateFlexboxContainers() {
        layoutAnswerArea.removeAllViews()
        layoutWordBank.removeAllViews()

        for (word in answerList) {
            val btn = createWordChip(word)
            btn.setOnClickListener {
                answerList.remove(word)
                wordBankList.add(word)
                updateFlexboxContainers()
            }
            layoutAnswerArea.addView(btn)
        }

        for (word in wordBankList) {
            val btn = createWordChip(word)
            btn.setOnClickListener {
                if (answerList.size < maxSentenceLength) {
                    wordBankList.remove(word)
                    answerList.add(word)
                    updateFlexboxContainers()
                } else {
                    showTopNotice(NoticeType.INFO, "Слов достаточно")
                }
            }
            layoutWordBank.addView(btn)
        }

        if (answerList.isNotEmpty()) {
            btnCheck.isEnabled = true
            btnCheck.alpha = 1.0f
        } else {
            btnCheck.isEnabled = false
            btnCheck.alpha = 0.5f
        }
    }

    private fun createWordChip(text: String): Button {
        val btn = Button(
            ContextThemeWrapper(
                this,
                com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton
            ), null, 0
        )
        btn.text = text
        btn.isAllCaps = false
        btn.textSize = 18f
        btn.setTextColor(Color.parseColor("#2C3E50"))
        btn.setBackgroundResource(R.drawable.bg_button_white)
        btn.setPadding(32, 20, 32, 20)
        val params = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16, 16, 16, 16)
        btn.layoutParams = params
        return btn
    }

    private fun showTopNotice(type: NoticeType, text: String) {
        val root = findViewById<View>(android.R.id.content)
        val bgColor = when (type) {
            NoticeType.CORRECT -> Color.parseColor("#2ECC71")
            NoticeType.WRONG, NoticeType.NO_HEARTS -> Color.parseColor("#E74C3C")
            NoticeType.INFO -> Color.parseColor("#F39C12")
            else -> Color.parseColor("#6C5CE7")
        }
        val snackbar = Snackbar.make(root, text, Snackbar.LENGTH_SHORT)
        val view = snackbar.view
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        val shape = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(bgColor)
        }
        view.background = shape
        val lp = view.layoutParams as? android.widget.FrameLayout.LayoutParams
            ?: view.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        lp?.let {
            if (it is android.widget.FrameLayout.LayoutParams) {
                it.gravity = Gravity.TOP
                it.topMargin = dp(56) + getStatusBarHeight()
                it.marginStart = dp(16)
                it.marginEnd = dp(16)
                view.layoutParams = it
            } else if (it is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                it.gravity = Gravity.TOP
                it.topMargin = dp(56) + getStatusBarHeight()
                it.marginStart = dp(16)
                it.marginEnd = dp(16)
                view.layoutParams = it
            }
        }
        snackbar.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun getStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else dp(24)
    }

    private fun extractWordFromQuestionText(text: String): String {
        val start = text.indexOf('\'')
        val end = text.lastIndexOf('\'')
        return if (start != -1 && end != -1 && end > start) text.substring(start + 1, end) else text
    }

    private fun extractHintInBrackets(text: String): String {
        val start = text.indexOf('(')
        val end = text.indexOf(')')
        return if (start != -1 && end != -1 && end > start) text.substring(start + 1, end) else ""
    }

    inner class PairsAdapter(
        private val items: List<MatchItem>,
        private val onItemClick: (MatchItem, Int) -> Unit
    ) : RecyclerView.Adapter<PairsAdapter.PairViewHolder>() {

        inner class PairViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.card_word)
            val text: TextView = view.findViewById(R.id.tv_word)
            fun bind(item: MatchItem, position: Int) {
                text.text = item.text
                if (item.isMatched) {
                    card.visibility = View.INVISIBLE
                    card.isClickable = false
                } else {
                    card.visibility = View.VISIBLE
                    card.isClickable = true
                    if (item.isSelected) {
                        card.setCardBackgroundColor(Color.parseColor("#D6EAF8"))
                        card.strokeColor = Color.parseColor("#3498DB")
                        card.strokeWidth = dp(2)
                        text.setTextColor(Color.parseColor("#3498DB"))
                    } else {
                        card.setCardBackgroundColor(Color.WHITE)
                        card.strokeColor = Color.parseColor("#E0E0E0")
                        card.strokeWidth = dp(1)
                        text.setTextColor(Color.parseColor("#2C3E50"))
                    }
                    itemView.setOnClickListener { onItemClick(item, position) }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PairViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pair_card, parent, false)
            return PairViewHolder(view)
        }
        override fun onBindViewHolder(holder: PairViewHolder, position: Int) {
            holder.bind(items[position], position)
        }
        override fun getItemCount(): Int = items.size
    }
}
