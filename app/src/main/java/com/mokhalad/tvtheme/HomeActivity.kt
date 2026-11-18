package com.mokhalad.tvtheme

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var tick: Runnable? = null
    private var news: List<String> = emptyList()
    private var ni = 0
    private var wall: ImageView? = null
    private var rootLayout: FrameLayout? = null
    private var scrim: View? = null
    private var cardsBox: LinearLayout? = null
    private var rvMain: RecyclerView? = null
    private var rvFav: RecyclerView? = null
    private var curList = ArrayList<AppItem>()
    private var curFavs = ArrayList<AppItem>()
    private var prayers: List<Prayer>? = null
    private var lastNext: String? = null
    private var lastSecs = 99999
    private var builtTheme = ""
    private var curCols = 5
    private var curRow = false
    private var adMain: Ad? = null
    private var adFav: Ad? = null
    private var banner: TextView? = null

    private var clockTv: TextView? = null
    private var dateTv: TextView? = null
    private var weatherTv: TextView? = null

    private var movePkg: String? = null
    private var moveFav = false
    private var moveSnap: List<AppItem> = emptyList()

    private var movingWidgetId: String? = null
    private var widgetSnap: List<String> = emptyList()

    private val defaultWidgets = "prayer,team,today,weather,oil,news"
    private var activeWidgets = mutableListOf<String>()

    private fun dp(v: Int) = UI.dp(this, v)
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun str(ar: String, en: String): String = if (Prefs.s(this, "lang", "ar") == "en") en else ar

    override fun onResume() { super.onResume(); setContentView(build()) }
    override fun onPause() { tick?.let { h.removeCallbacks(it) }; super.onPause() }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    override fun onKeyDown(k: Int, e: KeyEvent): Boolean {
        if (k == KeyEvent.KEYCODE_MENU) { startActivity(Intent(this, SettingsActivity::class.java)); return true }
        return super.onKeyDown(k, e)
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) {
            if (movePkg != null) {
                when (e.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> moveAppStep(e.keyCode)
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> endAppMove(true)
                    KeyEvent.KEYCODE_BACK -> endAppMove(false)
                }
                return true
            } else if (movingWidgetId != null) {
                when (e.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> moveWidgetStep(e.keyCode)
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> endWidgetMove(true)
                    KeyEvent.KEYCODE_BACK -> endWidgetMove(false)
                }
                return true
            }
        }
        return super.dispatchKeyEvent(e)
    }

    private fun refresh() { Data.clearCache(); setContentView(build()) }

    private fun dialogList(title: String, opts: List<Pair<String, () -> Unit>>) {
    if (opts.isEmpty()) return

    val neon = NeonStyle.neon(this)
    val radius = dp(12).toFloat()

    // الحاوية الرئيسية للقائمة
    val panel = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(20), dp(22), dp(20))
    }

    // عنوان القائمة
    val heading = TextView(this).apply {
        text = title
        textSize = 21f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER_VERTICAL or
            if (Prefs.s(this@HomeActivity, "lang", "ar") == "en")
                Gravity.LEFT else Gravity.RIGHT
        setPadding(dp(8), dp(4), dp(8), dp(16))
    }

    panel.addView(
        heading,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(54)
        )
    )

    // عناصر القائمة
    val listContainer = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    val buttons = mutableListOf<TextView>()

    opts.forEach { option ->
        val item = TextView(this).apply {
            text = option.first
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = NeonStyle.gravity(this@HomeActivity)
            setPadding(dp(18), dp(8), dp(18), dp(8))

            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true

            background = NeonStyle.bg(neon, radius, 0)

            setOnFocusChangeListener { view, focused ->
                NeonStyle.look(view, neon, radius, focused)
            }
        }

        listContainer.addView(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                bottomMargin = dp(8)
            }
        )

        buttons.add(item)
    }

    val scroll = ScrollView(this).apply {
        isFillViewport = true
        addView(
            listContainer,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    panel.addView(
        scroll,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val dialog = AlertDialog.Builder(this)
        .setView(panel)
        .create()

    buttons.forEachIndexed { index, item ->
        item.setOnClickListener {
            dialog.dismiss()
            opts[index].second.invoke()
        }
    }

    dialog.setOnShowListener {
        NeonStyle.styleDialog(dialog, neon)

        dialog.window?.setLayout(
            minOf(dp(460), (resources.displayMetrics.widthPixels * 0.55f).toInt()),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        buttons.firstOrNull()?.post {
            buttons.firstOrNull()?.requestFocus()
        }
    }

    dialog.show()
}

    private fun editPref(title: String, key: String, def: String) {
        val et = EditText(this).apply { setText(Prefs.s(this@HomeActivity, key, def)) }
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton(str("حفظ", "Save")) { _, _ -> Prefs.setS(this, key, et.text.toString().trim()); refresh() }
            .setNegativeButton(str("إلغاء", "Cancel"), null).show()
    }

    private fun pickPref(title: String, key: String, opts: List<Pair<String, String>>) {
        AlertDialog.Builder(this).setTitle(title).setItems(opts.map { it.second }.toTypedArray()) { _, i ->
            Prefs.setS(this, key, opts[i].first); refresh()
        }.show()
    }

    private fun clockMenu() = dialogList(str("الوقت والتاريخ", "Time & Date"), listOf(
        str("نظام الساعة (12 / 24)", "Time Format") to {
            pickPref(str("نظام الساعة", "Clock Format"), "h24", listOf("0" to "12", "1" to "24"))
        },
        str("التاريخ الهجري", "Hijri Date") to {
            pickPref(str("التاريخ الهجري", "Hijri Date"), "hijriOn", listOf("1" to str("إظهار", "Show"), "0" to str("إخفاء", "Hide")))
        }
    ))

    private fun widgetMenu(id: String, title: String, settingsAction: () -> Unit) {
        val opts = mutableListOf<Pair<String, () -> Unit>>(
            str("إعدادات البطاقة", "Widget Settings") to settingsAction,
            str("تحريك عائم", "Move (Floating)") to { startWidgetMove(id) },
            str("تكبير / تصغير", "Resize") to {
                val c = Prefs.s(this, "wSize_$id", "1")
                Prefs.setS(this, "wSize_$id", if(c=="1") "2" else "1")
                refresh()
            },
            str("إخفاء البطاقة", "Hide Widget") to { activeWidgets.remove(id); saveWidgets(); refresh() }
        )
        dialogList(title, opts)
    }

    private fun startWidgetMove(id: String) {
        movingWidgetId = id
        widgetSnap = ArrayList(activeWidgets)
        toast(str("استخدم الأسهم للتحريك • OK للتثبيت • Back للإلغاء", "Use arrows to move • OK to pin • Back to cancel"))
        val idx = activeWidgets.indexOf(id)
        val v = cardsBox?.getChildAt(idx)
        v?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.translationZ(20f)?.setDuration(200)?.start()
        v?.requestFocus()
    }

    private fun moveWidgetStep(keyCode: Int) {
        val id = movingWidgetId ?: return
        val idx = activeWidgets.indexOf(id)
        if (idx < 0) return
        var dir = 0
        val split = Prefs.s(this, "mode", "split") == "split"
        val isEn = Prefs.s(this, "lang", "ar") == "en"
        if (split) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) dir = 1
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) dir = -1
        } else {
            if (isEn) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) dir = 1
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) dir = -1
            } else {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) dir = 1
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) dir = -1
            }
        }
        if (dir == 0) return
        val nIdx = idx + dir
        if (nIdx in 0 until activeWidgets.size) {
            activeWidgets.removeAt(idx)
            activeWidgets.add(nIdx, id)
            val v = cardsBox?.getChildAt(idx)
            if (v != null) {
                cardsBox?.removeViewAt(idx)
                cardsBox?.addView(v, nIdx)
                v.post { v.requestFocus() }
            }
        }
    }

    private fun endWidgetMove(confirm: Boolean) {
        val id = movingWidgetId ?: return
        if (!confirm) {
            activeWidgets.clear(); activeWidgets.addAll(widgetSnap)
            movingWidgetId = null
            refresh()
            toast(str("تم الإلغاء", "Cancelled"))
            return
        }
        saveWidgets()
        val idx = activeWidgets.indexOf(id)
        val v = cardsBox?.getChildAt(idx)
        v?.animate()?.scaleX(1f)?.scaleY(1f)?.translationZ(0f)?.setDuration(200)?.start()
        movingWidgetId = null
        toast(str("تم التثبيت بنجاح", "Pinned successfully"))
    }

    private fun saveWidgets() = Prefs.setS(this, "activeWidgets", activeWidgets.joinToString(","))

    private fun getWidgetsList() {
        val saved = Prefs.s(this, "activeWidgets", defaultWidgets).split(",").filter { it.isNotEmpty() }
        activeWidgets = saved.toMutableList()
    }

    private fun applyWallBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurValue = Prefs.s(this, "wallBlur", "0.0").toFloatOrNull() ?: 0.0f
            if (blurValue > 0f) {
                val radius = blurValue * 25f
                wall?.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
            } else {
                wall?.setRenderEffect(null)
            }
        }
    }

    private fun loadWall() {
        val p = Themes.get(this)
        val mode = Prefs.s(this, "bgMode", "image")

        val defColor = p.text
        val defSub = p.sub
        clockTv?.setTextColor(defColor)
        dateTv?.setTextColor(defSub)
        weatherTv?.setTextColor(defSub)

        if (mode == "black") {
            rootLayout?.setBackgroundColor(Color.BLACK); wall?.visibility = View.GONE; scrim?.visibility = View.GONE
        } else if (mode == "neon") {
            try { rootLayout?.setBackgroundColor(Color.parseColor(Prefs.s(this, "customColor", "#0f111a"))) }
            catch (e: Exception) { rootLayout?.setBackgroundColor(Color.BLACK) }
            wall?.visibility = View.GONE; scrim?.visibility = View.GONE
        } else {
            rootLayout?.setBackgroundColor(p.bg)
            wall?.visibility = View.VISIBLE; scrim?.visibility = View.VISIBLE
            val alpha = Prefs.s(this, "wallAlpha", "0.3").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.3f
            scrim?.alpha = 1f - alpha

            Net.async({ Data.wallpaper(this) }) { b ->
                val w = wall
                if (w != null) {
                    if (b != null) {
                        w.setImageBitmap(b); w.alpha = 0f
                        w.animate().alpha(alpha).setDuration(450).start()
                        applyWallBlur()

                        var r = 0L; var g = 0L; var b_val = 0L; var count = 0
                        for (x in 0 until b.width step 20) {
                            for (y in 0 until b.height step 20) {
                                val px = b.getPixel(x, y)
                                r += Color.red(px); g += Color.green(px); b_val += Color.blue(px); count++
                            }
                        }
                        if (count > 0) {
                            val avgColor = Color.rgb((r/count).toInt(), (g/count).toInt(), (b_val/count).toInt())
                            val avgLum = ColorUtils.calculateLuminance(avgColor)
                            val effectiveLum = avgLum * alpha
                            val isLight = effectiveLum > 0.5f

                            val mainColor = if (isLight) Color.parseColor("#1A1A1A") else Color.WHITE
                            val subColor = if (isLight) Color.parseColor("#4A4A4A") else Color.parseColor("#D0D0D0")

                            h.post {
                                clockTv?.setTextColor(mainColor)
                                dateTv?.setTextColor(subColor)
                                weatherTv?.setTextColor(subColor)
                            }
                        }
                    } else w.setImageDrawable(null)
                }
            }
        }
    }

    private fun liveAlpha() {
        val a = Prefs.s(this, "wallAlpha", "0.3").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.3f
        wall?.animate()?.cancel(); wall?.alpha = a
        scrim?.alpha = 1f - a
        applyWallBlur()
    }

    private fun changeWall() {
        if (Prefs.s(this, "bgMode", "image") != "image") {
            toast(str("اضغط مطولاً لتغيير نوع الخلفية", "Long press to change the background type")); return
        }
        Data.nextWall(this); loadWall()
    }

    private fun showWallpaperPanel() = WallpaperUI.showDialog(this, Themes.get(this), { loadWall() }, { liveAlpha() })

    private fun focusFx(v: View, p: Pal, base: Boolean, scale: Float = 1.05f) {
        fun idle() = if (base) UI.glass(p, 24f) else null
        v.background = idle()
        v.setOnFocusChangeListener { x, f ->
            val isMoving = (x.tag != null && x.tag == movePkg) || (movingWidgetId != null && activeWidgets.indexOf(movingWidgetId) == cardsBox?.indexOfChild(x))
            if (!isMoving) {
                x.animate().scaleX(if (f) scale else 1f).scaleY(if (f) scale else 1f).translationZ(if (f) 8f else 0f).setDuration(140).start()
                x.background = if (f) UI.glass(p, 28f, true) else idle()
            }
        }
    }

    private val iconColors = HashMap<String, Int>()
    private fun getAppColor(a: AppItem, p: Pal): Int {
        if (iconColors.containsKey(a.pkg)) return iconColors[a.pkg]!!
        var finalColor = p.accent
        try {
            val bmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
            a.icon.setBounds(0, 0, 40, 40)
            a.icon.draw(Canvas(bmp))
            var r = 0L; var g = 0L; var b = 0L; var n = 0
            for (x in 0 until 40 step 4) for (y in 0 until 40 step 4) {
                val px = bmp.getPixel(x, y)
                if (Color.alpha(px) > 100) { r += Color.red(px); g += Color.green(px); b += Color.blue(px); n++ }
            }
            if (n > 0) {
                val hsv = FloatArray(3)
                Color.RGBToHSV((r/n).toInt(), (g/n).toInt(), (b/n).toInt(), hsv)
                hsv[1] = 0.85f.coerceAtLeast(hsv[1])
                hsv[2] = 0.95f.coerceAtLeast(hsv[2])
                finalColor = Color.HSVToColor(hsv)
            }
        } catch (e: Exception) {}
        iconColors[a.pkg] = finalColor
        return finalColor
    }

    private fun getNetworkIcon(): Int {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return when {
            caps == null -> R.drawable.ic_wifi_disconnected
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> R.drawable.ic_ethernet
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> R.drawable.ic_wifi
            else -> R.drawable.ic_wifi_disconnected
        }
    }

    private fun openAndroidTv() {
        val pm = packageManager
        val homes = try {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .filter { it.activityInfo.packageName != packageName }
        } catch (e: Exception) { emptyList() }
        for (r in homes.sortedBy { if (it.activityInfo.packageName.contains("launcher")) 0 else 1 }) {
            try {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setClassName(r.activityInfo.packageName, r.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) { }
        }
        for (pkg in listOf("com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "com.google.android.apps.tv.launcher")) {
            val i = try { pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) } catch (e: Exception) { null }
            if (i != null) { try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (e: Exception) { } }
        }
        val all = try {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0).map { it.activityInfo.packageName }
        } catch (e: Exception) { emptyList() }
        val found = if (all.isEmpty()) str("لا شيء", "none")
        else all.joinToString("\n")
        AlertDialog.Builder(this)
            .setTitle(str("تعذر فتح واجهة Android TV", "Could not open Android TV launcher"))
            .setMessage(str("الواجهات المكتشفة:\n", "Detected launchers:\n") + found)
            .setPositiveButton(str("اختيار من القائمة", "Choose from list")) { _, _ ->
                try {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), str("اختر الواجهة", "Choose launcher")))
                } catch (e: Exception) { toast(str("تعذر فتح القائمة", "Could not open the list")) }
            }
            .setNegativeButton(str("إغلاق", "Close"), null).show()
    }

    private fun launchScreensaver() {
        try {
            startActivity(Intent(this, ScreensaverActivity::class.java))
        } catch (_: Exception) {
            toast(str("تعذر فتح شاشة التوقف", "Could not open screensaver"))
        }
    }

    private fun build(): View {
        tick?.let { h.removeCallbacks(it) }
        builtTheme = Themes.key(this); val p = Themes.get(this)
        val split = Prefs.s(this, "mode", "split") == "split"
        val isEn = Prefs.s(this, "lang", "ar") == "en"
        curRow = Prefs.s(this, "layout", "grid") == "row"

        val isCard = Prefs.s(this, "iconShape", "default") == "card"
        curCols = if (isCard) 4 else Prefs.s(this, "cols", "5").toInt()

        getWidgetsList()

        val root = FrameLayout(this).apply {
            layoutDirection = if (isEn) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            clipChildren = false
            clipToPadding = false
        }
        rootLayout = root
        root.setBackgroundColor(p.bg)
        wall = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        root.addView(wall, -1, -1)
        scrim = View(this).apply { setBackgroundColor(p.scrim) }
        root.addView(scrim, -1, -1)

        loadWall()

        if (Prefs.s(this, "vignette", "0") == "1") {
            root.addView(VignetteView(this), FrameLayout.LayoutParams(-1, -1))
        }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }

        clockTv = UI.tv(this, "", 32f, p.text, true)
        dateTv = UI.tv(this, "", 14f, p.sub)

        val timeBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        dateTv?.setPaddingRelative(dp(14), 0, 0, 0)

        timeBox.addView(clockTv); timeBox.addView(dateTv)
        focusFx(timeBox, p, false, 1.03f)
        timeBox.setOnClickListener { clockMenu() }

        val topLeftBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
        topLeftBox.addView(timeBox)

        if (activeWidgets.contains("weather")) {
            weatherTv = UI.tv(this, "...", 16f, p.sub, true).apply {
                setPadding(dp(10), dp(2), dp(10), dp(4))
                isFocusable = true
                setOnClickListener { editPref(str("المدينة", "City"), "city", "Basra") }
            }
            topLeftBox.addView(weatherTv)
            Net.async({ Data.weather(this) }) { r ->
                weatherTv?.text = if (r != null) "${r.first}  ${r.second}" else "—"
            }
        }

        cardsBox = LinearLayout(this).apply {
            orientation = if (split) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            if (split) {
                setPadding(dp(16), dp(16), dp(16), dp(16))
            } else {
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
            clipChildren = false
            clipToPadding = false
        }

        val cardsScroll: View = if (split) {
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                clipChildren = false
                clipToPadding = false
                addView(cardsBox, FrameLayout.LayoutParams(-1, -2))
            }
        } else {
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                clipChildren = false
                clipToPadding = false
                addView(cardsBox, FrameLayout.LayoutParams(-2, -1))
            }
        }

        if (split) {
            info.addView(cardsScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            info.addView(cardsScroll, LinearLayout.LayoutParams(-1, 0, 1f))
            info.setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        fun addWidget(id: String, title: String, content: View, defSize: Float, onSettings: () -> Unit) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                isFocusable = true
                setPadding(dp(20), dp(16), dp(20), dp(16))
                clipChildren = false
                clipToPadding = false

                val wSizeStr = Prefs.s(this@HomeActivity, "wSize_$id", "1")
                val wSizeFloat = wSizeStr.toFloatOrNull() ?: 1f
                val width = if (split) -1 else dp((250 * wSizeFloat).toInt())

                layoutParams = if (split) {
                    LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(12), dp(12), dp(12)) }
                } else {
                    LinearLayout.LayoutParams(width, -2).apply { setMargins(dp(12), dp(12), dp(12), dp(12)) }
                }
            }
            box.addView(UI.tv(this, title, 12f, p.sub).apply { setPadding(0, 0, 0, dp(6)) })
            box.addView(content)
            focusFx(box, p, true, 1.03f)
            box.setOnLongClickListener { widgetMenu(id, title, onSettings); true }
            box.setOnClickListener { onSettings() }
            cardsBox?.addView(box)
        }

        var prayerV: TextView? = null
        var newsTv: TextView? = null

        for (wId in activeWidgets) {
            when (wId) {
                "prayer" -> {
                    val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    val v = UI.tv(this, "...", 19f, p.text, true)
                    val d = UI.tv(this, "", 12f, p.sub)
                    lay.addView(v); lay.addView(d)
                    addWidget("prayer", str("مواقيت الصلاة", "Prayers"), lay, 1f) { editPref(str("المدينة", "City"), "city", "Basra") }
                    prayerV = v
                    Net.async({ Data.prayers(this) }) { l -> if (l != null) { prayers = l; d.text = l.filter { it.name != "الشروق" }.joinToString("  ") { "${it.name} ${it.time}" } } }
                }
                "team" -> {
                    val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    val hint = UI.tv(this, "...", 15f, p.text, true)
                    lay.addView(hint)
                    addWidget("team", str("فرقي", "My Teams"), lay, 1.5f) { TeamPicker(this) { Sports.clearCache(); refresh() }.show() }
                    val teams = Teams.load(this)
                    if (teams.isEmpty()) {
                        hint.text = str("اضغط لاختيار فرقك من القائمة", "Press to pick your teams from the list")
                    } else {
                        Net.async({ Sports.nextGames(this, teams) }) { r ->
                            lay.removeAllViews()
                            if (r == null) {
                                lay.addView(UI.tv(this, str("تعذر التحميل", "Failed to load"), 16f, p.text, true))
                                lay.addView(UI.tv(this, Net.err, 12f, p.sub))
                            } else {
                                for ((t, g) in r.take(4)) lay.addView(MatchViews.teamRow(this, p, t, g))
                            }
                        }
                    }
                }
                "today" -> {
                    val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    val head = UI.tv(this, "...", 17f, p.text, true)
                    lay.addView(head)
                    var todayGames: List<Sports.Game> = emptyList()
                    addWidget("today", str("مباريات اليوم", "Today's Matches"), lay, 1.5f) {
                        MatchViews.showMatchesDialog(this, p, str("مباريات اليوم", "Today's Matches"), todayGames) { Sports.clearCache(); refresh() }
                    }
                    Net.async({ Sports.today(this) }) { l ->
                        if (l == null) {
                            head.text = str("تعذر التحميل", "Failed to load")
                            lay.addView(UI.tv(this, Net.err, 12f, p.sub))
                        } else {
                            todayGames = l
                            head.text = if (l.isEmpty()) str("لا مباريات اليوم", "No matches today") else "${l.size} " + str("مباراة اليوم", "matches today")
                            for (g in l.take(3)) lay.addView(MatchViews.matchRow(this, p, g))
                        }
                    }
                }
                "oil" -> {
                    val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    val v = UI.tv(this, "...", 19f, p.text, true)
                    val d = UI.tv(this, "", 12f, p.sub)
                    lay.addView(v); lay.addView(d)
                    addWidget("oil", str("أسعار النفط", "Oil Prices"), lay, 1f) { editPref("الفرق عن برنت", "basrahDiff", "") }
                    Net.async({ Data.oil(this) }) { o -> val t = Data.oilText(o); v.text = t.first; d.text = t.second }
                }
                "news" -> {
                    newsTv = UI.tv(this, "...", 15f, p.text, true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
                    addWidget("news", str("الأخبار", "News"), newsTv!!, 2f) { refresh() }
                    Net.async({ Data.news() }) { r -> news = r ?: emptyList(); if (news.isNotEmpty()) newsTv?.text = news[0] }
                }
            }
        }

        val allApps = Apps.load(this); val byPkg = allApps.associateBy { it.pkg }
        val hd = csv("hidden").toSet(); curFavs.clear(); curList.clear()
        for (pkg in csv("favs")) { val a = byPkg[pkg]; if (a != null && pkg !in hd) curFavs.add(a) }
        for (pkg in csv("order")) { val a = byPkg[pkg]; if (a != null && pkg !in hd && curFavs.none { it.pkg == pkg }) curList.add(a) }
        for (a in allApps) if (a.pkg !in hd && curFavs.none { it.pkg == a.pkg } && curList.none { it.pkg == a.pkg }) curList.add(a)

        val sz = Prefs.s(this, "iconSize", "m");
        val iconDp = when (sz) { "s" -> 48; "l" -> 76; else -> 60 }
        val cellH = when (sz) { "s" -> 104; "l" -> 152; else -> 128 }
        val cellW = when (sz) { "s" -> 140; "l" -> 210; else -> 170 }
        val showNames = Prefs.b(this, "showNames", true)

        val appsTab = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }

        if (curFavs.isNotEmpty()) {
            appsTab.addView(UI.tv(this, str("التطبيقات المفضلة", "Favorite Apps"), 14f, p.sub).apply {
                gravity = Gravity.LEFT
                setPadding(dp(10), dp(8), 0, dp(4))
            })
            rvFav = RecyclerView(this).apply {
                setPadding(dp(10), dp(10), dp(10), dp(10)); clipToPadding = false
                clipChildren = false
                layoutManager = LinearLayoutManager(this@HomeActivity, RecyclerView.HORIZONTAL, false)
            }
            adFav = Ad(curFavs, p, true, iconDp, cellW, cellH, true, showNames); rvFav?.adapter = adFav
            appsTab.addView(rvFav, LinearLayout.LayoutParams(-1, if (showNames) dp(cellH + 20) else dp(cellH - 5)))
        }

        rvMain = RecyclerView(this).apply {
            setPadding(dp(10), dp(10), dp(10), dp(50)); clipToPadding = false
            clipChildren = false
            isNestedScrollingEnabled = false // يسمح للـ ScrollView الخارجي بالتمرير
            layoutManager = if (curRow) LinearLayoutManager(this@HomeActivity, RecyclerView.HORIZONTAL, false) else GridLayoutManager(this@HomeActivity, curCols)
        }
        adMain = Ad(curList, p, curRow, iconDp, cellW, cellH, false, showNames); rvMain?.adapter = adMain
        appsTab.addView(rvMain, LinearLayout.LayoutParams(-1, -2))

        val appsScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(appsTab, FrameLayout.LayoutParams(-1, -2))
        }

        val appsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        appsPanel.addView(appsScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(6), dp(6), dp(6), dp(6))
            clipChildren = false
            clipToPadding = false
        }

        fun iconBtn(iconRes: Int, longAct: (() -> Unit)? = null, act: () -> Unit): ImageView {
            val size = dp(40)
            val iv = ImageView(this).apply {
                setImageResource(iconRes)
                isFocusable = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(8), dp(8), dp(8), dp(8))
                background = UI.glass(p, size / 2f)
                layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(dp(4), 0, dp(4), 0) }
                setOnFocusChangeListener { v, f ->
                    v.background = UI.glass(p, size / 2f, f)
                    v.animate().scaleX(if (f) 1.15f else 1f).scaleY(if (f) 1.15f else 1f).setDuration(150).start()
                }
                setOnClickListener { act() }
                if (longAct != null) setOnLongClickListener { longAct(); true }
            }
            header.addView(iv)
            return iv
        }

        iconBtn(R.drawable.ic_wallpaper, { showWallpaperPanel() }) { changeWall() }
        iconBtn(R.drawable.ic_screensaver) { launchScreensaver() }
        iconBtn(R.drawable.ic_settings) { startActivity(Intent(this, SettingsActivity::class.java)) }
        iconBtn(R.drawable.ic_androidtv) { openAndroidTv() }

        val netBtn = iconBtn(getNetworkIcon()) {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e2: Exception) {
                    toast(str("تعذر فتح إعدادات الشبكة", "Could not open network settings"))
                }
            }
        }
        netBtn.tag = getNetworkIcon()

        val clockPos = Prefs.s(this, "clockPos", "auto")
        val hasWidgets = activeWidgets.isNotEmpty()
        val widgetAreaStr = Prefs.s(this, "widgetArea", "auto")

        if (split) {
            val infoWeight = when (widgetAreaStr) {
                "25" -> 25f
                "33" -> 33f
                "50" -> 50f
                else -> 25f
            }
            val appsWeight = 100f - infoWeight

            val body = LinearLayout(this).apply {
                weightSum = 100f
                setPadding(0, dp(80), 0, dp(20))
            }
            if (!hasWidgets) {
                body.addView(appsPanel, LinearLayout.LayoutParams(-1, -1))
            } else {
                val aLp = LinearLayout.LayoutParams(0, -1, appsWeight)
                val iLp = LinearLayout.LayoutParams(0, -1, infoWeight)
                val infoOnRight = when (clockPos) {
                    "right" -> true
                    "left" -> false
                    else -> !isEn
                }
                if (infoOnRight) {
                    body.addView(appsPanel, aLp); body.addView(info, iLp)
                } else {
                    body.addView(info, iLp); body.addView(appsPanel, aLp)
                }
            }
            root.addView(body, -1, -1)
        } else {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(80), 0, dp(20))
            }
            if (!hasWidgets) {
                col.addView(appsPanel, LinearLayout.LayoutParams(-1, 0, 1f))
            } else {
                val widgetsTop = clockPos != "bottom"
                val infoWeight = 1f
                val appsWeight = when (widgetAreaStr) {
                    "25" -> 3f
                    "33" -> 2f
                    "50" -> 1f
                    else -> 3f
                }
                val infoLp = LinearLayout.LayoutParams(-1, 0, infoWeight)
                val appsLp = LinearLayout.LayoutParams(-1, 0, appsWeight)
                if (widgetsTop) {
                    col.addView(info, infoLp)
                    col.addView(appsPanel, appsLp)
                } else {
                    col.addView(appsPanel, appsLp)
                    col.addView(info, infoLp)
                }
            }
            root.addView(col, -1, -1)
        }

        root.addView(header, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.RIGHT).apply {
            topMargin = dp(16)
            rightMargin = dp(8)
        })

        root.addView(topLeftBox, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.LEFT).apply {
            topMargin = dp(16)
            leftMargin = dp(8)
        })

        banner = UI.tv(this, "", 22f, p.text, true).apply {
            visibility = View.GONE
            setPadding(dp(30), dp(12), dp(30), dp(12))
            background = UI.glass(p, 30f, true)
            gravity = Gravity.CENTER
        }
        root.addView(banner, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(24) })

        rvMain?.post { rvMain?.post { rvMain?.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() } }

        var s = 0
        tick = object : Runnable {
            override fun run() {
                val now = Date()
                clockTv?.text = Data.timeFmt(this@HomeActivity).format(now)
                val hj = Data.hijri(this@HomeActivity)
                val dateStr = SimpleDateFormat("EEEE d MMMM", if (isEn) Locale.ENGLISH else Locale.forLanguageTag("ar-u-nu-latn")).format(now)
                dateTv?.text = if (hj.isEmpty()) dateStr else "$dateStr  •  $hj"

                prayers?.let { l ->
                    val (n, secs) = Data.nextSecs(l)
                    prayerV?.text = "${n.name} • ${Data.clock(secs)}"
                    if (lastNext != null && n.name != lastNext && lastSecs <= 5) {
                        Adhan.fire(this@HomeActivity, lastNext!!, banner)
                    }
                    lastNext = n.name
                    lastSecs = secs
                }

                s++
                val rot = Prefs.s(this@HomeActivity, "wallRot", "10").toIntOrNull() ?: 10
                if (rot > 0 && s % (rot * 60) == 0 && Prefs.s(this@HomeActivity, "bgMode", "image") == "image") { Data.nextWall(this@HomeActivity); loadWall() }
                if (s % 10 == 0 && news.isNotEmpty()) newsTv?.text = news[++ni % news.size]

                if (s % 5 == 0) {
                    val newIcon = getNetworkIcon()
                    if (netBtn.tag != newIcon) {
                        netBtn.setImageResource(newIcon)
                        netBtn.tag = newIcon
                    }
                }

                if (s % 60 == 0 && Themes.key(this@HomeActivity) != builtTheme) { setContentView(build()); return }
                h.postDelayed(this, 1000)
            }
        }
        h.post(tick!!)
        return root
    }

    private fun csv(k: String) = Prefs.s(this, k, "").split(",").filter { it.isNotEmpty() }

    private fun appMenu(a: AppItem, isFav: Boolean) {
        val opts = mutableListOf<Pair<String, () -> Unit>>(
            str("تحريك عائم", "Move (Floating)") to {
                movePkg = a.pkg; moveFav = isFav
                moveSnap = ArrayList(if(isFav) curFavs else curList)
                toast(str("استخدم الأسهم للتحريك • OK للتثبيت • Back للإلغاء", "Use arrows to move • OK to pin • Back to cancel"))
                val rv = if(isFav) rvFav else rvMain
                rv?.adapter?.notifyDataSetChanged()
                rv?.post {
                    val pos = (if(isFav) curFavs else curList).indexOfFirst { it.pkg == a.pkg }
                    rv.scrollToPosition(pos)
                    rv.post { rv.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus() }
                }
            }
        )

        if (isFav) opts.add(str("إزالة من المفضلة", "Remove from Fav") to { curFavs.remove(a); curList.add(0, a); saveOrder(); refresh() })
        else opts.add(str("إضافة للمفضلة", "Pin to Fav") to { curList.remove(a); curFavs.add(a); saveOrder(); refresh() })

        opts.add(str("إخفاء من الشاشة", "Hide") to {
            Prefs.setS(this, "hidden", (csv("hidden") + a.pkg).joinToString(","))
            if(isFav) curFavs.remove(a) else curList.remove(a); saveOrder(); refresh()
        })

        opts.add(str("معلومات التطبيق", "App Info") to { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.pkg}"))) })

        opts.add(str("إلغاء التثبيت", "Uninstall") to {
            AlertDialog.Builder(this)
                .setTitle(str("هل أنت متأكد؟", "Are you sure?"))
                .setMessage(str("هل تريد حقاً حذف التطبيق ${a.label}؟", "Do you really want to delete ${a.label}?"))
                .setPositiveButton(str("تأكيد الحذف", "Confirm Delete")) { _, _ ->
                    startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${a.pkg}")))
                }
                .setNegativeButton(str("رجوع", "Cancel"), null)
                .show()
            Unit
        })
        dialogList(a.label, opts)
    }

    private fun saveOrder() {
        Prefs.setS(this, "order", curList.joinToString(",") { it.pkg })
        Prefs.setS(this, "favs", curFavs.joinToString(",") { it.pkg })
    }

    private fun endAppMove(confirm: Boolean) {
        val pkg = movePkg ?: return
        val items = if (moveFav) curFavs else curList
        if (!confirm) { items.clear(); items.addAll(moveSnap) } else saveOrder()
        movePkg = null
        (if(moveFav) adFav else adMain)?.notifyDataSetChanged()
        toast(if (confirm) str("تم التثبيت بنجاح", "Pinned successfully") else str("تم الإلغاء", "Cancelled"))
    }

    private fun moveAppStep(keyCode: Int) {
        val pkg = movePkg ?: return
        val items = if (moveFav) curFavs else curList
        val from = items.indexOfFirst { it.pkg == pkg }
        if (from < 0) return
        var to = from

        val isEn = Prefs.s(this, "lang", "ar") == "en"
        val singleRow = moveFav || curRow

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> to = if(isEn) from - 1 else from + 1
            KeyEvent.KEYCODE_DPAD_RIGHT -> to = if(isEn) from + 1 else from - 1
            KeyEvent.KEYCODE_DPAD_UP -> if(!singleRow) to = from - curCols
            KeyEvent.KEYCODE_DPAD_DOWN -> if(!singleRow) to = from + curCols
        }

        if (to >= items.size && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && from < items.size - 1) to = items.size - 1

        if (to in 0 until items.size && to != from) {
            val it = items.removeAt(from); items.add(to, it)
            val adapter = if(moveFav) adFav else adMain
            adapter?.notifyItemMoved(from, to)
            val rv = if(moveFav) rvFav else rvMain
            rv?.scrollToPosition(to)
            rv?.post { rv.findViewHolderForAdapterPosition(to)?.itemView?.requestFocus() }
        }
    }

    inner class Ad(
        val items: MutableList<AppItem>,
        val p: Pal,
        val row: Boolean,
        val iconDp: Int,
        val cellW: Int,
        val cellH: Int,
        val isFav: Boolean,
        val showNames: Boolean
    ) : RecyclerView.Adapter<Ad.VH>() {
        inner class VH(val box: LinearLayout, val icBox: LinearLayout, val ic: ShapedIcon, val tx: TextView) : RecyclerView.ViewHolder(box)
        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, t: Int): VH {
            val isCard = Prefs.s(this@HomeActivity, "iconShape", "default") == "card"

            val box = LinearLayout(this@HomeActivity).apply {
                isFocusable = true
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                clipChildren = false
                clipToPadding = false

                if (isCard) {
                    val w = if (row) dp(140) else -1
                    layoutParams = RecyclerView.LayoutParams(w, dp(104)).apply {
                        setMargins(dp(8), dp(8), dp(8), dp(8))
                    }
                } else {
                    val w = if (row) dp(iconDp + 30) else -1
                    val mx = if (row) dp(4) else dp(8)
                    layoutParams = RecyclerView.LayoutParams(w, dp(cellH)).apply {
                        setMargins(mx, dp(8), mx, dp(8))
                    }
                }
            }

            val icBox = LinearLayout(this@HomeActivity).apply {
                gravity = Gravity.CENTER
                clipChildren = false
                clipToPadding = false
                if (isCard) {
                    background = null
                    setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(-1, dp(104))
                } else {
                    layoutParams = LinearLayout.LayoutParams(dp(iconDp), dp(iconDp))
                }
            }

            val ic = ShapedIcon(this@HomeActivity, Prefs.s(this@HomeActivity, "iconShape", "default"), p.fill, p.accent)

            if (isCard) {
                icBox.addView(ic, LinearLayout.LayoutParams(-1, -1))
            } else {
                icBox.addView(ic, dp(iconDp), dp(iconDp))
            }
            box.addView(icBox)

            val tx = UI.tv(this@HomeActivity, "", 14f, p.text, isCard).apply {
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(4), dp(6), 0)
                visibility = if (showNames) View.VISIBLE else View.GONE
            }
            box.addView(tx, -1, -2)

            box.setOnFocusChangeListener { _, focused ->
                val mv = (box.tag != null && box.tag == movePkg)
                ic.ring = focused || mv
                val appItem = items.firstOrNull { it.pkg == box.tag }
                val smartColor = if(appItem != null) getAppColor(appItem, p) else p.accent

                if (mv) {
                    box.translationZ = 20f
                    if (isCard) {
                        icBox.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                    } else {
                        ic.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150).start()
                    }
                    ic.accent = smartColor
                    tx.setTextColor(smartColor)
                } else if (focused) {
                    box.translationZ = 8f
                    if (isCard) {
                        icBox.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                    } else {
                        ic.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start()
                    }
                    ic.accent = smartColor
                    tx.setTextColor(smartColor)
                } else {
                    box.translationZ = 0f
                    if (isCard) {
                        icBox.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    } else {
                        ic.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    }
                    ic.accent = p.accent
                    tx.setTextColor(p.text)
                }
                ic.invalidate()
            }
            return VH(box, icBox, ic, tx)
        }

        override fun onBindViewHolder(vh: VH, i: Int) {
            val isCard = Prefs.s(this@HomeActivity, "iconShape", "default") == "card"
            val a = items[i]; val mv = a.pkg == movePkg; val focused = vh.box.hasFocus()

            if (isCard) vh.ic.setApp(a.icon, a.banner) else vh.ic.setApp(a.icon, null)

            vh.ic.ring = focused || mv
            vh.box.tag = a.pkg; vh.tx.text = a.label
            val smartColor = getAppColor(a, p)
            vh.ic.accent = if(focused || mv) smartColor else p.accent
            vh.tx.setTextColor(if (focused || mv) smartColor else p.text)

            if (isCard) {
                if (mv || focused) {
                    vh.box.translationZ = if (mv) 20f else 8f
                    vh.icBox.scaleX = 1.05f; vh.icBox.scaleY = 1.05f
                } else {
                    vh.box.translationZ = 0f
                    vh.icBox.scaleX = 1f; vh.icBox.scaleY = 1f
                }
                vh.ic.scaleX = 1f; vh.ic.scaleY = 1f
            } else {
                if (mv) {
                    vh.box.translationZ = 20f
                    vh.ic.scaleX = 1.3f; vh.ic.scaleY = 1.3f
                } else {
                    vh.box.translationZ = if(focused) 8f else 0f
                    vh.ic.scaleX = if(focused) 1.15f else 1f; vh.ic.scaleY = if(focused) 1.15f else 1f
                }
            }

            vh.box.setOnClickListener { startActivity(a.intent) }
            vh.box.setOnLongClickListener { appMenu(a, isFav); true }
        }
    }

    inner class VignetteView(context: Context) : View(context) {
        private val paint = Paint()
        private var shader: Shader? = null

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            shader = RadialGradient(
                w / 2f, h / 2f, maxOf(w, h) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.parseColor("#B3000000")),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}
