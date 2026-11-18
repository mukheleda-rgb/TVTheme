package com.mokhalad.tvtheme

import android.app.Activity
import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

// اختيار الفرق من قائمة (دوري ثم فريق) مع الشعارات، ويدعم متابعة أكثر من فريق
class TeamPicker(private val a: Activity, private val onDone: () -> Unit) {
    private val p = Themes.get(a)
    private val en = Prefs.s(a, "lang", "ar") == "en"
    private fun dp(v: Int) = UI.dp(a, v)
    private fun s(ar: String, en2: String) = if (en) en2 else ar
    private lateinit var dlg: Dialog
    private lateinit var title: TextView
    private lateinit var box: LinearLayout
    private var inLeague = false
    private var screenId = 0

    fun show() {
        dlg = Dialog(a, android.R.style.Theme_Material_Dialog_NoActionBar)
        val content = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (en) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            setPadding(dp(28), dp(22), dp(28), dp(18))
        }
        title = UI.tv(a, "", 24f, p.accent, true).apply { setPadding(0, 0, 0, dp(12)) }
        content.addView(title)
        box = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        content.addView(ScrollView(a).apply { addView(box) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val done = row(content)
        done.first.gravity = Gravity.CENTER
        done.second.text = s("تم", "Done")
        done.first.setOnClickListener { dlg.dismiss() }

        dlg.setContentView(content)
        dlg.setOnDismissListener { onDone() }
        dlg.setOnKeyListener { _, k, e ->
            if (k == KeyEvent.KEYCODE_BACK && inLeague) {
                if (e.action == KeyEvent.ACTION_UP) showMain()
                true
            } else false
        }
        showMain()
        dlg.show()
        dlg.window?.apply {
            setBackgroundDrawable(GradientDrawable().apply { setColor((p.bg and 0x00FFFFFF) or 0xF0000000.toInt()); cornerRadius = dp(24).toFloat() })
            setLayout(dp(760), (a.resources.displayMetrics.heightPixels * 0.9).toInt())
        }
    }

    private fun row(parent: ViewGroup): Pair<LinearLayout, TextView> {
        val r = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; isFocusable = true
            setPadding(dp(20), dp(10), dp(20), dp(10))
            background = UI.glass(p, 18f)
            setOnFocusChangeListener { v, f -> v.background = UI.glass(p, 18f, f) }
        }
        val t = UI.tv(a, "", 18f, p.text)
        r.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(r, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) })
        return r to t
    }

    // صف مع شعار اختياري ونص جانبي (علامة المتابعة)
    private fun item(logo: String, text: String, mark: String): Pair<LinearLayout, TextView> {
        val (r, t) = row(box)
        t.text = text
        if (logo.isNotEmpty()) {
            val iv = ImageView(a).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            r.addView(iv, 0, LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(12) })
            Logos.load(logo, iv)
        }
        val m = UI.tv(a, mark, 14f, p.accent, true)
        r.addView(m)
        return r to m
    }

    private fun showMain() {
        inLeague = false; screenId++
        title.text = s("اختيار الفرق", "Pick Teams")
        box.removeAllViews()
        val teams = Teams.load(a)
        if (teams.isNotEmpty()) {
            box.addView(UI.tv(a, s("الفرق المتابَعة (اضغط للإزالة):", "Followed teams (press to remove):"), 14f, p.sub).apply { setPadding(0, 0, 0, dp(6)) })
            for (t in teams) {
                val (r, _) = item(t.logo, t.name, Sports.leagueLabel(a, t.league))
                r.setOnClickListener { Teams.remove(a, t.id); showMain() }
            }
        }
        box.addView(UI.tv(a, s("لإضافة فريق اختر الدوري أو البطولة:", "To add a team, choose a league:"), 14f, p.sub).apply { setPadding(0, dp(10), 0, dp(6)) })
        for (l in Sports.leagues) {
            val (r, _) = item("", if (en) l.en else l.ar, "")
            r.setOnClickListener { showLeague(l) }
        }
    }

    private fun showLeague(l: Sports.League) {
        inLeague = true; screenId++
        val my = screenId
        title.text = if (en) l.en else l.ar
        box.removeAllViews()
        val (back, _) = item("", s("رجوع", "Back"), "")
        back.setOnClickListener { showMain() }
        val loading = UI.tv(a, s("جارٍ تحميل الفرق…", "Loading teams…"), 16f, p.sub)
        box.addView(loading)
        val mark = s("متابَع", "Following")
        Net.async({ Sports.teams(l.slug) }) { list ->
            if (my == screenId) {
                box.removeView(loading)
                if (list == null) {
                    box.addView(UI.tv(a, s("تعذر تحميل الفرق: ", "Could not load teams: ") + Net.err, 16f, p.sub))
                } else {
                    for (t in list) {
                        val (r, m) = item(t.logo, t.name, if (Teams.has(a, t.id)) mark else "")
                        r.setOnClickListener {
                            if (Teams.has(a, t.id)) { Teams.remove(a, t.id); m.text = "" }
                            else if (Teams.add(a, t)) m.text = mark
                            else Toast.makeText(a, s("الحد الأقصى ${Teams.MAX} فرق", "Maximum ${Teams.MAX} teams"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
