package com.example.individ_project.ui.home

import android.graphics.*
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.individ_project.R
import kotlin.math.sqrt

class LevelPathDecoration(
    private val adapter: LevelsAdapter
) : RecyclerView.ItemDecoration() {

    private val purple = Color.parseColor("#6C5CE7")
    private val gray = Color.parseColor("#B2BEC3")

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(26f, 18f), 0f)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val children = (0 until parent.childCount)
            .map { parent.getChildAt(it) }
            .sortedBy { parent.getChildAdapterPosition(it) }

        // идём по всем соседним видимым элементам
        for (i in 0 until children.size - 1) {
            val v1 = children[i]
            val v2 = children[i + 1]

            val pos1 = parent.getChildAdapterPosition(v1)
            val pos2 = parent.getChildAdapterPosition(v2)
            if (pos1 == RecyclerView.NO_POSITION || pos2 == RecyclerView.NO_POSITION) continue

            // если хоть один из элементов — заголовок акта, между ними дорожку не рисуем
            val item1 = adapterItemAt(pos1) ?: continue
            val item2 = adapterItemAt(pos2) ?: continue
            if (item1 is HomeItem.ActHeader || item2 is HomeItem.ActHeader) continue

            val b1 = v1.findViewById<View>(R.id.btn_level)
            val b2 = v2.findViewById<View>(R.id.btn_level)
            if (b1 == null || b2 == null) continue

            val c1x = v1.left + b1.x + b1.width / 2f
            val c1y = v1.top + b1.y + b1.height / 2f

            val c2x = v2.left + b2.x + b2.width / 2f
            val c2y = v2.top + b2.y + b2.height / 2f

            paint.color = if (!adapter.isLockedAt(pos1)) purple else gray

            val dx = c2x - c1x
            val dy = c2y - c1y
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

            val ux = dx / len
            val uy = dy / len

            val nx = -uy
            val ny = ux

            val sign = if (pos1 % 2 == 0) 1f else -1f

            val midX = (c1x + c2x) / 2f
            val midY = (c1y + c2y) / 2f
            val amp = dp(parent, 180).coerceAtMost(len * 0.95f) * sign

            val ctrlX = midX + nx * amp
            val ctrlY = midY + ny * amp

            val r1 = b1.width / 2f
            val r2 = b2.width / 2f
            val gap = dp(parent, 6)

            val (sx, sy) = pointOnCircleToward(c1x, c1y, r1 + gap, ctrlX, ctrlY)
            val (ex, ey) = pointOnCircleToward(c2x, c2y, r2 + gap, ctrlX, ctrlY)

            val path = Path().apply {
                moveTo(sx, sy)
                quadTo(ctrlX, ctrlY, ex, ey)
            }

            c.drawPath(path, paint)
        }
    }

    // безопасно достаём элемент адаптера по позиции
    private fun adapterItemAt(position: Int): HomeItem? =
        adapter.run {
            if (position in 0 until itemCount) items[position] else null
        }

    private fun pointOnCircleToward(
        cx: Float,
        cy: Float,
        radius: Float,
        tx: Float,
        ty: Float
    ): Pair<Float, Float> {
        val vx = tx - cx
        val vy = ty - cy
        val vlen = sqrt(vx * vx + vy * vy).coerceAtLeast(1f)
        val ux = vx / vlen
        val uy = vy / vlen
        return Pair(cx + ux * radius, cy + uy * radius)
    }

    private fun dp(parent: RecyclerView, v: Int): Float {
        return v * parent.resources.displayMetrics.density
    }
}
