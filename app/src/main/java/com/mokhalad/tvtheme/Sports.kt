package com.mokhalad.tvtheme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable

// مصدر المباريات: ESPN (استعلامات يوم واحد / شهر واحد لأن ESPN ألغى استعلام المدى الزمني ويعيد HTTP 400)
// + TheSportsDB احتياطياً لمباريات اليوم.
object Sports {
    private const val UA = "TVTheme/1.0 (Android TV)"
    private val arLocale = Locale.forLanguageTag("ar-u-nu-latn")

    data class League(val slug: String, val ar: String, val en: String)
    data class Team(val id: String, val name: String, val logo: String, val league: String)
    data class Game(
        val league: String, val leagueEn: String,
        val home: String, val away: String, val homeId: String, val awayId: String,
        val homeLogo: String, val awayLogo: String,
        val whenMs: Long, val state: String, val hs: String, val asc: String
    )

    val leagues = listOf(
        League("eng.1", "الدوري الإنجليزي الممتاز", "Premier League"),
        League("esp.1", "الدوري الإسباني", "La Liga"),
        League("ita.1", "الدوري الإيطالي", "Serie A"),
        League("ger.1", "الدوري الألماني", "Bundesliga"),
        League("fra.1", "الدوري الفرنسي", "Ligue 1"),
        League("uefa.champions", "دوري أبطال أوروبا", "Champions League"),
        League("uefa.europa", "الدوري الأوروبي", "Europa League"),
        League("ksa.1", "الدوري السعودي", "Saudi Pro League"),
        League("tur.1", "الدوري التركي", "Super Lig"),
        League("por.1", "الدوري البرتغالي", "Primeira Liga"),
        League("ned.1", "الدوري الهولندي", "Eredivisie"),
        League("bra.1", "الدوري البرازيلي", "Brasileirao"),
        League("arg.1", "الدوري الأرجنتيني", "Argentine League"),
        League("usa.1", "الدوري الأمريكي MLS", "MLS"),
        League("fifa.world", "المنتخبات الوطنية", "National Teams")
    )

    private val cups = mapOf(
        "uefa.europa.conf" to ("دوري المؤتمر الأوروبي" to "Conference League"),
        "eng.fa" to ("كأس الاتحاد الإنجليزي" to "FA Cup"),
        "eng.league_cup" to ("كأس الرابطة الإنجليزية" to "League Cup"),
        "esp.copa_del_rey" to ("كأس ملك إسبانيا" to "Copa del Rey"),
        "ita.coppa_italia" to ("كأس إيطاليا" to "Coppa Italia"),
        "ger.dfb_pokal" to ("كأس ألمانيا" to "DFB Pokal"),
        "fra.coupe_de_france" to ("كأس فرنسا" to "Coupe de France"),
        "fifa.friendly" to ("مباريات ودية دولية" to "International Friendlies"),
        "uefa.nations" to ("دوري الأمم الأوروبية" to "UEFA Nations League")
    )

    private val europe = listOf("uefa.champions", "uefa.europa", "uefa.europa.conf")

    // مسابقات أخرى تلعب فيها أندية كل دوري (كؤوس وبطولات قارية)
    private val extras: Map<String, List<String>> = mapOf(
        "eng.1" to europe + listOf("eng.fa", "eng.league_cup"),
        "esp.1" to europe + listOf("esp.copa_del_rey"),
        "ita.1" to europe + listOf("ita.coppa_italia"),
        "ger.1" to europe + listOf("ger.dfb_pokal"),
        "fra.1" to europe + listOf("fra.coupe_de_france"),
        "por.1" to europe, "ned.1" to europe, "tur.1" to europe,
        "fifa.world" to listOf("fifa.friendly", "uefa.nations")
    )

    private fun s(c: Context, ar: String, en: String) = if (Prefs.s(c, "lang", "ar") == "en") en else ar
    private fun loc(c: Context) = if (Prefs.s(c, "lang", "ar") == "en") Locale.ENGLISH else arLocale

    fun leagueLabel(c: Context, slug: String, fallback: String = ""): String {
        val en = Prefs.s(c, "lang", "ar") == "en"
        val l = leagues.firstOrNull { it.slug == slug }
        if (l != null) return if (en) l.en else l.ar
        val cp = cups[slug]
        if (cp != null) return if (en) cp.second else cp.first
        return if (fallback.isNotEmpty()) fallback else slug
    }

    fun leagueLabel(c: Context, g: Game): String = leagueLabel(c, g.league, g.leagueEn)

    fun title(g: Game): String =
        if (g.state == "pre") "${g.home} × ${g.away}" else "${g.home} ${g.hs} - ${g.asc} ${g.away}"

    fun sub(c: Context, g: Game, withDate: Boolean): String {
        val time = when (g.state) {
            "in" -> s(c, "مباشر", "Live")
            "post" -> s(c, "انتهت", "Full time")
            else -> SimpleDateFormat(if (withDate) "EEE d MMM • hh:mm a" else "hh:mm a", loc(c)).format(Date(g.whenMs))
        }
        return leagueLabel(c, g) + " • " + time
    }

    // ---------- الشبكة ----------
    private fun espn(path: String): String? {
        for (host in listOf("site.api.espn.com", "site.web.api.espn.com")) {
            val j = Net.get("https://$host/apis/site/v2/sports/soccer/$path", UA)
            if (j != null) return j
        }
        return null
    }

    private val cache = HashMap<String, Pair<Long, List<Game>>>()
    private val teamCache = HashMap<String, List<Team>>()

    fun clearCache() { synchronized(cache) { cache.clear() }; teamCache.clear() }

    private fun parse(j: String, slug: String): List<Game> {
        val root = JSONObject(j)
        val lname = root.optJSONArray("leagues")?.optJSONObject(0)?.optString("name") ?: ""
        val out = ArrayList<Game>()
        val evs = root.optJSONArray("events") ?: return out
        for (i in 0 until evs.length()) {
            val e = evs.optJSONObject(i) ?: continue
            val comp = e.optJSONArray("competitions")?.optJSONObject(0) ?: continue
            val cs = comp.optJSONArray("competitors") ?: continue
            var home: JSONObject? = null
            var away: JSONObject? = null
            for (k in 0 until cs.length()) {
                val t = cs.optJSONObject(k) ?: continue
                if (t.optString("homeAway") == "home") home = t else away = t
            }
            val h = home ?: continue
            val a = away ?: continue
            val ms = try { java.time.OffsetDateTime.parse(e.optString("date")).toInstant().toEpochMilli() } catch (x: Exception) { continue }
            val st = e.optJSONObject("status")?.optJSONObject("type")?.optString("state") ?: "pre"
            out.add(Game(slug, lname, nm(h), nm(a), tid(h), tid(a), logo(h), logo(a), ms, st, h.optString("score"), a.optString("score")))
        }
        return out
    }

    private fun nm(t: JSONObject) = t.optJSONObject("team")?.optString("displayName") ?: ""
    private fun tid(t: JSONObject) = t.optJSONObject("team")?.optString("id") ?: ""
    private fun logo(t: JSONObject): String {
        val tm = t.optJSONObject("team") ?: return ""
        val l = tm.optString("logo")
        if (l.isNotEmpty()) return l
        return tm.optJSONArray("logos")?.optJSONObject(0)?.optString("href") ?: ""
    }

    // dates = yyyyMMdd (يوم) أو yyyyMM (شهر)
    private fun games(slug: String, dates: String, ttl: Long): List<Game>? {
        val key = "$slug|$dates"
        synchronized(cache) {
            val c = cache[key]
            if (c != null && System.currentTimeMillis() - c.first < ttl) return c.second
        }
        val j = espn("$slug/scoreboard?dates=$dates&limit=500") ?: espn("$slug/scoreboard?dates=$dates") ?: return null
        val g = try { parse(j, slug) } catch (e: Exception) { return null }
        synchronized(cache) { cache[key] = System.currentTimeMillis() to g }
        return g
    }

    private fun many(slugs: Collection<String>, dates: String, ttl: Long): List<List<Game>?> =
        Net.io.invokeAll(slugs.map { sl -> Callable<List<Game>?> { games(sl, dates, ttl) } })
            .map { try { it.get() } catch (e: Exception) { null } }

    // ---------- قائمة فرق الدوري (للاختيار) ----------
    fun teams(slug: String): List<Team>? {
        teamCache[slug]?.let { return it }
        val j = espn("$slug/teams") ?: return null
        val arr = try {
            JSONObject(j).optJSONArray("sports")?.optJSONObject(0)?.optJSONArray("leagues")?.optJSONObject(0)?.optJSONArray("teams")
        } catch (e: Exception) { null } ?: return null
        val out = ArrayList<Team>()
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i)?.optJSONObject("team") ?: continue
            val id = t.optString("id")
            if (id.isEmpty()) continue
            val lg = t.optJSONArray("logos")?.optJSONObject(0)?.optString("href") ?: t.optString("logo")
            out.add(Team(id, t.optString("displayName", t.optString("name")), lg, slug))
        }
        out.sortBy { it.name.lowercase() }
        if (out.isEmpty()) return null
        teamCache[slug] = out
        return out
    }

    // ---------- مباريات اليوم ----------
    fun today(c: Context): List<Game>? {
        val slugs = LinkedHashSet<String>(listOf("eng.1", "esp.1", "ita.1", "ger.1", "fra.1", "uefa.champions", "uefa.europa", "ksa.1"))
        val followed = Teams.load(c)
        for (t in followed) { slugs.add(t.league); if (t.league == "fifa.world") slugs.add("fifa.friendly") }
        val f = SimpleDateFormat("yyyyMMdd", Locale.US)
        val d = f.format(Date())
        val res = many(slugs, d, 120_000)
        val ids = followed.map { it.id }.toSet()
        val list: List<Game> = if (res.all { it == null }) (todaySdb() ?: return null)
        else res.filterNotNull().flatten().filter { f.format(Date(it.whenMs)) == d }
        return list.distinctBy { it.homeId + "|" + it.awayId + "|" + it.whenMs }
            .sortedWith(compareBy({ if (it.homeId in ids || it.awayId in ids) 0 else 1 }, { it.whenMs }))
    }

    // احتياطي: TheSportsDB
    private fun todaySdb(): List<Game>? {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val j = Net.get("https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=$d&s=Soccer") ?: return null
        val a = try { JSONObject(j).optJSONArray("events") } catch (e: Exception) { null } ?: return emptyList()
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val day = SimpleDateFormat("yyyyMMdd", Locale.US)
        val today = day.format(Date())
        val out = ArrayList<Game>()
        for (i in 0 until a.length()) {
            val e = a.optJSONObject(i) ?: continue
            val tm = e.optString("strTime").let { if (it.isBlank() || it == "null") "00:00" else it.take(5) }
            val ms = try { f.parse(e.optString("dateEvent") + " " + tm)!!.time } catch (x: Exception) { continue }
            if (day.format(Date(ms)) != today) continue
            val stt = e.optString("strStatus")
            val st = when {
                stt.contains("Finished", true) || stt == "FT" -> "post"
                stt.isBlank() || stt == "null" || stt.contains("Not Started", true) || stt == "NS" -> "pre"
                else -> "in"
            }
            fun sc(k: String) = e.optString(k).let { if (it == "null") "" else it }
            out.add(Game("tsdb", e.optString("strLeague"), e.optString("strHomeTeam"), e.optString("strAwayTeam"),
                e.optString("idHomeTeam"), e.optString("idAwayTeam"),
                e.optString("strHomeTeamBadge"), e.optString("strAwayTeamBadge"),
                ms, st, sc("intHomeScore"), sc("intAwayScore")))
        }
        return out
    }

    // ---------- المباراة القادمة لكل فريق متابَع (في الدوري والكؤوس والبطولات القارية) ----------
    fun nextGames(c: Context, teams: List<Team>): List<Pair<Team, Game?>>? {
        if (teams.isEmpty()) return emptyList()
        val slugs = LinkedHashSet<String>()
        for (t in teams) { slugs.add(t.league); slugs.addAll(extras[t.league] ?: emptyList()) }
        val cal = Calendar.getInstance()
        val ym = SimpleDateFormat("yyyyMM", Locale.US)
        val m1 = ym.format(cal.time)
        cal.add(Calendar.MONTH, 1)
        val m2 = ym.format(cal.time)
        val r1 = many(slugs, m1, 600_000)
        if (r1.all { it == null }) return null
        val all = ArrayList<Game>()
        r1.filterNotNull().forEach { all.addAll(it) }
        val from = System.currentTimeMillis() - 3 * 3600_000L
        fun nextOf(t: Team): Game? =
            all.filter { it.whenMs > from && (it.homeId == t.id || it.awayId == t.id) }.minByOrNull { it.whenMs }
        if (teams.any { nextOf(it) == null }) many(slugs, m2, 600_000).filterNotNull().forEach { all.addAll(it) }
        return teams.map { it to nextOf(it) }
    }

    // ---------- أسطر نصية (لشاشة التوقف) ----------
    fun teamLines(c: Context): List<String>? {
        val teams = Teams.load(c)
        if (teams.isEmpty()) return emptyList()
        val l = nextGames(c, teams) ?: return null
        return l.map { (t, g) ->
            if (g == null) t.name + " • " + s(c, "لا مباريات قريبة", "no upcoming match")
            else title(g) + "\n" + sub(c, g, true)
        }
    }

    fun todayLines(c: Context): List<String>? {
        val g = today(c) ?: return null
        return g.map { title(it) + "\n" + sub(c, it, false) }
    }
}

// الفرق المتابَعة (مخزّنة كـ JSON في التفضيلات)
object Teams {
    const val MAX = 8

    fun load(c: Context): List<Sports.Team> = try {
        val a = JSONArray(Prefs.s(c, "teams", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Sports.Team(o.getString("id"), o.getString("name"), o.optString("logo"), o.optString("league"))
        }
    } catch (e: Exception) { emptyList() }

    private fun save(c: Context, l: List<Sports.Team>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("logo", it.logo).put("league", it.league)) }
        Prefs.setS(c, "teams", a.toString())
    }

    fun has(c: Context, id: String) = load(c).any { it.id == id }

    fun add(c: Context, t: Sports.Team): Boolean {
        val l = load(c).toMutableList()
        if (l.any { it.id == t.id }) return true
        if (l.size >= MAX) return false
        l.add(t); save(c, l); return true
    }

    fun remove(c: Context, id: String) = save(c, load(c).filter { it.id != id })
}
