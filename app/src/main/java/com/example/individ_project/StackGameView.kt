package com.example.individ_project

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class StackGameView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // --- Параметры ---
    private val blockHeight = 40f
    private val blockDepthHeight = 50f
    private val startSize = 300f

    // --- Состояние ---
    data class BlockData(val x: Float, val z: Float, val w: Float, val d: Float, val color: Int)
    private val stackBlocks = mutableListOf<BlockData>()

    private var currentWidth = startSize
    private var currentDepth = startSize

    // Движение
    private var movingPos = 0f
    private var movingDirection = 1
    private var isMovingX = false

    // Скорость: начинаем медленно, разгоняемся плавно
    private val initialSpeed = 8f
    private var moveSpeed = initialSpeed

    // Игра
    var score = 0
    var isGameOver = false
    var onScoreUpdate: ((Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    // Графика
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Затемнение
    private var dimAlpha = 0 // 0..150 для затемнения
    private val dimPaint = Paint().apply { color = Color.BLACK }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            // Если игра идет - двигаем блоки
            if (!isGameOver) {
                updatePosition()
            } else {
                // Если проиграли - плавно затемняем экран
                if (dimAlpha < 180) {
                    dimAlpha += 5
                }
            }
            invalidate()
        }
    }

    init {
        resetGame()
    }

    fun resetGame() {
        stackBlocks.clear()
        score = 0
        currentWidth = startSize
        currentDepth = startSize
        isGameOver = false
        moveSpeed = initialSpeed
        dimAlpha = 0
        isMovingX = false

        post {
            // Фундамент
            stackBlocks.add(
                BlockData(0f, 0f, startSize, startSize, getGradientColor(0))
            )
            spawnNextBlock()
            if (!animator.isStarted) animator.start()
            onScoreUpdate?.invoke(0)
        }
    }

    // Генерация красивого градиента (Sine rainbow)
    private fun getGradientColor(index: Int): Int {
        val frequency = 0.1f // Плавность перехода (меньше = плавнее)
        val r = (sin(frequency * index + 0) * 127 + 128).toInt()
        val g = (sin(frequency * index + 2) * 127 + 128).toInt()
        val b = (sin(frequency * index + 4) * 127 + 128).toInt()
        return Color.rgb(r, g, b)
    }

    private fun spawnNextBlock() {
        isMovingX = !isMovingX
        movingDirection = 1
        val offset = 450f // Чуть дальше вылет
        movingPos = if (movingDirection == 1) -offset else offset

        // Очень плавное ускорение
        moveSpeed += 0.2f
    }

    private fun updatePosition() {
        movingPos += moveSpeed * movingDirection
        val limit = 550f
        if (movingPos > limit && movingDirection == 1) movingDirection = -1
        else if (movingPos < -limit && movingDirection == -1) movingDirection = 1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height * 0.6f

        // Камера
        val visibleLimit = 8
        // Плавный сдвиг камеры
        val targetOffsetY = if (score > visibleLimit) (score - visibleLimit) * blockHeight else 0f
        // Можно добавить лерп для камеры, но пока оставим жесткую привязку для стабильности

        canvas.save()
        canvas.translate(cx, cy + targetOffsetY)

        // 1. Рисуем башню
        for ((i, block) in stackBlocks.withIndex()) {
            val screenY = -i * blockHeight
            drawIsometricBlock(canvas, block.x, block.z, block.w, block.d, screenY, block.color)
        }

        // 2. Рисуем текущий
        if (!isGameOver && stackBlocks.isNotEmpty()) {
            val last = stackBlocks.last()
            val currX = if (isMovingX) movingPos else last.x
            val currZ = if (isMovingX) last.z else movingPos
            val screenY = -stackBlocks.size * blockHeight

            // Цвет следующего блока
            val nextColor = getGradientColor(score + 1)

            drawIsometricBlock(canvas, currX, currZ, currentWidth, currentDepth, screenY, nextColor)
        }

        canvas.restore()

        // 3. Эффект затемнения при проигрыше (рисуем поверх всего)
        if (isGameOver && dimAlpha > 0) {
            dimPaint.alpha = dimAlpha
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        }
    }

    private fun drawIsometricBlock(canvas: Canvas, x: Float, z: Float, w: Float, d: Float, yOffset: Float, color: Int) {
        val isoScaleX = 1.0f
        val isoScaleY = 0.5f

        fun toScreen(lx: Float, lz: Float): Pair<Float, Float> {
            val sx = (lx - lz) * isoScaleX
            val sy = (lx + lz) * isoScaleY + yOffset
            return sx to sy
        }

        val p1 = toScreen(x, z)
        val p2 = toScreen(x + w, z)
        val p3 = toScreen(x + w, z + d)
        val p4 = toScreen(x, z + d)

        val pathTop = Path().apply {
            moveTo(p1.first, p1.second)
            lineTo(p2.first, p2.second)
            lineTo(p3.first, p3.second)
            lineTo(p4.first, p4.second)
            close()
        }
        paint.color = color
        canvas.drawPath(pathTop, paint)

        val pathRight = Path().apply {
            moveTo(p2.first, p2.second)
            lineTo(p3.first, p3.second)
            lineTo(p3.first, p3.second + blockDepthHeight)
            lineTo(p2.first, p2.second + blockDepthHeight)
            close()
        }
        paint.color = manipulateColor(color, 0.85f)
        canvas.drawPath(pathRight, paint)

        val pathLeft = Path().apply {
            moveTo(p3.first, p3.second)
            lineTo(p4.first, p4.second)
            lineTo(p4.first, p4.second + blockDepthHeight)
            lineTo(p3.first, p3.second + blockDepthHeight)
            close()
        }
        paint.color = manipulateColor(color, 0.7f)
        canvas.drawPath(pathLeft, paint)
    }

    private fun manipulateColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (isGameOver) return true
            placeBlock()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun placeBlock() {
        if (stackBlocks.isEmpty()) return

        val last = stackBlocks.last()
        var currX = if (isMovingX) movingPos else last.x
        var currZ = if (isMovingX) last.z else movingPos
        var currW = currentWidth
        var currD = currentDepth

        val delta = if (isMovingX) currX - last.x else currZ - last.z
        val absDelta = abs(delta)
        val maxDim = if (isMovingX) last.w else last.d

        // Допуск (погрешность), чтобы легче было попасть идеально
        val tolerance = 5f

        if (absDelta <= tolerance) {
            // Идеальное попадание!
            currX = last.x
            currZ = last.z
            // Бонус? (можно добавить эффекты)
        } else if (absDelta >= maxDim) {
            gameOver()
            return
        } else {
            // Обрезка
            if (isMovingX) {
                currW -= absDelta
                if (delta > 0) currX = last.x + delta else currX = movingPos // Исправленная логика
                // Чтобы не путаться, используем надежную логику пересечения:
                val myLeft = if(isMovingX) movingPos else last.x
                val newLeft = max(myLeft, last.x)
                val newRight = min(myLeft + currentWidth, last.x + last.w)
                currX = newLeft
                currW = newRight - newLeft
            } else {
                val myBack = movingPos
                val newBack = max(myBack, last.z)
                val newFront = min(myBack + currentDepth, last.z + last.d)
                currZ = newBack
                currD = newFront - newBack
            }
        }

        currentWidth = currW
        currentDepth = currD

        if (currentWidth < 1f || currentDepth < 1f) {
            gameOver()
            return
        }

        val nextColor = getGradientColor(score + 1)
        stackBlocks.add(BlockData(currX, currZ, currentWidth, currentDepth, nextColor))

        score++
        onScoreUpdate?.invoke(score)
        spawnNextBlock()
        invalidate()
    }

    private fun gameOver() {
        isGameOver = true
        onGameOver?.invoke()
    }
}
