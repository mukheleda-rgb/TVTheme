package com.mokhalad.tvtheme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object Net {
    val io: ExecutorService = Executors.newCachedThreadPool()
    private val ui = Handler(Looper.getMainLooper())
    @Volatile var err: String = ""

    private const val UA =
        "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    fun get(u: String, ua: String = UA): String? = try {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("User-Agent", ua)
        c.setRequestProperty("Accept", "application/json,*/*")
        val code = c.responseCode
        if (code !in 200..299) { err = "HTTP $code"; null }
        else c.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) { err = e.javaClass.simpleName; null }

    fun decode(b: ByteArray): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(b, 0, b.size, o)
        var s = 1
        while (o.outWidth / s > 2200) s *= 2
        BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s })
    } catch (e: Exception) { null }

    fun bitmap(u: String): Bitmap? = try {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 15000
        c.setRequestProperty("User-Agent", UA)
        if (c.responseCode !in 200..299) null
        else c.inputStream.use { decode(it.readBytes()) }
    } catch (e: Exception) { null }

    fun bitmapFile(f: File): Bitmap? = try { decode(f.readBytes()) } catch (e: Exception) { null }

    fun <T> async(work: () -> T?, done: (T?) -> Unit) {
        io.execute {
            val r = try { work() } catch (e: Exception) { null }
            ui.post { done(r) }
        }
    }
}