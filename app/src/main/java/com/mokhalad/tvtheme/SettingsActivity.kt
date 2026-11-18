package com.mokhalad.tvtheme

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private lateinit var p: Pal
    private lateinit var optContainer: LinearLayout
    private lateinit var catContainer: LinearLayout
    private lateinit var leftScroll: ScrollView
    private lateinit var rightScroll: ScrollView
    private val cats = ArrayList<() -> Unit>()
    private var curCat = 0
    private var resumedOnce = false
    private val accentViews = ArrayList<TextView>()
    private val neon: Int get() = p.accent
    private val defaultWidgets = "prayer,team,today,weather,oil,news"

    private fun dp(v: Int) = UI.dp(this, v)
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun str(ar: String, en: String): String = if (Prefs.s(this, "lang", "ar") == "en") en else ar

    private val recreateKeys = setOf("theme", "lang", "autoTheme", "themeDay", "themeNight", "dayStart", "dayEnd", "clockPos", "iconShape")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = NeonStyle.pal(this)
        val isEn = Prefs.s(this, "lang", "ar") == "en"

        // ✅ 1. إجبار الاتجاه LTR للتحكم اليدوي بترتيب العناصر
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(NeonStyle.BG)
        }

        // إعداد القائمة الجانبية (الأقسام)
        leftScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(300), -1)
            clipToPadding = false
            clipChildren = false
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(56))
            setBackgroundColor(NeonStyle.PANEL)
        }
        catContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(44), dp(22), dp(44))
            clipChildren = false
        }
        leftScroll.addView(catContainer)

        // إعداد القسم الأيمن (الخيارات)
        rightScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
            clipToPadding = false
            clipChildren = false
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(56))
        }
        optContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(44), dp(48), dp(44))
            clipChildren = false
        }
        rightScroll.addView(optContainer)

        // ✅ 2. ترتيب العناصر بناءً على اللغة
        if (isEn) {
            root.addView(leftScroll)  // القائمة يسار
            root.addView(rightScroll) // الخيارات يمين
        } else {
            root.addView(rightScroll) // الخيارات يسار
            root.addView(leftScroll)  // القائمة يمين
        }

        setContentView(root)
        setupCategories(b?.getInt("cat", 0) ?: 0)
    }

    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out); out.putInt("cat", curCat) }

    override fun onResume() {
        super.onResume()
        if (resumedOnce && cats.isNotEmpty()) cats[curCat.coerceIn(0, cats.size - 1)]()
        resumedOnce = true
    }

    // ✅ دالة تعديل وقت الخمول في النظام
    private fun setScreenOffTimeout(minutes: Int) {
        if (Settings.System.canWrite(this)) {
            val millis = minutes * 60 * 1000
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, millis)
            toast(str("تم تعيين مدة الخمول إلى $minutes دقيقة", "Idle timeout set to $minutes minutes"))
        } else {
            toast(str("يرجى السماح للتطبيق بتعديل إعدادات النظام", "Please allow the app to modify system settings"))
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun addCat(name: String, onSelect: () -> Unit) {
        val idx = cats.size
        cats.add(onSelect)
        val tv = UI.tv(this, name, 18f, p.text).apply {
            isFocusable = true
            gravity = NeonStyle.gravity(this@SettingsActivity)
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = NeonStyle.bg(neon, dp(12).toFloat(), 0)
            setOnFocusChangeListener { v, f ->
                if (f) {
                    curCat = idx
                    refreshCats()
                    leftScroll.smoothScrollTo(0, v.top - dp(50))
                }
                NeonStyle.look(v, neon, dp(12).toFloat(), f, idx == curCat)
                if (f) showPane(onSelect)
            }
            setOnClickListener { curCat = idx; refreshCats(); showPane(onSelect) }
        }
        catContainer.addView(tv, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
    }

    private fun refreshCats() {
        for (i in 0 until catContainer.childCount) {
            val v = catContainer.getChildAt(i)
            NeonStyle.look(v, neon, dp(12).toFloat(), v.isFocused, i == curCat)
        }
    }

    private fun showPane(onSelect: () -> Unit) {
        optContainer.animate().cancel()
        optContainer.alpha = 0f
        optContainer.translationX = if (Prefs.s(this, "lang", "ar") == "en") 20f else -20f
        onSelect()
        optContainer.animate().alpha(1f).translationX(0f).setDuration(200).start()
        rightScroll.smoothScrollTo(0, 0)
    }

    private fun redrawPane() {
        val idx = optContainer.indexOfChild(optContainer.focusedChild)
        cats[curCat.coerceIn(0, cats.size - 1)]()
        if (idx >= 0) optContainer.post { optContainer.getChildAt(idx.coerceAtMost(optContainer.childCount - 1))?.requestFocus() }
    }

    private fun themeOpts() = listOf(
        "neon" to str("نيون", "Neon"), "glass" to str("زجاجي", "Glass"),
        "light" to str("فاتح", "Light"), "dark" to str("غامق", "Dark")
    )

    private fun setupCategories(startCat: Int) {
        // 1) إعدادات النظام
        addCat(str("إعدادات النظام", "System Settings")) {
            optContainer.removeAllViews(); head(str("إعدادات النظام", "System Settings"))
            action(str("فتح إعدادات التلفاز", "Open TV Settings")) {
                try { startActivity(Intent(Settings.ACTION_SETTINGS)) }
                catch (e: Exception) { try { startActivity(packageManager.getLeanbackLaunchIntentForPackage("com.android.tv.settings")) } catch (x: Exception) { } }
            }
            dropdown(str("اللغة", "Language"), "lang", "ar", listOf("ar" to "العربية", "en" to "English"))
            dropdown(str("نظام الساعة", "Time Format"), "h24", "0", listOf("0" to "12 H", "1" to "24 H"))
        }

        // 2) الواجهة
        addCat(str("الواجهة", "User Interface")) {
            optContainer.removeAllViews(); head(str("تخصيص الواجهة", "Interface Customization"))
            dropdown(str("نمط التقسيم", "Layout Mode"), "mode", "split", listOf("split" to str("مقسوم", "Split"), "classic" to str("كلاسيكي", "Classic")))
            dropdown(str("ترتيب التطبيقات", "Apps Layout"), "layout", "grid", listOf("grid" to str("شبكة", "Grid"), "row" to str("صف واحد", "Single Row")))
            dropdown(str("حجم الأيقونات", "Icons Size"), "iconSize", "m", listOf("s" to str("صغير", "Small"), "m" to str("متوسط", "Medium"), "l" to str("كبير", "Large")))
            dropdown(str("شكل الأيقونات", "Icons Shape"), "iconShape", "default", listOf(
                "default" to str("افتراضي", "Default"), "square" to str("مربع", "Square"),
                "circle" to str("دائري", "Circle"), "diamond" to str("معيّن", "Diamond"),
                "card" to str("بطاقة عريضة", "Wide Card")))
            dropdown(str("عدد الأعمدة (شبكة)", "Grid Columns"), "cols", "5", listOf("4" to "4", "5" to "5", "6" to "6", "7" to "7"))
            dropdown(str("موقع الساعة/البطاقات", "Clock/Widgets Position"), "clockPos", "auto", listOf(
                "auto" to str("تلقائي", "Auto"),
                "left" to str("يسار", "Left"),
                "right" to str("يمين", "Right"),
                "top" to str("أعلى (كلاسيكي)", "Top (Classic)"),
                "bottom" to str("أسفل (كلاسيكي)", "Bottom (Classic)")
            ))
            dropdown(str("مساحة البطاقات (الودجت)", "Widgets Area"), "widgetArea", "auto", listOf(
                "auto" to str("تلقائي", "Auto"), "25" to str("ربع الشاشة", "Quarter"),
                "33" to str("ثلث الشاشة", "Third"), "50" to str("نصف الشاشة", "Half")))
            dropdown(str("لون مؤشر الأيقونات", "Icon Cursor Color"), "ringColor", "neon", listOf(
                "neon" to str("لون النيون", "Neon color"), "app" to str("لون التطبيق", "App color")))
            toggle(str("إظهار أسماء التطبيقات", "Show App Names"), "showNames", true)
        }

        // 3) الثيم والخلفيات
        addCat(str("الثيم والخلفيات", "Theme & Wallpapers")) {
            optContainer.removeAllViews(); head(str("الثيم والخلفيات", "Theme & Wallpapers"))
            neonRow()
            dropdown(str("الثيم اليدوي", "Manual Theme"), "theme", "glass", themeOpts())
            WallpaperUI.build(this, p, optContainer, { redrawPane() }, { }, { })
            toggle(str("تأثير الحواف الداكنة (Vignette)", "Vignette Effect"), "vignette", false)
        }

        // 4) الثيم التلقائي
        addCat(str("الثيم التلقائي", "Auto Theme")) {
            optContainer.removeAllViews(); head(str("الثيم التلقائي", "Auto Theme"))
            note(str("يبدّل الثيم تلقائياً بين النهار والليل.", "Switches the theme between day and night automatically."))
            dropdown(str("التبديل التلقائي", "Auto Switch"), "autoTheme", "0", listOf("0" to str("متوقف", "Off"), "1" to str("مفعّل", "On")))
            dropdown(str("ثيم النهار", "Day Theme"), "themeDay", "light", themeOpts())
            dropdown(str("ثيم الليل", "Night Theme"), "themeNight", "dark", themeOpts())
            dropdown(str("بداية النهار (ساعة)", "Day Start (hour)"), "dayStart", "6", listOf("5" to "5", "6" to "6", "7" to "7"))
            dropdown(str("نهاية النهار (ساعة)", "Day End (hour)"), "dayEnd", "18", listOf("17" to "17", "18" to "18", "19" to "19"))
        }

        // 5) البطاقات الذكية
        addCat(str("البطاقات الذكية", "Smart Widgets")) {
            optContainer.removeAllViews(); head(str("إدارة البطاقات", "Manage Widgets"))
            note(str("اضغط مطولاً على أي بطاقة في الشاشة الرئيسية للتحريك أو تغيير الحجم.", "Long press any widget on the home screen to move or resize."))
            val allWidgets = listOf("prayer" to str("الصلاة", "Prayer"), "team" to str("فريقي", "Team"), "today" to str("مباريات اليوم", "Today"), "weather" to str("الطقس", "Weather"), "oil" to str("النفط", "Oil"), "news" to str("الأخبار", "News"))
            for ((id, name) in allWidgets) {
                val t = row()
                fun upd() {
                    val act = Prefs.s(this, "activeWidgets", defaultWidgets).split(",").filter { it.isNotEmpty() }
                    t.text = "$name:  " + if (id in act) str("ظاهر", "Visible") else str("مخفي", "Hidden")
                }
                t.setOnClickListener {
                    val act = Prefs.s(this, "activeWidgets", defaultWidgets).split(",").filter { it.isNotEmpty() }.toMutableList()
                    if (id in act) act.remove(id) else act.add(id)
                    Prefs.setS(this, "activeWidgets", act.joinToString(",")); upd()
                }
                upd()
            }
            edit(str("المدينة (للطقس والصلاة)", "City"), "city", "Basra")
            action(str("اختيار الفرق من قائمة", "Pick Teams from List")) { TeamPicker(this) { Sports.clearCache() }.show() }
            edit(str("فرق البصرة عن برنت (مثال: -4.5)", "Basrah vs Brent diff"), "basrahDiff", "")
        }

        // 6) الأذان والصلاة
        addCat(str("الأذان والصلاة", "Adhan & Prayer")) {
            optContainer.removeAllViews(); head(str("الأذان والصلاة", "Adhan & Prayer"))
            dropdown(str("نوع الإشعار", "Notification Type"), "adhan", "banner", listOf(
                "off" to str("معطّل", "Off"), "banner" to str("بانر فقط", "Banner Only"), "sound" to str("بانر + صوت", "Banner + Sound")))
            dropdown(str("طريقة الحساب", "Calculation Method"), "method", "3", listOf(
                "3" to "Muslim World League", "2" to "ISNA", "4" to "Umm Al-Qura",
                "5" to "Egyptian", "1" to "Karachi", "8" to "Gulf Region", "0" to "Jafari"))
            edit(str("الدولة (للصلاة)", "Country (Prayer)"), "country", "Iraq")
            edit(str("تعديل التاريخ الهجري (أيام)", "Hijri Adjustment (days)"), "hijriAdj", "0")
            dropdown(str("إظهار الهجري", "Show Hijri"), "hijriOn", "1", listOf("1" to str("نعم", "Yes"), "0" to str("لا", "No")))
        }

        // 7) شاشة التوقف
        addCat(str("شاشة التوقف", "Screensaver")) {
            optContainer.removeAllViews(); head(str("شاشة التوقف", "Screensaver"))

            // ✅ إضافة خيار مدة الخمول (الوقت قبل تفعيل شاشة التوقف)
            val timeoutRow = row()
            fun updateTimeoutRow() {
                val currentMin = Prefs.s(this, "idleTimeout", "10")
                timeoutRow.text = str("مدة الخمول قبل شاشة التوقف", "Idle timeout (min)") + ":  $currentMin"
            }
            timeoutRow.setOnClickListener {
                val opts = listOf("1", "5", "10", "15", "30", "60")
                val current = Prefs.s(this, "idleTimeout", "10")
                val currentIndex = opts.indexOf(current).coerceAtLeast(0)

                val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_NoActionBar)
                    .setTitle(str("مدة الخمول قبل شاشة التوقف", "Idle timeout (min)"))
                    .setSingleChoiceItems(opts.toTypedArray(), currentIndex) { d, which ->
                        val min = opts[which].toInt()
                        Prefs.setS(this, "idleTimeout", opts[which])
                        setScreenOffTimeout(min) // استدعاء الدالة لتغيير إعدادات النظام
                        updateTimeoutRow()
                        d.dismiss()
                    }
                    .setNegativeButton(str("إلغاء", "Cancel"), null)
                    .create()
                dialog.setOnShowListener { NeonStyle.styleDialog(dialog, neon) }
                dialog.show()
            }
            updateTimeoutRow()

            toggle(str("الصلاة", "Prayer"), "sPrayer", true)
            toggle(str("الطقس", "Weather"), "sWeather", true)
            toggle(str("الفريق", "Team"), "sTeam", true)
            toggle(str("مباريات اليوم", "Today"), "sToday", true)
            toggle(str("النفط", "Oil"), "sOil", true)
            toggle(str("الأخبار", "News"), "sNews", true)
            dropdown(str("تبديل الخلفية (ثوان)", "Slide Interval (s)"), "sInterval", "30", listOf("15" to "15", "30" to "30", "60" to "60", "120" to "120"))
            dropdown(str("تأثير Ken Burns", "Ken Burns"), "sKen", "true", listOf("true" to str("مفعّل", "On"), "false" to str("معطّل", "Off")))
        }

        // 8) زر الريموت
        addCat(str("زر الريموت", "Remote Key")) {
            optContainer.removeAllViews(); head(str("زر الريموت", "Remote Key"))
            action(str("تفعيل خدمة الأزرار", "Enable Key Service")) {
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (e: Exception) { toast("Error") }
            }
            dropdown(str("وظيفة الزر", "Key Action"), "keyAct", "none", listOf(
                "none" to str("معطّل", "Off"), "app" to str("فتح تطبيق", "Open App"), "store" to str("المتجر", "Store"),
                "sys" to str("إعدادات النظام", "System Settings"), "theme" to str("إعدادات التطبيق", "App Settings"),
                "assist" to str("المساعد", "Assistant")))
            note(str("الشرح: الإعدادات ← إمكانية الوصول ← TV Theme Keys.", "Enable via Settings → Accessibility → TV Theme Keys."))
        }

        // 9) النسخ الاحتياطي
        addCat(str("النسخ الاحتياطي", "Backup")) {
            optContainer.removeAllViews(); head(str("النسخ الاحتياطي", "Backup"))
            action(str("حفظ الإعدادات", "Backup Settings")) {
                val ok = Backup.saveDownloads(this, Backup.toJson(this))
                toast(if (ok) str("تم الحفظ في Downloads", "Saved to Downloads") else str("فشل الحفظ", "Failed"))
            }
            action(str("استعادة الإعدادات", "Restore Settings")) {
                val json = Backup.readDownloads(this)
                if (json == null) { toast(str("لم يتم العثور على نسخة", "No backup found")); return@action }
                try {
                    val n = Backup.fromJson(this, json)
                    toast(str("تم الاستعادة ($n إعداد)", "Restored ($n items)"))
                    recreate()
                } catch (e: Exception) { toast(str("فشل الاستعادة", "Restore failed")) }
            }
        }

        // 10) التطبيقات المخفية
        addCat(str("التطبيقات المخفية", "Hidden Apps")) {
            optContainer.removeAllViews(); head(str("التطبيقات المخفية", "Hidden Apps"))
            val hidden = Prefs.s(this, "hidden", "").split(",").filter { it.isNotEmpty() }
            if (hidden.isEmpty()) {
                note(str("لا توجد تطبيقات مخفية حالياً.", "No hidden apps."))
            } else {
                for (pkg in hidden) {
                    val label = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }
                    action("$label  ($pkg)") {
                        Prefs.setS(this, "hidden", hidden.filter { it != pkg }.joinToString(","))
                        redrawPane()
                    }
                }
                action(str("إظهار الكل", "Show All")) { Prefs.setS(this, "hidden", ""); redrawPane() }
            }
        }

        val i = startCat.coerceIn(0, cats.size - 1)
        curCat = i
        cats[i]()
        catContainer.getChildAt(i)?.requestFocus()
        refreshCats()
    }

    private fun head(t: String) {
        accentViews.clear()
        val v = UI.tv(this, t, 26f, p.accent, true).apply { setPadding(0, 0, 0, dp(22)) }
        accentViews.add(v)
        optContainer.addView(v)
    }

    private fun neonRow() {
        val r = dp(12).toFloat()
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(16), dp(28), dp(16))
            background = NeonStyle.bg(neon, r, 0)
        }
        wrap.addView(UI.tv(this, str("لون النيون (بنفسجي ← أخضر فسفوري)", "Neon color (purple → phosphor green)"), 18f, p.text)
            .apply { gravity = NeonStyle.gravity(this@SettingsActivity) })
        val bar = HueBar(this, Prefs.s(this, "neonHue", "0.4").toFloatOrNull() ?: 0.4f) { t ->
            Prefs.setS(this, "neonHue", t.toString())
            p = NeonStyle.pal(this)
            accentViews.forEach { it.setTextColor(neon) }
            refreshCats()
            NeonStyle.look(wrap, neon, r, true)
        }
        bar.setOnFocusChangeListener { _, f -> NeonStyle.look(wrap, neon, r, f) }
        wrap.addView(bar, LinearLayout.LayoutParams(-1, dp(18)).apply { topMargin = dp(12) })
        optContainer.addView(wrap, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
    }

    private fun note(t: String) = optContainer.addView(UI.tv(this, t, 14f, p.sub).apply { setPadding(0, 0, 0, dp(16)) })

    private fun row(): TextView = UI.tv(this, "", 18f, p.text).apply {
        isFocusable = true; setPadding(dp(28), dp(18), dp(28), dp(18))
        gravity = NeonStyle.gravity(this@SettingsActivity)
        background = NeonStyle.bg(neon, dp(12).toFloat(), 0)
        setOnFocusChangeListener { v, f -> NeonStyle.look(v, neon, dp(12).toFloat(), f) }
        optContainer.addView(this, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
    }

    private fun action(title: String, run: () -> Unit) { val t = row(); t.text = title; t.setOnClickListener { run() } }

    private fun dropdown(title: String, key: String, def: String, opts: List<Pair<String, String>>) {
        val t = row()
        fun cur(): String = Prefs.s(this, key, def)
        fun upd() { t.text = "$title:  ${(opts.firstOrNull { it.first == cur() } ?: opts[0]).second}" }

        t.setOnClickListener {
            val currentVal = cur()
            val currentIndex = opts.indexOfFirst { it.first == currentVal }.coerceAtLeast(0)
            val titles = opts.map { it.second }.toTypedArray()

            val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_NoActionBar)
                .setTitle(title)
                .setSingleChoiceItems(titles, currentIndex) { d, which ->
                    Prefs.setS(this, key, opts[which].first)
                    d.dismiss()
                    if (key in recreateKeys) recreate() else upd()
                }
                .setNegativeButton(str("إلغاء", "Cancel"), null)
                .create()

            dialog.setOnShowListener { NeonStyle.styleDialog(dialog, neon) }
            dialog.show()
        }
        upd()
    }

    private fun toggle(title: String, key: String, def: Boolean) {
        val t = row()
        fun upd() { t.text = "$title:  " + if (Prefs.b(this, key, def)) str("مفعّل", "On") else str("معطّل", "Off") }
        t.setOnClickListener { Prefs.setB(this, key, !Prefs.b(this, key, def)); upd() }
        upd()
    }

    private fun edit(title: String, key: String, def: String) {
        val t = row()
        fun upd() { t.text = "$title:  ${Prefs.s(this, key, def)}" }
        t.setOnClickListener {
            val et = EditText(this).apply { setText(Prefs.s(this@SettingsActivity, key, def)); setTextColor(Color.WHITE) }
            val dialog = AlertDialog.Builder(this).setTitle(title).setView(et)
                .setPositiveButton(str("حفظ", "Save")) { _, _ -> Prefs.setS(this, key, et.text.toString().trim()); upd(); if (key in recreateKeys) recreate() }
                .setNegativeButton(str("إلغاء", "Cancel"), null).create()
            dialog.setOnShowListener { NeonStyle.styleDialog(dialog, neon) }
            dialog.show()
        }
        upd()
    }
}