package com.mokhalad.tvtheme

import android.content.Context
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView

object Adhan {
    fun fire(c: Context, name: String, banner: TextView?) {
        val m = Prefs.s(c, "adhan", "banner")
        if (m == "off") return
        banner?.let { b ->
            b.text = "حان الآن وقت صلاة $name"
            b.visibility = View.VISIBLE
            b.postDelayed({ b.visibility = View.GONE }, 90000)
        }
        if (m == "sound") try {
            val r = RingtoneManager.getRingtone(c, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            r.play()
            Handler(Looper.getMainLooper()).postDelayed({ try { r.stop() } catch (e: Exception) { } }, 15000)
        } catch (e: Exception) { }
    }
}
