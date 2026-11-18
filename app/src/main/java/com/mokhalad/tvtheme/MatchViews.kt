package com.mokhalad.tvtheme

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object Logos {
    private val cache = object : LruCache<String, Bitmap>(80) {}

    fun load(url: String, iv: ImageView) {
        iv.tag = url
        if (url.isEmpty()) { iv.setImageDrawable(null); return }
        val hit = cache.get(url)
        if (hit != null) { iv.setImageBitmap(hit); return }
        iv.setImageDrawable(null)
        Net.async({ Net.bitmap(url) }) { b ->
            if (b != null) { cache.put(url, b); if (iv.tag == url) iv.setImageBitmap(b) }
        }
    }
}

object MatchViews {
    private fun dp(c: Context, v: Int) = UI.dp(c, v)

    private fun logoView(c: Context, url: String, size: Int, mStart: Int, mEnd: Int): ImageView {
        val iv = ImageView(c).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        Logos.load(url, iv)
        iv.layoutParams = LinearLayout.LayoutParams(dp(c, size), dp(c, size)).apply { marginStart = dp(c, mStart); marginEnd = dp(c, mEnd) }
        return iv
    }

    // صف مباراة: شعار المضيف | الفريقان + الدوري والوقت | شعار الضيف
    fun matchRow(c: Context, p: Pal, g: Sports.Game, withDate: Boolean = false): View {
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(c, 4), 0, dp(c, 4)) }
        val col = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        col.addView(UI.tv(c, Sports.title(g), 14f, p.text, true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END })
        col.addView(UI.tv(c, Sports.sub(c, g, withDate), 11f, p.sub).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
        row.addView(logoView(c, g.homeLogo, 28, 0, 8))
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(logoView(c, g.awayLogo, 28, 8, 0))
        return row
    }

    // صف فريق متابَع مع مباراته القادمة (أو رسالة لا توجد مباريات)
    fun teamRow(c: Context, p: Pal, t: Sports.Team, g: Sports.Game?): View {
        if (g != null) return matchRow(c, p, g, true)
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(c, 4), 0, dp(c, 4)) }
        val col = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        col.addView(UI.tv(c, t.name, 14f, p.text, true))
        col.addView(UI.tv(c, if (Prefs.s(c, "lang", "ar") == "en") "No upcoming matches" else "لا توجد مباريات قريبة", 11f, p.sub))
        row.addView(logoView(c, t.logo, 28, 0, 8))
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    // نافذة بكل المباريات
    fun showMatchesDialog(a: Activity, p: Pal, title: String, games: List<Sports.Game>, onRefresh: () -> Unit) {
        val en = Prefs.s(a, "lang", "ar") == "en"
        val dlg = Dialog(a, android.R.style.Theme_Material_Dialog_NoActionBar)
        val content = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (en) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            setPadding(dp(a, 28), dp(a, 22), dp(a, 28), dp(a, 18))
        }
        content.addView(UI.tv(a, title, 24f, p.accent, true).apply { setPadding(0, 0, 0, dp(a, 12)) })
        val box = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        if (games.isEmpty()) box.addView(UI.tv(a, if (en) "No matches" else "لا توجد مباريات", 18f, p.sub))
        for (g in games) box.addView(matchRow(a, p, g, true).apply { setPadding(0, dp(a, 8), 0, dp(a, 8)) })
        content.addView(ScrollView(a).apply { addView(box) }, LinearLayout.LayoutParams(-1, 0, 1f))

        fun btn(text: String, run: () -> Unit) {
            val t: TextView = UI.tv(a, text, 18f, p.text).apply {
                isFocusable = true; gravity = Gravity.CENTER
                setPadding(dp(a, 24), dp(a, 12), dp(a, 24), dp(a, 12))
                background = UI.glass(p, 20f)
                setOnFocusChangeListener { v, f -> v.background = UI.glass(p, 20f, f) }
                setOnClickListener { run() }
            }
            content.addView(t, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(a, 8) })
        }
        btn(if (en) "Refresh" else "تحديث") { dlg.dismiss(); onRefresh() }
        btn(if (en) "Close" else "إغلاق") { dlg.dismiss() }

        dlg.setContentView(content)
        dlg.show()
        dlg.window?.apply {
            setBackgroundDrawable(GradientDrawable().apply { setColor((p.bg and 0x00FFFFFF) or 0xF0000000.toInt()); cornerRadius = dp(a, 24).toFloat() })
            setLayout(dp(a, 760), (a.resources.displayMetrics.heightPixels * 0.9).toInt())
        }
    }
}
