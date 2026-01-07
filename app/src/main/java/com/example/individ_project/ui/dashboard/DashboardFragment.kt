package com.example.individ_project.ui.dashboard

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.individ_project.R
import com.example.individ_project.cards.data.CardEntity
import com.example.individ_project.cards.data.CardSetEntity
import com.example.individ_project.cards.data.CardSetType
import com.example.individ_project.cards.data.CardsRepository
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    // --- Views ---
    private lateinit var layoutSets: View
    private lateinit var layoutStudy: View
    private lateinit var layoutCreate: View

    private lateinit var rvSets: RecyclerView
    private lateinit var btnCreate: ExtendedFloatingActionButton

    private lateinit var tvSetTitleStudy: TextView
    private lateinit var tvFrontBack: TextView
    private lateinit var cardWord: CardView
    private lateinit var btnKnown: Button
    private lateinit var btnNext: Button
    private lateinit var btnBackToSets: ImageButton
    private lateinit var btnEditSetStudy: ImageButton

    private lateinit var tvCreateTitle: TextView
    private lateinit var etSetName: TextInputEditText
    private lateinit var layoutPairsContainer: LinearLayout
    private lateinit var btnAddPair: Button
    private lateinit var btnSaveSet: Button
    private lateinit var btnCancelCreate: ImageButton
    private lateinit var scrollViewCreate: ScrollView

    // --- Логика ---
    private lateinit var adapter: SetsAdapter
    private lateinit var repo: CardsRepository

    private var currentCards: List<CardEntity> = emptyList()
    private var currentIndex = 0
    private var showingFront = true
    private var currentOpenedSet: CardSetEntity? = null
    private var editingSetId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        repo = CardsRepository.getInstance(requireContext())
        initViews(view)
        setupListeners()
        setupRecyclerView()
        loadData()
        return view
    }

    private fun initViews(view: View) {
        layoutSets = view.findViewById(R.id.layout_sets)
        layoutStudy = view.findViewById(R.id.layout_study)
        layoutCreate = view.findViewById(R.id.layout_create)

        rvSets = view.findViewById(R.id.rv_sets)
        btnCreate = view.findViewById(R.id.btn_create_set)

        tvSetTitleStudy = view.findViewById(R.id.tv_set_title)
        tvFrontBack = view.findViewById(R.id.tv_front_back)
        cardWord = view.findViewById(R.id.card_word)
        btnKnown = view.findViewById(R.id.btn_known)
        btnNext = view.findViewById(R.id.btn_next)
        btnBackToSets = view.findViewById(R.id.btn_back_to_sets)
        btnEditSetStudy = view.findViewById(R.id.btn_edit_set)

        tvCreateTitle = view.findViewById(R.id.tv_create_title)
        etSetName = view.findViewById(R.id.et_set_name)
        layoutPairsContainer = view.findViewById(R.id.layout_pairs_container)
        btnAddPair = view.findViewById(R.id.btn_add_pair)
        btnSaveSet = view.findViewById(R.id.btn_save_set)
        btnCancelCreate = view.findViewById(R.id.btn_cancel_create)
        scrollViewCreate = view.findViewById(R.id.scroll_view_create)
    }

    private fun setupListeners() {
        btnCreate.setOnClickListener { showCreateScreen(null) }

        btnBackToSets.setOnClickListener {
            hideKeyboard()
            layoutStudy.visibility = View.GONE
            layoutCreate.visibility = View.GONE
            layoutSets.visibility = View.VISIBLE
            loadData()
        }

        btnEditSetStudy.setOnClickListener {
            if (currentOpenedSet != null) {
                showCreateScreen(currentOpenedSet)
            }
        }

        btnKnown.setOnClickListener { showNextCard() }
        btnNext.setOnClickListener { showNextCard() }
        cardWord.setOnClickListener { toggleFrontBack() }

        btnAddPair.setOnClickListener { addPairRow() }
        btnSaveSet.setOnClickListener { saveNewSet() }
        btnCancelCreate.setOnClickListener {
            hideKeyboard()
            layoutCreate.visibility = View.GONE
            layoutSets.visibility = View.VISIBLE
            loadData()
        }
    }

    private fun setupRecyclerView() {
        rvSets.layoutManager = LinearLayoutManager(requireContext())

        if (rvSets.itemDecorationCount > 0) {
            rvSets.removeItemDecorationAt(0)
        }
        rvSets.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                if (parent.getChildAdapterPosition(view) != state.itemCount - 1) {
                    outRect.bottom = (16 * resources.displayMetrics.density).toInt()
                }
            }
        })

        adapter = SetsAdapter(
            onClick = { set -> openSet(set) },
            onDelete = { set -> deleteSet(set) },
            onEdit = { set -> showCreateScreen(set) }
        )
        rvSets.adapter = adapter
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repo.ensurePredefinedSets()
            val sets = repo.getAllSets()

            // СЧИТАЕМ КОЛИЧЕСТВО СЛОВ ДЛЯ КАЖДОГО НАБОРА
            val setsWithCounts = sets.map { set ->
                val count = repo.getCardsBySet(set.id).size
                set to count // Создаем пару (Set, Count)
            }

            adapter.submit(setsWithCounts)
        }
    }

    // --- Логика Редактора ---

    private fun addPairRow(frontText: String = "", backText: String = "") {
        val row = layoutInflater.inflate(R.layout.item_card_pair_edit, layoutPairsContainer, false)
        val etFront = row.findViewById<EditText>(R.id.et_front)
        val etBack = row.findViewById<EditText>(R.id.et_back)

        etFront.setText(frontText)
        etBack.setText(backText)

        layoutPairsContainer.addView(row)
        scrollViewCreate.post { scrollViewCreate.fullScroll(View.FOCUS_DOWN) }
    }

    private fun showCreateScreen(set: CardSetEntity?) {
        editingSetId = set?.id
        layoutPairsContainer.removeAllViews()

        if (set != null) {
            tvCreateTitle.text = "Редактирование набора"
            etSetName.setText(set.name)
            viewLifecycleOwner.lifecycleScope.launch {
                val cards = repo.getCardsBySet(set.id)
                for (card in cards) {
                    addPairRow(card.frontText, card.backText)
                }
                if (cards.isEmpty()) addPairRow()
            }
        } else {
            tvCreateTitle.text = "Новый набор"
            etSetName.setText("")
            repeat(3) { addPairRow() }
        }

        layoutSets.visibility = View.GONE
        layoutStudy.visibility = View.GONE
        layoutCreate.visibility = View.VISIBLE
    }

    private fun saveNewSet() {
        val name = etSetName.text.toString().trim()
        if (name.isEmpty()) return

        val pairs = mutableListOf<Pair<String, String>>()
        for (i in 0 until layoutPairsContainer.childCount) {
            val row = layoutPairsContainer.getChildAt(i)
            val etFront = row.findViewById<EditText>(R.id.et_front)
            val etBack = row.findViewById<EditText>(R.id.et_back)

            val front = etFront.text.toString().trim()
            val back = etBack.text.toString().trim()

            if (front.isNotEmpty() && back.isNotEmpty()) {
                pairs += front to back
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (editingSetId != null) {
                repo.deleteSet(editingSetId!!)
                repo.createCustomSet(name, pairs)
            } else {
                repo.createCustomSet(name, pairs)
            }
            hideKeyboard()
            layoutCreate.visibility = View.GONE
            layoutSets.visibility = View.VISIBLE
            loadData()
        }
    }

    private fun deleteSet(set: CardSetEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление")
            .setMessage("Удалить набор \"${set.name}\"?")
            .setPositiveButton("Да") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.deleteSet(set.id)
                    loadData()
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    // --- Логика Изучения ---

    private fun openSet(set: CardSetEntity) {
        currentOpenedSet = set
        hideKeyboard()
        viewLifecycleOwner.lifecycleScope.launch {
            currentCards = repo.getCardsBySet(set.id)
            if (currentCards.isEmpty()) {
                if (set.type == CardSetType.CUSTOM) {
                    showCreateScreen(set)
                }
                return@launch
            }
            currentIndex = 0
            showingFront = true
            tvSetTitleStudy.text = set.name
            btnEditSetStudy.visibility = if (set.type == CardSetType.CUSTOM) View.VISIBLE else View.INVISIBLE

            layoutSets.visibility = View.GONE
            layoutCreate.visibility = View.GONE
            layoutStudy.visibility = View.VISIBLE
            showCurrentCard()
        }
    }

    private fun showCurrentCard() {
        if (currentCards.isEmpty()) return
        val card = currentCards[currentIndex]
        tvFrontBack.text = if (showingFront) card.frontText else card.backText
        tvFrontBack.alpha = if (showingFront) 1.0f else 0.9f
    }

    private fun toggleFrontBack() {
        if (currentCards.isEmpty()) return
        cardWord.animate()
            .rotationY(90f)
            .setDuration(150)
            .withEndAction {
                showingFront = !showingFront
                showCurrentCard()
                cardWord.rotationY = -90f
                cardWord.animate().rotationY(0f).setDuration(150).start()
            }
            .start()
    }

    private fun showNextCard() {
        if (currentCards.isEmpty()) return
        cardWord.animate()
            .translationX(-1000f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                currentIndex = (currentIndex + 1) % currentCards.size
                showingFront = true
                showCurrentCard()
                cardWord.translationX = 1000f
                cardWord.animate().translationX(0f).alpha(1f).setDuration(300).start()
            }
            .start()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = requireActivity().currentFocus
        if (v != null) imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    // --- ОБНОВЛЕННЫЙ АДАПТЕР (Принимает Pair<CardSetEntity, Int>) ---
    class SetsAdapter(
        private val onClick: (CardSetEntity) -> Unit,
        private val onDelete: (CardSetEntity) -> Unit,
        private val onEdit: (CardSetEntity) -> Unit
    ) : RecyclerView.Adapter<SetsAdapter.SetVH>() {

        // ТЕПЕРЬ СПИСОК ПАР (Набор, КоличествоСлов)
        private val items = mutableListOf<Pair<CardSetEntity, Int>>()

        fun submit(list: List<Pair<CardSetEntity, Int>>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        inner class SetVH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tv_set_name)
            private val tvTag: TextView = view.findViewById(R.id.tv_set_tag)
            private val tvSubtitle: TextView = view.findViewById(R.id.tv_set_subtitle)
            private val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_set)
            private val btnEdit: ImageButton = view.findViewById(R.id.btn_edit_set)

            fun bind(data: Pair<CardSetEntity, Int>) {
                val item = data.first
                val count = data.second

                tvName.text = item.name
                tvTag.text = item.levelTag ?: "ALL"

                // ПОКАЗЫВАЕМ РЕАЛЬНОЕ КОЛИЧЕСТВО СЛОВ
                tvSubtitle.text = "Слов: $count"

                if (item.type == CardSetType.CUSTOM) {
                    btnDelete.visibility = View.VISIBLE
                    btnEdit.visibility = View.VISIBLE
                    btnDelete.setOnClickListener { onDelete(item) }
                    btnEdit.setOnClickListener { onEdit(item) }
                } else {
                    btnDelete.visibility = View.GONE
                    btnEdit.visibility = View.GONE
                }
                itemView.setOnClickListener { onClick(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_card_set, parent, false)
            return SetVH(v)
        }

        override fun onBindViewHolder(holder: SetVH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}
