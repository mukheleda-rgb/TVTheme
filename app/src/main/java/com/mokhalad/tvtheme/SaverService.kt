package com.mokhalad.tvtheme

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class SaverService : DreamService() {
    private val h = Handler(Looper.getMainLooper())
    private var tick: Runnable? = null
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var panel: LinearLayout
    private lateinit var wallA: ImageView
    private lateinit var wallB: ImageView
    private var nextIsA = true
    private var newsTv: TextView? = null
    private var prayerA: TextView? = null
    private var banner: TextView? = null
    private var prayers: List<Prayer>? = null
    private var lastNext: String? = null
    private var lastSecs = 99999
    private var news = emptyList<String>()
    private var ni = 0
    private var secs = 0

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        Data.nextWall(this)
        setContentView(build())
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        slide(true)
        tick = object : Runnable {
            override fun run() { update(); h.postDelayed(this, 1000) }
        }
        h.post(tick!!)
    }

    override fun onDreamingStopped() {
        tick?.let { h.removeCallbacks(it) }
        super.onDreamingStopped()
    }

    // عرض شرائح بحركة تكبير بطيئة (Ken Burns)
    private fun slide(first: Boolean) {
        if (!first) Data.nextWall(this)
        Net.async({ Data.wallpaper(this) }) { b ->
            if (b != null) {
                val inV = if (nextIsA) wallA else wallB
                val outV = if (nextIsA) wallB else wallA
                nextIsA = !nextIsA
                val interval = Prefs.s(this, "sInterval", "30").toInt()
                inV.setImageBitmap(b)
                inV.scaleX = 1f; inV.scaleY = 1f; inV.alpha = 0f
                inV.bringToFront()
                ObjectAnimator.ofFloat(inV, "alpha", 0f, 1f).setDuration(1800).start()
                outV.animate().alpha(0f).setStartDelay(1800).setDuration(0).start()
                if (Prefs.s(this, "sKen", "true") == "true") {
                    ObjectAnimator.ofPropertyValuesHolder(inV,
                        PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f)).apply {
                        duration = interval * 1000L + 4000
                        interpolator = LinearInterpolator()
                    }.start()
                }
            }
        }
    }

    private fun update() {
        val now = Date()
        clock.text = Data.timeFmt(this).format(now)
        val g = SimpleDateFormat("EEEE d MMMM", Locale.forLanguageTag("ar-u-nu-latn")).format(now)
        val hj = Data.hijri(this)
        date.text = if (hj.isEmpty()) g else "$g\n$hj"
        prayers?.let { l ->
            val (n, left) = Data.nextSecs(l)
            val prev = lastNext
            if (prev != null && n.name != prev && lastSecs <= 5) Adhan.fire(this, prev, banner)
            lastNext = n.name; lastSecs = left
            prayerA?.text = "${n.name} • ${Data.clock(left)}"
        }
        secs++
        if (secs % 12 == 0 && news.isNotEmpty()) newsTv?.text = "" + news[++ni % news.size]
        val interval = Prefs.s(this, "sInterval", "30").toInt()
        if (secs % interval == 0) slide(false)
        // إزاحة بسيطة كل دقيقة لحماية الشاشة من الحرق
        if (secs % 60 == 0) panel.animate().translationY((((secs / 60) % 5) - 2) * 14f).setDuration(3000).start()
    }

    private fun build(): View {
        val p = Themes.get(this)
        fun dp(v: Int) = UI.dp(this, v)
        val root = FrameLayout(this).apply { setBackgroundColor(p.bg); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val wallBox = FrameLayout(this)
        wallA = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; alpha = 0f }
        wallB = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; alpha = 0f }
        wallBox.addView(wallA, -1, -1); wallBox.addView(wallB, -1, -1)
        root.addView(wallBox, -1, -1)
        root.addView(View(this).apply { setBackgroundColor(p.scrim) }, -1, -1)

        // ✅ 1. الساعة (أسفل يمين الشاشة، حجم أصغر)
        val clockPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
        clock = UI.tv(this, "", 60f, p.text, true).apply { gravity = Gravity.RIGHT }
        date = UI.tv(this, "", 16f, p.sub).apply { gravity = Gravity.RIGHT; setPadding(0, 0, dp(4), dp(10)) }
        clockPanel.addView(clock, -1, -2)
        clockPanel.addView(date, -1, -2)
        root.addView(clockPanel, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.RIGHT).apply {
            bottomMargin = dp(40)
            rightMargin = dp(40)
        })

        // ✅ 2. البطاقات (يسار الشاشة، شفافة بالكامل، خطوط صغيرة)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }

        fun line(sp: Float = 14f, col: Int = p.text, bold: Boolean = false) =
            UI.tv(this, "", sp, col, bold).apply {
                gravity = Gravity.LEFT
                setPadding(0, dp(4), 0, dp(4))
                box.addView(this, -1, -2)
            }

        var any = false
        if (Prefs.b(this, "sPrayer")) {
            any = true
            val a = line(16f, p.text, true); val b = line(12f, p.sub)
            prayerA = a
            Net.async({ Data.prayers(this) }) { l ->
                if (l != null) {
                    prayers = l
                    b.text = l.filter { it.name != "الشروق" }.joinToString("   ") { "${it.name} ${it.time}" }
                }
            }
        }
        if (Prefs.b(this, "sWeather")) {
            any = true
            val a = line(14f)
            Net.async({ Data.weather(this) }) { r -> if (r != null) a.text = "${r.first}  •  ${r.second}" }
        }
        if (Prefs.b(this, "sTeam")) {
            any = true
            val a = line(14f)
            Net.async({ Sports.teamLines(this) }) { l -> if (!l.isNullOrEmpty()) a.text = l.take(3).joinToString("\n") }
        }
        if (Prefs.b(this, "sToday")) {
            any = true
            val a = line(12f)
            Net.async({ Sports.todayLines(this) }) { l -> if (!l.isNullOrEmpty()) a.text = l.take(3).joinToString("\n") }
        }
        if (Prefs.b(this, "sOil")) {
            any = true
            val a = line(12f)
            Net.async({ Data.oil(this) }) { o -> if (o != null) { val t = Data.oilText(o); a.text = "" + t.first + (if (t.second.isNotEmpty()) "\n" + t.second else "") } }
        }
        if (Prefs.b(this, "sNews")) {
            any = true
            val a = line(12f, p.sub); a.maxLines = 2; newsTv = a
            Net.async({ Data.news() }) { r -> news = r ?: emptyList(); if (news.isNotEmpty()) a.text = "" + news[0] }
        }

        if (any) {
            root.addView(box, FrameLayout.LayoutParams(-2, -2, Gravity.LEFT or Gravity.CENTER_VERTICAL).apply {
                leftMargin = dp(60)
            })
        }

        // شريط الأذان
        val bn = UI.tv(this, "", 24f, p.text, true).apply {
            visibility = View.GONE
            setPadding(dp(36), dp(16), dp(36), dp(16))
            background = UI.glass(p, 40f, true)
        }
        banner = bn
        root.addView(bn, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(40) })
        return root
    }
}
