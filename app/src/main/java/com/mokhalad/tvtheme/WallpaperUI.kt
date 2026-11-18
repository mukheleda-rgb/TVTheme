package com.mokhalad.tvtheme

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

object WallpaperUI {
    private fun str(c: Context, ar: String, en: String) = if (Prefs.s(c, "lang", "ar") == "en") en else ar
    private fun dp(c: Context, v: Int) = UI.dp(c, v)

    private fun rowIn(c: Context, p: Pal, box: ViewGroup): TextView = UI.tv(c, "", 18f, p.text).apply {
        isFocusable = true
        setPadding(dp(c, 24), dp(c, 14), dp(c, 24), dp(c, 14))
        background = UI.glass(p, 20f)
        setOnFocusChangeListener { v, f -> v.background = UI.glass(p, 20f, f) }
        box.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(c, 8)) })
    }

    private fun action(c: Context, p: Pal, box: ViewGroup, title: String, run: () -> Unit) {
        val t = rowIn(c, p, box); t.text = title; t.setOnClickListener { run() }
    }

    private fun cycle(c: Context, p: Pal, box: ViewGroup, title: String, key: String, def: String,
                      opts: List<Pair<String, String>>, after: () -> Unit) {
        val t = rowIn(c, p, box)
        fun cur() = Prefs.s(c, key, def)
        t.text = "$title:  ${(opts.firstOrNull { it.first == cur() } ?: opts[0]).second}"
        t.setOnClickListener {
            val i = opts.indexOfFirst { it.first == cur() }
            Prefs.setS(c, key, opts[(i + 1) % opts.size].first)
            after()
        }
    }

    // ✅ تعديل دالة الشريط للسماح بالوصول إلى 0
    private fun slider(c: Context, p: Pal, box: ViewGroup, title: String, key: String, def: Float, onLive: () -> Unit) {
        val wrap = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 24), dp(c, 12), dp(c, 24), dp(c, 12))
            background = UI.glass(p, 20f)
        }
        val label = UI.tv(c, "", 18f, p.text)
        val sb = SeekBar(c).apply {
            max = 100; min = 0; keyProgressIncrement = 5; isFocusable = true // min = 0
            val cur = Prefs.s(c, key, def.toString()).toFloatOrNull() ?: def
            progress = Math.round(cur * 100).coerceIn(0, 100)
        }
        fun upd() { label.text = "$title:  ${sb.progress}%" }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                Prefs.setS(c, key, (v / 100f).toString()); upd(); onLive()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        sb.setOnFocusChangeListener { _, f -> wrap.background = UI.glass(p, 20f, f) }
        wrap.addView(label)
        wrap.addView(sb, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(c, 8) })
        box.addView(wrap, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(c, 8)) })
        upd()
    }

    private fun intervalSlider(c: Context, p: Pal, box: ViewGroup, title: String, key: String, def: Int) {
        val wrap = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 24), dp(c, 12), dp(c, 24), dp(c, 12))
            background = UI.glass(p, 20f)
        }
        val label = UI.tv(c, "", 18f, p.text)
        val sb = SeekBar(c).apply {
            max = 120; min = 1; keyProgressIncrement = 1; isFocusable = true
            progress = Prefs.s(c, key, def.toString()).toIntOrNull()?.coerceIn(1, 120) ?: def
        }
        fun upd() { label.text = "$title:  ${sb.progress} " + str(c, "دقيقة", "min") }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                Prefs.setS(c, key, v.toString()); upd()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        sb.setOnFocusChangeListener { _, f -> wrap.background = UI.glass(p, 20f, f) }
        wrap.addView(label)
        wrap.addView(sb, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(c, 8) })
        box.addView(wrap, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(c, 8)) })
        upd()
    }

    private fun colorRow(a: Activity, p: Pal, box: ViewGroup, title: String, current: Int, onPick: (Int) -> Unit) {
        val r = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; isFocusable = true
            setPadding(dp(a, 24), dp(a, 14), dp(a, 24), dp(a, 14))
            background = UI.glass(p, 20f)
            setOnFocusChangeListener { v, f -> v.background = UI.glass(p, 20f, f) }
        }
        r.addView(UI.tv(a, "$title:  " + String.format("#%06X", 0xFFFFFF and current), 18f, p.text), LinearLayout.LayoutParams(0, -2, 1f))
        val sw = View(a).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(current); setStroke(dp(a, 2), Color.WHITE) }
        }
        r.addView(sw, LinearLayout.LayoutParams(dp(a, 36), dp(a, 36)))
        r.setOnClickListener { ColorPickerDialog(a, current, onPick).show() }
        box.addView(r, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(a, 8)) })
    }

    fun build(a: Activity, p: Pal, box: ViewGroup, redraw: () -> Unit, onChange: () -> Unit, live: () -> Unit) {
        fun s(ar: String, en: String) = str(a, ar, en)
        fun changed() { onChange(); redraw() }
        val mode = Prefs.s(a, "bgMode", "image")

        cycle(a, p, box, s("نوع الخلفية", "Background Type"), "bgMode", "image", listOf(
            "image" to s("صورة", "Image"), "black" to s("أسود", "Solid Black"), "neon" to s("لون مخصص", "Custom Color")
        )) { changed() }

        if (mode == "image") {
            cycle(a, p, box, s("مصدر الصور", "Image Source"), "wall", "bing", listOf(
                "bing" to s("صور Bing", "Bing Images"),
                "picsum" to "Picsum",
                "gallery" to s("معرض صوري", "My Gallery")
            )) { changed() }

            action(a, p, box, s("اختيار صورة من Bing", "Pick a Bing Image")) {
                BingPickerDialog(a) { idx ->
                    Prefs.setS(a, "wall", "bing"); Prefs.setS(a, "wallIdx", idx.toString()); changed()
                }.show()
            }
            action(a, p, box, s("الصورة التالية", "Next Image")) { Data.nextWall(a); onChange() }
            action(a, p, box, s("إضافة صور من ذاكرة التلفاز", "Add Photos from Storage")) {
                a.startActivity(Intent(a, PickImagesActivity::class.java))
            }
            action(a, p, box, s("إضافة صورة من رابط", "Add Photo from URL")) {
                val et = EditText(a)
                AlertDialog.Builder(a).setTitle(s("رابط مباشر للصورة (jpg / png)", "Direct image link (jpg / png)")).setView(et)
                    .setPositiveButton(s("إضافة", "Add")) { _, _ ->
                        Toast.makeText(a, s("جارٍ التنزيل…", "Downloading…"), Toast.LENGTH_SHORT).show()
                        val u = et.text.toString().trim()
                        Net.async({ Data.addFromUrl(a, u) }) { ok ->
                            Toast.makeText(a, if (ok == true) s("تمت الإضافة", "Added") else s("فشل التنزيل", "Download failed"), Toast.LENGTH_SHORT).show()
                            if (ok == true) { Prefs.setS(a, "wall", "gallery"); changed() }
                        }
                    }.setNegativeButton(s("إلغاء", "Cancel"), null).show()
            }
            val g = rowIn(a, p, box)
            g.text = s("الصور المحفوظة", "Saved Photos") + ":  ${Data.galleryFiles(a).size}  (" + s("اضغط لحذف الكل", "press to delete all") + ")"
            g.setOnClickListener {
                AlertDialog.Builder(a).setTitle(s("حذف كل الصور المحفوظة؟", "Delete all saved photos?"))
                    .setPositiveButton(s("حذف", "Delete")) { _, _ -> Data.clearGallery(a); changed() }
                    .setNegativeButton(s("إلغاء", "Cancel"), null).show()
            }
            cycle(a, p, box, s("أقصى عدد للصور", "Max Photos"), "maxImgs", "10",
                listOf("5" to "5", "10" to "10", "20" to "20")) { redraw() }

            intervalSlider(a, p, box, s("تغيير تلقائي للخلفية", "Auto Change"), "wallRot", 10)

            // ✅ شريط الشفافية
            slider(a, p, box, s("شفافية الخلفية (الظهور)", "Wallpaper Visibility"), "wallAlpha", 0.3f, live)

            // ✅ شريط التمويه الجديد
            slider(a, p, box, s("تمويه الخلفية", "Wallpaper Blur"), "wallBlur", 0.0f, live)

        } else if (mode == "neon") {
            val cur = try { Color.parseColor(Prefs.s(a, "customColor", "#0f111a")) } catch (e: Exception) { Color.BLACK }
            colorRow(a, p, box, s("لون الخلفية (عجلة الألوان)", "Background Color (wheel)"), cur) { col ->
                Prefs.setS(a, "customColor", String.format("#%06X", 0xFFFFFF and col))
                Prefs.setS(a, "bgMode", "neon")
                changed()
            }
        }
    }

    fun showDialog(a: Activity, p: Pal, onChange: () -> Unit, live: () -> Unit) {
        val dlg = Dialog(a, android.R.style.Theme_Material_Dialog_NoActionBar)
        val isEn = Prefs.s(a, "lang", "ar") == "en"
        val content = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (isEn) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            setPadding(dp(a, 28), dp(a, 22), dp(a, 28), dp(a, 18))
        }
        content.addView(UI.tv(a, str(a, "إعدادات الخلفية", "Wallpaper Settings"), 24f, p.accent, true).apply { setPadding(0, 0, 0, dp(a, 12)) })
        val box = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        val sv = ScrollView(a).apply { addView(box) }
        content.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        val close = rowIn(a, p, content)
        close.text = str(a, "إغلاق", "Close")
        close.setOnClickListener { dlg.dismiss() }

        lateinit var redo: () -> Unit
        redo = {
            val idx = box.indexOfChild(box.focusedChild)
            box.removeAllViews()
            build(a, p, box, redo, onChange, live)
            if (idx >= 0) box.post { box.getChildAt(idx.coerceAtMost(box.childCount - 1))?.requestFocus() }
        }
        redo()
        dlg.setContentView(content)
        dlg.show()
        dlg.window?.apply {
            setBackgroundDrawable(GradientDrawable().apply {
                setColor((p.bg and 0x00FFFFFF) or 0xF0000000.toInt()); cornerRadius = dp(a, 24).toFloat()
            })
            setLayout(dp(a, 780), (a.resources.displayMetrics.heightPixels * 0.9).toInt())
        }
    }
}