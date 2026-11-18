package com.mokhalad.tvtheme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class ColorWheelView(context: Context) : View(context) {
    private var _hue = 0f
    private var _sat = 1f
    private var _val = 1f

    var onChange: (() -> Unit)? = null

    var hue: Float
        get() = _hue
        set(v) { _hue = ((v % 360f) + 360f) % 360f; invalidate(); onChange?.invoke() }

    var saturation: Float
        get() = _sat
        set(v) { _sat = v.coerceIn(0f, 1f); invalidate(); onChange?.invoke() }

    var value: Float
        get() = _val
        set(v) { _val = v.coerceIn(0f, 1f); invalidate(); onChange?.invoke() }

    fun pickedColor(): Int = Color.HSVToColor(floatArrayOf(_hue, _sat, _val))

    private val hueArr = IntArray(361) { Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f)) }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 6f; color = Color.BLACK
    }
    private val cursorInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.WHITE
    }

    private var cx = 0f; private var cy = 0f
    private var outerR = 0f; private var innerR = 0f; private var ringR = 0f
    private val ringRect = RectF()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        cx = w / 2f; cy = h / 2f
        outerR = minOf(w, h) / 2f - 4f
        innerR = outerR * 0.72f
        ringR = (outerR + innerR) / 2f
        ringRect.set(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
    }

    override fun onDraw(canvas: Canvas) {
        // رسم حلقة الألوان (Hue)
        ringPaint.strokeWidth = outerR - innerR
        ringPaint.shader = SweepGradient(cx, cy, hueArr, null)
        canvas.drawArc(ringRect, 0f, 360f, false, ringPaint)

        // رسم القرص الداخلي (Saturation/Value)
        val fullCol = Color.HSVToColor(floatArrayOf(_hue, 1f, _val))
        val centerCol = Color.HSVToColor(floatArrayOf(_hue, 0f, _val))
        satPaint.shader = RadialGradient(cx, cy, innerR, intArrayOf(centerCol, fullCol), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, innerR, satPaint)

        // رسم المؤشر
        val a = Math.toRadians(_hue.toDouble())
        val r = _sat * innerR
        val px = cx + (r * cos(a)).toFloat()
        val py = cy + (r * sin(a)).toFloat()
        canvas.drawCircle(px, py, 12f, cursorOuter)
        canvas.drawCircle(px, py, 12f, cursorInner)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // تحريك المؤشر بالأسهم
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { hue = _hue - 5f; return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { hue = _hue + 5f; return true }
            KeyEvent.KEYCODE_DPAD_UP -> if (_sat < 0.995f) { saturation = _sat + 0.05f; return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (_sat > 0.005f) { saturation = _sat - 0.05f; return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
            val dx = e.x - cx; val dy = e.y - cy
            val r = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (r <= outerR) {
                val ang = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                hue = ang
                saturation = if (r <= innerR) r / innerR else 1f
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}