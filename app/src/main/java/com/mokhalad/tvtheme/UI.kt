package com.mokhalad.tvtheme

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import java.util.Calendar

class Pal(val bg: Int, val scrim: Int, val fill: Int, val stroke: Int, val text: Int, val sub: Int, val accent: Int)

object Themes {
    private fun k(s: String) = Color.parseColor(s)

    fun key(c: Context): String {
        if (Prefs.b(c, "autoTheme", false)) {
            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val day = h >= Prefs.s(c, "dayStart", "6").toInt() && h < Prefs.s(c, "dayEnd", "18").toInt()
            return if (day) Prefs.s(c, "themeDay", "light") else Prefs.s(c, "themeNight", "dark")
        }
        return Prefs.s(c, "theme", "glass")
    }

    private fun base(c: Context): Pal = when (key(c)) {
        "neon" -> Pal(k("#070B18"), k("#CC050817"), k("#3310E8FF"), k("#AA10E8FF"), k("#F4FBFF"), k("#A5B9D0"), k("#00E5FF"))
        "light" -> Pal(k("#EFF2F6"), k("#66FFFFFF"), k("#88FFFFFF"), k("#CCFFFFFF"), k("#1B1F24"), k("#4A5560"), k("#2B7FFF"))
        "dark" -> Pal(k("#101010"), k("#BB000000"), k("#22FFFFFF"), k("#44FFFFFF"), k("#FFFFFF"), k("#B0B0B0"), k("#FFFFFF"))
        else -> Pal(k("#14222A"), k("#99101A1F"), k("#2AFFFFFF"), k("#55B8D4E3"), k("#EAF2F6"), k("#A9BCC6"), k("#FF9D2E"))
    }

    // اللون المميز (accent) يتبع لون النيون المختار في الإعدادات؛ وثيم "نيون" يتبعه بالكامل
    fun get(c: Context): Pal {
        val k = key(c); val b = base(c); val n = NeonStyle.neon(c)
        return if (k == "neon") Pal(b.bg, b.scrim, NeonStyle.withAlpha(n, .2f), NeonStyle.withAlpha(n, .67f), b.text, b.sub, n)
        else Pal(b.bg, b.scrim, b.fill, b.stroke, b.text, b.sub, n)
    }

    // ✅ دالة ألوان النيون المستوحاة من ملفات Compose
    fun neonColor(t: Float): Int {
        val x = t.coerceIn(0f, 1f)
        return Color.HSVToColor(floatArrayOf(275f - 145f * x, 1f, 0.8f + 0.2f * x))
    }
}

object UI {
    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()
    private var reg: Typeface? = null
    private var bold: Typeface? = null
    
    private fun load(c: Context, n: String) = try { Typeface.createFromAsset(c.assets, "fonts/$n") } catch (e: Exception) { null }
    
    fun font(c: Context, b: Boolean): Typeface {
        if (reg == null) {
            reg = load(c, "Dubai-Regular.ttf") ?: Typeface.DEFAULT
            bold = load(c, "Dubai-Bold.ttf") ?: Typeface.DEFAULT_BOLD
        }
        return if (b) bold!! else reg!!
    }
    
    fun tv(c: Context, t: String, sp: Float, col: Int, b: Boolean = false) = TextView(c).apply {
        text = t; textSize = sp; setTextColor(col); typeface = font(c, b)
    }
    
    fun glass(p: Pal, r: Float, focused: Boolean = false) = GradientDrawable().apply {
        cornerRadius = r
        setColor(if (focused) (p.accent and 0x00FFFFFF) or 0x33000000 else p.fill)
        val d = android.content.res.Resources.getSystem().displayMetrics.density
        setStroke(((if (focused) 2.5f else 1f) * d).toInt().coerceAtLeast(1), if (focused) p.accent else p.stroke)
    }

    // ✅ خلفية التدرج بناءً على لون التطبيق (مستوحاة من Compose)
    fun gradientBackground(color: Int, alpha: Float = 0.55f): GradientDrawable {
        val c1 = Color.BLACK
        val c2 = (color and 0x00FFFFFF) or (((alpha * 255).toInt() and 0xFF) shl 24)
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2, color))
    }
}