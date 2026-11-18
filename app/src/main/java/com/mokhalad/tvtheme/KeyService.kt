package com.mokhalad.tvtheme

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

object Keys {
    // أزرار لا يُسمح بإعادة توجيهها حتى لا يتعطل التنقل
    val blocked = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME
    )
}

class KeyService : AccessibilityService() {
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(e: KeyEvent): Boolean {
        val act = Prefs.s(this, "keyAct", "none")
        if (act == "none") return false
        val code = Prefs.s(this, "keyCode", "219").toIntOrNull() ?: return false
        if (e.keyCode != code || code in Keys.blocked) return false
        if (e.action == KeyEvent.ACTION_UP) run(act)
        return true
    }

    private fun go(i: Intent?) {
        if (i != null) try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
    }

    private fun run(act: String) {
        when (act) {
            "app" -> go(Apps.intentFor(this, Prefs.s(this, "keyApp", "")))
            "store" -> go(Apps.intentFor(this, "com.android.vending"))
            "sys" -> go(Intent(Settings.ACTION_SETTINGS))
            "theme" -> go(Intent(this, SettingsActivity::class.java))
            "assist" -> go(Intent(Intent.ACTION_VOICE_COMMAND))
        }
    }
}
