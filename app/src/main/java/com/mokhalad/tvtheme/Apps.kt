package com.mokhalad.tvtheme

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

class AppItem(
    val label: String,
    val pkg: String,
    val intent: Intent,
    val icon: Drawable,
    val banner: Drawable? // ✅ إضافة البانر
)

object Apps {
    fun load(c: Context): List<AppItem> {
        val pm = c.packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppItem>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            for (r in pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                val pkg = r.activityInfo.packageName
                if (pkg == c.packageName || !seen.add(pkg)) continue
                val i = Intent(Intent.ACTION_MAIN).addCategory(cat).setClassName(pkg, r.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                // ✅ استخراج البانر الرسمي إن وجد
                val banner = try { r.activityInfo.loadBanner(pm) } catch (e: Exception) { null }

                out.add(AppItem(r.loadLabel(pm).toString(), pkg, i, r.loadIcon(pm), banner))
            }
        }
        return out.sortedBy { it.label.lowercase() }
    }

    fun intentFor(c: Context, pkg: String): Intent? =
        c.packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: c.packageManager.getLaunchIntentForPackage(pkg)
}