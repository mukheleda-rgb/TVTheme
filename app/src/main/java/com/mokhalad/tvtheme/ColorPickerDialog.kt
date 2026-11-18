package com.mokhalad.tvtheme

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast

class ColorPickerDialog(
    private val context: Context,
    initialColor: Int,
    private val onPicked: (Int) -> Unit
) {
    private val hsv = FloatArray(3).also { Color.colorToHSV(initialColor, it) }
    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
    private fun str(ar: String, en: String) = if (Prefs.s(context, "lang", "ar") == "en") en else ar

    fun show() {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), dp(24), dp(30), dp(20))
        }

        // مربع المعاينة (يتغير لونه مباشرة)
        val preview = View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.HSVToColor(hsv))
                setStroke(dp(2), Color.WHITE)
            }
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, dp(40)).apply { setMargins(0, 0, 0, dp(12)) })

        val brightLabel = UI.tv(context, str("السطوع", "Brightness"), 12f, Color.LTGRAY).apply {
            gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(4))
        }
        val brightness = SeekBar(context).apply {
            max = 100
            progress = (hsv[2] * 100).toInt()
            isFocusable = true
        }
        root.addView(brightLabel)
        root.addView(brightness, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(20), 0, dp(20), dp(8)) })

        val wheel = ColorWheelView(context).apply {
            hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
            isFocusable = true
        }
        root.addView(wheel, LinearLayout.LayoutParams(dp(320), dp(320)).apply { gravity = Gravity.CENTER_HORIZONTAL })

        val hint = UI.tv(
            context,
            str("استخدم الأسهم لاختيار اللون\nاضغط OK لتأكيد اللون والخروج",
                "Use arrows to pick a color\nPress OK to confirm and exit"),
            12f, Color.GRAY
        ).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0) }
        root.addView(hint)

        // تحديث اللون في المعاينة عند تغيير العجلة أو شريط السطوع
        fun refresh() {
            (preview.background as GradientDrawable).setColor(wheel.pickedColor())
            val pr = Math.round(wheel.value * 100)
            if (brightness.progress != pr) brightness.progress = pr
        }
        wheel.onChange = { refresh() }
        brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, prg: Int, fromUser: Boolean) { wheel.value = prg / 100f }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(context)
            .setTitle(str("اختر لوناً", "Pick a Color"))
            .setView(root)
            .setPositiveButton(str("حفظ", "Save")) { _, _ -> onPicked(wheel.pickedColor()) }
            .setNegativeButton(str("إلغاء", "Cancel"), null)
            .create()

        // ✅ الحل السحري: استماع لزر OK (Enter / Center) أثناء الوقوف على العجلة
        wheel.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    // حفظ اللون والخروج فوراً
                    onPicked(wheel.pickedColor())
                    dialog.dismiss()
                    return@setOnKeyListener true
                } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                    // زر القائمة ينقل التركيز إلى شريط السطوع
                    brightness.requestFocus()
                    return@setOnKeyListener true
                }
            }
            false
        }

        // إعطاء التركيز للعجلة فور فتح النافذة
        dialog.setOnShowListener {
            wheel.requestFocus()
            refresh()
        }
        dialog.show()
    }
}