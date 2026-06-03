package com.example.spendwise

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GeometricBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shapes = mutableListOf<GeoShape>()
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val neutralShades = listOf(
        Color.parseColor("#E8EAF6"), // indigo light
        Color.parseColor("#E3F2FD"), // blue light
        Color.parseColor("#F5F6FA"), // surface
        Color.parseColor("#EDE9FE")  // purple light
    )

    private val strokeColor = Color.parseColor("#3949AB") // brand indigo

    data class GeoShape(
        var x: Float,
        var y: Float,
        var radius: Float,
        val sides: Int,
        var rotation: Float,
        val rotSpeed: Float,
        val vx: Float,
        val vy: Float,
        val fillColor: Int?,
        val isStroke: Boolean,
        val alpha: Float
    )

    private val frameRunnable = object : Runnable {
        override fun run() {
            updateShapes()
            invalidate()
            if (isRunning) handler.postDelayed(this, 16)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        if (w > 0 && h > 0) initShapes(w, h)
    }

    private fun initShapes(w: Int, h: Int) {
        shapes.clear()
        val sideOptions = intArrayOf(3, 4, 5, 6)
        repeat(28) { i ->
            val sides = sideOptions[Random.nextInt(sideOptions.size)]
            val isStroke = i % 5 == 0
            val fillColor: Int? = if (isStroke) null else neutralShades[Random.nextInt(neutralShades.size)]
            
            shapes.add(
                GeoShape(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    radius = 14f + Random.nextFloat() * 52f,
                    sides = sides,
                    rotation = Random.nextFloat() * (Math.PI * 2).toFloat(),
                    rotSpeed = (Random.nextFloat() - 0.5f) * 0.006f,
                    vx = (Random.nextFloat() - 0.5f) * 0.9f,
                    vy = (Random.nextFloat() - 0.5f) * 1.3f,
                    fillColor = fillColor,
                    isStroke = isStroke,
                    alpha = 0.25f + Random.nextFloat() * 0.35f
                )
            )
        }
    }

    private fun updateShapes() {
        val w = width.toFloat()
        val h = height.toFloat()
        for (i in shapes.indices) {
            val s = shapes[i]
            s.x += s.vx
            s.y += s.vy
            s.rotation += s.rotSpeed
            
            if (s.x < -s.radius) s.x = w + s.radius
            if (s.x > w + s.radius) s.x = -s.radius
            if (s.y < -s.radius) s.y = h + s.radius
            if (s.y > h + s.radius) s.y = -s.radius
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        shapes.forEach { s ->
            paint.reset()
            paint.isAntiAlias = true
            val path = buildPolygonPath(s.x, s.y, s.radius, s.sides, s.rotation)
            if (s.isStroke) {
                paint.style = Paint.Style.STROKE
                paint.color = strokeColor
                paint.strokeWidth = 1.0f
                paint.alpha = (s.alpha * 0.4f * 255).toInt()
            } else {
                paint.style = Paint.Style.FILL
                paint.color = s.fillColor ?: Color.TRANSPARENT
                paint.alpha = (s.alpha * 0.4f * 255).toInt()
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun buildPolygonPath(cx: Float, cy: Float, r: Float, sides: Int, rot: Float): Path {
        val path = Path()
        for (i in 0 until sides) {
            val angle = rot + i * (2.0 * Math.PI / sides)
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }

    fun startAnimation() {
        isRunning = true
        handler.post(frameRunnable)
    }

    fun stopAnimation() {
        isRunning = false
        handler.removeCallbacks(frameRunnable)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
