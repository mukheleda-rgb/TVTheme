package com.mokhalad.tvtheme

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.Html
import java.io.File
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.concurrent.Callable
import java.util.*

data class Prayer(val name: String, val time: String)
data class Match(val title: String, val whenText: String, val homeLogo: String = "", val awayLogo: String = "")
data class Oil(val brent: Double, val chg: Double, val basrah: Double?)
data class BingImg(val idx: Int, val url: String, val title: String, val thumb: String = "")

object Data {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private val wallCache = HashMap<String, Bitmap>()
    private val arLocale = Locale.forLanguageTag("ar-u-nu-latn")

    fun prayers(c: Context): List<Prayer>? {
        val j = Net.get("https://api.aladhan.com/v1/timingsByCity?city=${enc(Prefs.s(c, "city", "Basra"))}&country=${enc(Prefs.s(c, "country", "Iraq"))}&method=${Prefs.s(c, "method", "3")}") ?: return null
        val t = JSONObject(j).getJSONObject("data").getJSONObject("timings")
        return listOf("الفجر" to "Fajr", "الشروق" to "Sunrise", "الظهر" to "Dhuhr", "العصر" to "Asr", "المغرب" to "Maghrib", "العشاء" to "Isha")
            .map { Prayer(it.first, t.getString(it.second).take(5)) }
    }

    fun weather(c: Context): Pair<String, String>? {
        val city = Prefs.s(c, "city", "Basra")
        val g = JSONObject(Net.get("https://geocoding-api.open-meteo.com/v1/search?name=${enc(city)}&count=1") ?: return null)
            .optJSONArray("results")?.optJSONObject(0) ?: return null
        val w = JSONObject(Net.get("https://api.open-meteo.com/v1/forecast?latitude=${g.getDouble("latitude")}&longitude=${g.getDouble("longitude")}&current=temperature_2m,weather_code&timezone=auto") ?: return null)
            .getJSONObject("current")
        val d = when (w.getInt("weather_code")) {
            0 -> "صافٍ"; 1, 2 -> "غائم جزئياً"; 3 -> "غائم"; 45, 48 -> "ضباب"
            in 51..67 -> "مطر"; in 71..77 -> "ثلج"; in 80..82 -> "زخات مطر"; in 95..99 -> "عاصفة رعدية"
            else -> "—"
        }
        return "${Math.round(w.getDouble("temperature_2m"))}°" to "$d • $city"
    }

    private fun local(date: String, time: String?, pattern: String): String = try {
        val t = if (time.isNullOrBlank() || time == "null") "00:00" else time.take(5)
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        SimpleDateFormat(pattern, arLocale).format(f.parse("$date $t")!!)
    } catch (e: Exception) { date }

    fun timeFmt(c: Context) = SimpleDateFormat(if (Prefs.s(c, "h24", "0") == "1") "HH:mm" else "hh:mm", Locale.US)
    fun clearCache() { Sports.clearCache() }

    fun news(): List<String>? {
        val x = Net.get("https://news.google.com/rss/search?q=${enc("العراق")}&hl=ar&gl=IQ&ceid=IQ:ar") ?: return null
        val item = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
        val title = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
        return item.findAll(x).mapNotNull { title.find(it.groupValues[1])?.groupValues?.get(1) }
            .map { Html.fromHtml(it.replace("<![CDATA[", "").replace("]]>", ""), Html.FROM_HTML_MODE_LEGACY).toString().trim() }
            .take(10).toList()
    }

    fun nextSecs(l: List<Prayer>): Pair<Prayer, Int> {
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
        fun t(p: Prayer) = (p.time.substring(0, 2).toInt() * 60 + p.time.substring(3, 5).toInt()) * 60
        val list = l.filter { it.name != "الشروق" }
        val up = list.firstOrNull { t(it) > now }
        val n = up ?: list.first()
        return n to (if (up != null) t(n) - now else t(n) + 86400 - now)
    }

    fun clock(secs: Int): String = String.format(Locale.US, "%d:%02d:%02d", secs / 3600, secs / 60 % 60, secs % 60)

    private val hm = arrayOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    fun hijri(c: Context): String = if (Prefs.s(c, "hijriOn", "1") == "0") "" else try {
        val adj = Prefs.s(c, "hijriAdj", "0").toLong()
        val d = java.time.chrono.HijrahDate.now().plus(adj, java.time.temporal.ChronoUnit.DAYS)
        val day = d.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        val mon = d.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val yr = d.get(java.time.temporal.ChronoField.YEAR)
        "$day ${hm[mon - 1]} $yr هـ"
    } catch (e: Exception) { "" }

    fun oil(c: Context): Oil? {
        val j = Net.get("https://query1.finance.yahoo.com/v8/finance/chart/BZ%3DF?range=1d&interval=1d") ?: return null
        val m = JSONObject(j).getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("meta")
        val px = m.getDouble("regularMarketPrice")
        val prev = m.optDouble("chartPreviousClose", px)
        val diff = Prefs.s(c, "basrahDiff", "").toDoubleOrNull()
        return Oil(px, if (prev > 0) (px - prev) / prev * 100 else 0.0, diff?.let { px + it })
    }

    fun oilText(o: Oil?): Pair<String, String> =
        if (o == null) "تعذر التحميل" to ""
        else String.format(Locale.US, "برنت \$%.2f  (%+.1f%%)", o.brent, o.chg) to
                (o.basrah?.let { String.format(Locale.US, "البصرة: \$%.2f", it) } ?: "")

    fun galleryFiles(c: Context): List<File> = File(c.filesDir, "wall").listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
    fun nextWall(c: Context) { Prefs.setS(c, "wallIdx", (Prefs.s(c, "wallIdx", "0").toInt() + 1).toString()) }

    private fun saveBitmap(c: Context, b: Bitmap) {
        val dir = File(c.filesDir, "wall").apply { mkdirs() }
        File(dir, "${System.currentTimeMillis()}_${(Math.random() * 100000).toInt()}.jpg")
            .outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        val max = Prefs.s(c, "maxImgs", "10").toIntOrNull() ?: 10
        val fs = galleryFiles(c)
        if (fs.size > max) fs.take(fs.size - max).forEach { it.delete() }
    }

    fun addFromUri(c: Context, u: Uri): Boolean {
        val bytes = try { c.contentResolver.openInputStream(u)?.use { it.readBytes() } } catch (e: Exception) { null } ?: return false
        val b = Net.decode(bytes) ?: return false
        saveBitmap(c, b); return true
    }

    fun addFromUrl(c: Context, url: String): Boolean {
        val b = Net.bitmap(url) ?: return false
        saveBitmap(c, b); return true
    }

    fun clearGallery(c: Context) { galleryFiles(c).forEach { it.delete() } }

    fun wallpaper(c: Context, idx: Int = Prefs.s(c, "wallIdx", "0").toInt()): Bitmap? {
        when (Prefs.s(c, "wall", "bing")) {
            "bing" -> {
                val i = Math.floorMod(idx, 8)
                val j = Net.get("https://www.bing.com/HPImageArchive.aspx?format=js&idx=$i&n=1&mkt=en-US") ?: return null
                val url = "https://www.bing.com" + JSONObject(j).getJSONArray("images").getJSONObject(0).getString("url")
                return wallCache[url] ?: Net.bitmap(url)?.also { wallCache.clear(); wallCache[url] = it }
            }
            "picsum" -> {
                // ✅ مصدر Picsum المجاني
                val url = "https://picsum.photos/seed/$idx/1920/1080"
                return wallCache[url] ?: Net.bitmap(url)?.also { wallCache.clear(); wallCache[url] = it }
            }
            "gallery" -> {
                val fs = galleryFiles(c)
                if (fs.isEmpty()) return null
                return Net.bitmapFile(fs[Math.floorMod(idx, fs.size)])
            }
            else -> return null
        }
    }

    // قائمة صور Bing (8 صور) لاختيار المستخدم
    fun bingList(n: Int = 8): List<BingImg>? {
        val j = Net.get("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=$n&mkt=en-US") ?: return null
        val arr = JSONObject(j).getJSONArray("images")
        val out = ArrayList<BingImg>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val u = "https://www.bing.com" + o.getString("urlbase") + "_1920x1080.jpg"
            out.add(BingImg(i, u, o.optString("title", ""), "https://www.bing.com" + o.getString("urlbase") + "_400x240.jpg"))
        }
        return out
    }
}