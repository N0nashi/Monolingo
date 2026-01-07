package com.example.individ_project.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.individ_project.R
import android.widget.ImageButton

class LevelsAdapter(
    val items: List<HomeItem>,
    private val onLevelClick: (Level) -> Unit,
    private val onActContinueClick: (Int) -> Unit = {}   // actIndex
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_LEVEL = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is HomeItem.ActHeader -> TYPE_HEADER
            is HomeItem.LevelItem -> TYPE_LEVEL
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val v = inflater.inflate(R.layout.item_act_header, parent, false)
            ActHeaderVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_level, parent, false)
            LevelVH(v)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HomeItem.ActHeader ->
                (holder as ActHeaderVH).bind(item, onActContinueClick)
            is HomeItem.LevelItem ->
                (holder as LevelVH).bind(item.level, position, items, onLevelClick)
        }
    }

    // -------- Заголовок акта --------
    class ActHeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tv_act_title)
        private val subtitle = view.findViewById<TextView>(R.id.tv_act_subtitle)
        private val btnContinue = view.findViewById<ImageButton>(R.id.btn_act_continue)

        fun bind(item: HomeItem.ActHeader, onContinue: (Int) -> Unit) {
            title.text = "ACT ${item.actIndex}"
            subtitle.text = "Unit ${item.actIndex}"

            btnContinue.setOnClickListener {
                onContinue(item.actIndex)
            }
        }
    }

    // -------- Кружок уровня --------
    class LevelVH(view: View) : RecyclerView.ViewHolder(view) {

        private val btnLevel = view.findViewById<Button>(R.id.btn_level)

        fun bind(
            level: Level,
            adapterPos: Int,
            allItems: List<HomeItem>,
            onClick: (Level) -> Unit
        ) {
            btnLevel.text = level.id.toString()

            // фон: фиолетовый/серый
            if (level.isLocked) {
                btnLevel.setBackgroundResource(R.drawable.bg_circle_button_gray)
            } else {
                btnLevel.setBackgroundResource(R.drawable.bg_circle_button_purple)
            }

            btnLevel.isEnabled = !level.isLocked
            btnLevel.alpha = if (level.isLocked) 0.6f else 1f

            // ---- зигзаг только по уровням ----
            var levelIndex = 0
            for (i in 0..adapterPos) {
                if (allItems[i] is HomeItem.LevelItem) levelIndex++
            }
            val zeroBased = levelIndex - 1

            val lp = itemView.layoutParams as ViewGroup.MarginLayoutParams
            val offset = (itemView.resources.displayMetrics.density * 80).toInt()

            if (zeroBased % 2 == 0) {
                lp.marginStart = offset
                lp.marginEnd = 0
            } else {
                lp.marginStart = 0
                lp.marginEnd = offset
            }
            itemView.layoutParams = lp
            // -----------------------------------

            btnLevel.setOnClickListener {
                if (!level.isLocked) onClick(level)
            }
            itemView.setOnClickListener(null)
        }
    }

    fun isLockedAt(position: Int): Boolean {
        val item = items[position]
        return item is HomeItem.LevelItem && item.level.isLocked
    }
}
