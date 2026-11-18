package com.mokhalad.tvtheme

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * لوحة إعدادات سريعة مخصصة لـ Android TV.
 * تُعرض كنافذة شفافة فوق الشاشة الرئيسية، وتُغلق بزر Back.
 */
object QuickSettingsPanel {

    private fun str(c: Context, ar: String, en: String) =
        if (Prefs.s(c, "lang", "ar") == "en") en else ar

    private fun dp(c: Context, v: Int) = UI.dp(c, v)

    data class Item(
        val icon: String,
        val titleAr: String,
        val titleEn: String,
        val action: (Activity) -> Unit
    )

    fun show(activity: Activity) {
        val p = Themes.get(activity)
        val isEn = Prefs.s(activity, "lang", "ar") == "en"

        val dlg = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        val root = FrameLayout(activity).apply {
            setBackgroundColor(0x99000000.toInt()) // خلفية معتمة شفافة
            setOnClickListener { dlg.dismiss() }    // الضغط خارج اللوحة يغلقها
        }

        // الحاوية الرئيسية للوحة
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 40), dp(activity, 30), dp(activity, 40), dp(activity, 30))
            background = GradientDrawable().apply {
                cornerRadius = dp(activity, 28).toFloat()
                setColor((p.bg and 0x00FFFFFF) or 0xF0000000.toInt()) // خلفية اللوحة
                setStroke(dp(activity, 2), p.accent)
            }
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // عنوان اللوحة
        val title = UI.tv(activity, str(activity, "الإعدادات السريعة", "Quick Settings"), 26f, p.accent, true)
            .apply { setPadding(0, 0, 0, dp(activity, 20)) }
        panel.addView(title)

        // شبكة الأزرار (3 أعمدة)
        val grid = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

        val items = listOf(
            Item("📶", "Wi-Fi", "Wi-Fi") { openSettings(it, Settings.ACTION_WIFI_SETTINGS) },
            Item("🔵", "البلوتوث", "Bluetooth") { openSettings(it, Settings.ACTION_BLUETOOTH_SETTINGS) },
            Item("⚙️", "الإعدادات", "Settings") { openSystemSettings(it) },
            Item("🖥️", "المدخلات", "Inputs") { openInputs(it) },
            Item("🔊", "الصوت", "Sound") { openSettings(it, Settings.ACTION_SOUND_SETTINGS) },
            Item("☀️", "السطوع / الشاشة", "Display") { openSettings(it, Settings.ACTION_DISPLAY_SETTINGS) },
            Item("🔒", "قفل الجهاز", "Lock") { lockDevice(it) },
            Item("🌐", "الشبكة", "Network") { openSettings(it, Settings.ACTION_WIRELESS_SETTINGS) },
            Item("ℹ️", "معلومات النظام", "System Info") { openSystemInfo(it) }
        )

        var row: LinearLayout? = null
        items.forEachIndexed { index, item ->
            if (index % 3 == 0) {
                row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, 0, 0, dp(activity, 14))
                })
            }
            val card = buildCard(activity, p, item, isEn) { dlg.dismiss() }
            row?.addView(card, LinearLayout.LayoutParams(0, dp(activity, 120), 1f).apply {
                setMargins(dp(activity, 8), 0, dp(activity, 8), 0)
            })
        }
        panel.addView(grid)

        // زر الإغلاق
        val closeBtn = UI.tv(activity, str(activity, "إغلاق", "Close"), 18f, p.text, true).apply {
            isFocusable = true
            gravity = Gravity.CENTER
            setPadding(dp(activity, 30), dp(activity, 14), dp(activity, 30), dp(activity, 14))
            background = UI.glass(p, 20f)
            setOnFocusChangeListener { v, f -> v.background = UI.glass(p, 20f, f) }
            setOnClickListener { dlg.dismiss() }
        }
        panel.addView(closeBtn, LinearLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(activity, 18)
        })

        // إضافة اللوحة إلى الجذر مع تركيز تلقائي
        root.addView(panel, FrameLayout.LayoutParams(
            dp(activity, 720), -2, Gravity.CENTER
        ))
        dlg.setContentView(root)
        dlg.setOnShowListener {
            // تركيز أول عنصر في الشبكة
            val firstRow = grid.getChildAt(0) as? LinearLayout
            firstRow?.getChildAt(0)?.requestFocus()
        }
        dlg.show()
    }

    private fun buildCard(
        activity: Activity,
        p: Pal,
        item: Item,
        isEn: Boolean,
        onDismiss: () -> Unit
    ): View {
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isFocusable = true
            setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12))
            background = UI.glass(p, 18f)
            setOnFocusChangeListener { v, f ->
                v.animate().scaleX(if (f) 1.08f else 1f).scaleY(if (f) 1.08f else 1f)
                    .setDuration(120).start()
                v.background = UI.glass(p, 18f, f)
            }
            setOnClickListener {
                item.action(activity)
                onDismiss()
            }
        }
        val icon = UI.tv(activity, item.icon, 32f, p.accent).apply { gravity = Gravity.CENTER }
        val label = UI.tv(activity, if (isEn) item.titleEn else item.titleAr, 14f, p.text, true)
            .apply { gravity = Gravity.CENTER; setPadding(0, dp(activity, 6), 0, 0) }
        card.addView(icon)
        card.addView(label)
        return card
    }

    private fun openSettings(activity: Activity, action: String) {
        try {
            activity.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            openSystemSettings(activity)
        }
    }

    private fun openSystemSettings(activity: Activity) {
        try {
            activity.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                activity.startActivity(
                    activity.packageManager.getLeanbackLaunchIntentForPackage("com.android.tv.settings")
                )
            } catch (ex: Exception) {
                Toast.makeText(activity, "تعذر فتح الإعدادات", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openInputs(activity: Activity) {
        // محاولة فتح قائمة تبديل المدخلات في Android TV
        val tries = listOf(
            Intent("com.android.tv.action.VIEW_INPUTS"),
            Intent("android.settings.TV_INPUT_SETTINGS"),
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        )
        for (i in tries) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(i); return
            } catch (e: Exception) { }
        }
        openSystemSettings(activity)
    }

    private fun lockDevice(activity: Activity) {
        try {
            // محاولة قفل الشاشة عبر DevicePolicyManager غير مباشرة
            val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            // ملاحظة: يتطلب صلاحية Device Admin لتعمل فعلياً
            Toast.makeText(activity, "القفل يتطلب صلاحية مدير الجهاز", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(activity, "القفل غير مدعوم", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSystemInfo(activity: Activity) {
        try {
            activity.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            openSystemSettings(activity)
        }
    }
}