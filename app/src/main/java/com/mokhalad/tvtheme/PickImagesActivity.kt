package com.mokhalad.tvtheme

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

// شفاف: يفتح منتقي الملفات ثم يستورد الصور إلى معرض الخلفيات
class PickImagesActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }, 7)
        } catch (e: Exception) {
            Toast.makeText(this, "لا يوجد مستعرض ملفات على هذا التلفاز، استخدم الرابط", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        if (rc != 7 || res != RESULT_OK || d == null) { finish(); return }
        val uris = ArrayList<Uri>()
        d.clipData?.let { for (i in 0 until it.itemCount) uris.add(it.getItemAt(i).uri) }
        if (uris.isEmpty()) d.data?.let { uris.add(it) }
        val app = applicationContext
        Toast.makeText(app, "جارٍ إضافة الصور…", Toast.LENGTH_SHORT).show()
        Net.async({ uris.count { Data.addFromUri(app, it) } }) { n ->
            Toast.makeText(app, "تمت إضافة ${n ?: 0} صورة", Toast.LENGTH_SHORT).show()
            if ((n ?: 0) > 0) { Prefs.setS(app, "wall", "gallery"); Prefs.setS(app, "bgMode", "image") }
            finish()
        }
    }
}
