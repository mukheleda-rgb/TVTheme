package com.mokhalad.tvtheme

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar

/** مظهر النيون المستخدم في قائمة الإعدادات (نفس ألوان وتأثيرات لانشر النيون). */
object NeonStyle {
    val BG = 0xFF080810.toInt()
    val PANEL = 0x14FFFFFF
    val IDLE = 0x22FFFFFF
    val SUB = 0xB3FFFFFF.toInt()
    val DIALOG = 0xF0101018.toInt()

    private fun dp(v: Int) = (v * Resources.getSystem().displayMetrics.density).toInt()
    fun withAlpha(c: Int, a: Float) = (c and 0x00FFFFFF) or ((a * 255).toInt() shl 24)

    /** لون النيون الحالي (0 = بنفسجي غامق ... 1 = أخضر فسفوري) */
    fun neon(c: Context): Int = Themes.neonColor(Prefs.s(c, "neonHue", "0.4").toFloatOrNull() ?: 0.4f)

    /** لوحة ألوان خاصة بقائمة الإعدادات: خلفية داكنة، نص أبيض، لون مميز = النيون */
    fun pal(c: Context): Pal {
        val n = neon(c)
        return Pal(BG, 0xCC080810.toInt(), IDLE, n, Color.WHITE, SUB, n)
    }

    fun gravity(c: Context): Int =
        (if (Prefs.s(c, "lang", "ar") == "en") Gravity.LEFT else Gravity.RIGHT) or Gravity.CENTER_VERTICAL

    /** state: 0 عادي، 1 محدد (القسم الحالي)، 2 عليه المؤشر */
    fun bg(neon: Int, r: Float, state: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = r
        when (state) {
            2 -> { setColor(withAlpha(neon, .45f)); setStroke(dp(2), neon) }
            1 -> { setColor(withAlpha(neon, .25f)); setStroke(dp(2), neon) }
            else -> setColor(IDLE)
        }
    }

    /** تأثير التنقل: إطار نيون + تعبئة + تكبير خفيف + توهج */
    fun look(v: View, neon: Int, r: Float, f: Boolean, selected: Boolean = false) {
        v.background = bg(neon, r, if (f) 2 else if (selected) 1 else 0)
        val s = if (f) 1.04f else 1f
        v.animate().scaleX(s).scaleY(s).translationZ(if (f) 8f else 0f).setDuration(140).start()
        if (Build.VERSION.SDK_INT >= 28) { v.outlineSpotShadowColor = neon; v.outlineAmbientShadowColor = neon }
        v.elevation = if (f) dp(10).toFloat() else 0f
    }

    fun tintSeek(sb: SeekBar, neon: Int) {
        val cs = ColorStateList.valueOf(neon)
        sb.progressTintList = cs
        sb.thumbTintList = cs
    }

    fun styleDialog(d: AlertDialog, neon: Int) {
        d.window?.setBackgroundDrawable(GradientDrawable().apply {
            setColor(DIALOG); cornerRadius = dp(16).toFloat(); setStroke(dp(1), withAlpha(neon, .6f))
        })
        d.listView?.apply { divider = null; selector = bg(neon, dp(10).toFloat(), 2) }
        d.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(neon)
        d.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(neon)
    }
}

/** شريط اختيار لون النيون (يتحرك بسهم اليمين/اليسار) */
class HueBar(c: Context, var t: Float, private val onChange: (Float) -> Unit) : View(c) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init { isFocusable = true }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val r = h / 2
        paint.shader = LinearGradient(0f, 0f, w, 0f, IntArray(8) { Themes.neonColor(it / 7f) }, null, Shader.TileMode.CLAMP)
        rect.set(0f, 0f, w, h)
        cv.drawRoundRect(rect, r, r, paint)
        paint.shader = null
        val x = r + (w - 2 * r) * t
        paint.color = Color.WHITE
        cv.drawCircle(x, h / 2, r, paint)
        paint.color = Themes.neonColor(t)
        cv.drawCircle(x, h / 2, r * 0.6f, paint)
    }

    override fun onKeyDown(k: Int, e: KeyEvent?): Boolean {
        val d = when (k) { KeyEvent.KEYCODE_DPAD_LEFT -> -1; KeyEvent.KEYCODE_DPAD_RIGHT -> 1; else -> 0 }
        if (d == 0) return super.onKeyDown(k, e)
        t = (t + d * 0.05f).coerceIn(0f, 1f)
        invalidate()
        onChange(t)
        return true
    }
}
