package com.mokhalad.tvtheme

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject

object Backup {
    private const val NAME = "TVTheme-backup.json"

    fun toJson(c: Context): String {
        val o = JSONObject()
        for ((k, v) in c.getSharedPreferences("tvtheme", 0).all) { if (k != "wallIdx") o.put(k, v) }
        return o.toString(2)
    }

    fun fromJson(c: Context, s: String): Int {
        val o = JSONObject(s)
        var n = 0
        for (k in o.keys()) {
            val v = o.get(k)
            if (v is Boolean) Prefs.setB(c, k, v) else Prefs.setS(c, k, v.toString())
            n++
        }
        return n
    }

    fun saveDownloads(c: Context, json: String): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        return try {
            val r = c.contentResolver
            try { r.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "${MediaStore.MediaColumns.DISPLAY_NAME}=?", arrayOf(NAME)) } catch (e: Exception) { }
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download")
            }
            val u = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: return false
            r.openOutputStream(u)?.use { it.write(json.toByteArray()) }
            true
        } catch (e: Exception) { false }
    }

    fun readDownloads(c: Context): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        return try {
            c.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?", arrayOf("TVTheme-backup%"),
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cur ->
                if (cur.moveToFirst()) {
                    val u = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cur.getLong(0))
                    c.contentResolver.openInputStream(u)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } else null
            }
        } catch (e: Exception) { null }
    }
}
