package com.valerio.gametime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import java.time.Instant
import java.time.LocalDate

/**
 * Lê o histórico de uso do Android (UsageStatsManager), calcula o tempo em primeiro plano
 * por app/dia e grava no Store. O Android só guarda ~7-10 dias de eventos, por isso o app
 * sincroniza em segundo plano (WorkManager) e guarda tudo localmente.
 */
object UsageSync {
    private val lock = Any()

    @Suppress("DEPRECATION")
    fun hasPermission(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun sync(ctx: Context) {
        val app = ctx.applicationContext
        Store.init(app)
        if (!hasPermission(app)) return
        synchronized(lock) { doSync(app) }
    }

    @Suppress("DEPRECATION")
    fun isGame(ai: ApplicationInfo): Boolean =
        ai.category == ApplicationInfo.CATEGORY_GAME || (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    private fun detectGames(ctx: Context, d: AppData): List<Game> {
        val pm = ctx.packageManager
        val known = d.games.map { it.id }.toSet()
        val out = ArrayList<Game>()
        val apps = try {
            pm.getInstalledApplications(0)
        } catch (e: Exception) {
            return out
        }
        for (ai in apps) {
            val pkg = ai.packageName
            if (pkg == ctx.packageName || pkg in known || pkg in d.ignored) continue
            if (!isGame(ai)) continue
            if (pm.getLaunchIntentForPackage(pkg) == null) continue
            out.add(Game(id = pkg, name = pm.getApplicationLabel(ai).toString(), packageName = pkg, addedAt = System.currentTimeMillis()))
        }
        return out
    }

    private class SessionResult(
        val days: HashMap<String, HashMap<String, Long>>,
        val lastEnd: HashMap<String, Long>
    )

    private fun computeSessions(usm: UsageStatsManager, start: Long, end: Long): SessionResult {
        val days = HashMap<String, HashMap<String, Long>>()
        val lastEnd = HashMap<String, Long>()
        val resumed = HashMap<String, HashSet<String>>()
        val startedAt = HashMap<String, Long>()
        val zone = Dates.zone

        fun addSession(pkg: String, s: Long, e: Long) {
            if (e - s < 2000L) return
            val m = days.getOrPut(pkg) { HashMap() }
            var cur = s
            while (cur < e) {
                val ld = Instant.ofEpochMilli(cur).atZone(zone).toLocalDate()
                val nextMidnight = ld.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val segEnd = minOf(e, nextMidnight)
                val key = ld.toString()
                m[key] = (m[key] ?: 0L) + (segEnd - cur)
                cur = segEnd
            }
            val prev = lastEnd[pkg] ?: 0L
            if (e > prev) lastEnd[pkg] = e
        }

        fun closePkg(pkg: String, ts: Long) {
            resumed.remove(pkg)
            val s = startedAt.remove(pkg) ?: return
            if (ts > s) addSession(pkg, s, ts)
        }

        val events = usm.queryEvents(start, end)
        val ev = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val type = ev.eventType
            val ts = ev.timeStamp
            if (type == 16 || type == 26) { // tela desligou / desligamento
                for (p in startedAt.keys.toList()) closePkg(p, ts)
                continue
            }
            val pkg = ev.packageName ?: continue
            if (type == 1) { // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
                for (other in startedAt.keys.toList()) if (other != pkg) closePkg(other, ts)
                resumed.getOrPut(pkg) { HashSet() }.add(ev.className ?: "")
                if (!startedAt.containsKey(pkg)) startedAt[pkg] = ts
            } else if (type == 2) { // ACTIVITY_PAUSED / MOVE_TO_BACKGROUND
                val set = resumed[pkg]
                if (set != null) {
                    set.remove(ev.className ?: "")
                    if (set.isEmpty()) closePkg(pkg, ts)
                }
            }
        }
        for (p in startedAt.keys.toList()) closePkg(p, end)
        return SessionResult(days, lastEnd)
    }

    /** pkg -> (total em primeiro plano, último uso) a partir das estatísticas agregadas do Android. */
    private fun historicTotals(usm: UsageStatsManager, now: Long): Map<String, Pair<Long, Long>> {
        val begin = now - 2L * 365L * 24L * 3600000L
        fun collect(interval: Int): HashMap<String, Pair<Long, Long>> {
            val out = HashMap<String, Pair<Long, Long>>()
            val list: List<UsageStats>? = try {
                usm.queryUsageStats(interval, begin, now)
            } catch (e: Exception) {
                null
            }
            if (list != null) {
                for (u in list) {
                    val cur = out[u.packageName]
                    out[u.packageName] = Pair(
                        (cur?.first ?: 0L) + u.totalTimeInForeground,
                        maxOf(cur?.second ?: 0L, u.lastTimeUsed)
                    )
                }
            }
            return out
        }
        val y = collect(UsageStatsManager.INTERVAL_YEARLY)
        val m = collect(UsageStatsManager.INTERVAL_MONTHLY)
        val res = HashMap<String, Pair<Long, Long>>(y)
        for ((k, v) in m) {
            val cur = res[k]
            if (cur == null || v.first > cur.first) res[k] = v
        }
        return res
    }

    private fun doSync(ctx: Context) {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val snapshot = Store.state.value
        val now = System.currentTimeMillis()

        val detected = if (snapshot.autoDetect) detectGames(ctx, snapshot) else emptyList()
        val allGames = snapshot.games + detected
        val needBaseline = allGames.filter { it.packageName != null && !it.baselineDone }

        val windowDays = if (needBaseline.isNotEmpty()) 7 else 3
        val windowStartDate = LocalDate.now().minusDays((windowDays - 1).toLong())
        val windowStartKey = windowStartDate.toString()
        val res = computeSessions(usm, Dates.startOfDayMillis(windowStartDate), now)

        val baselines = HashMap<String, Long>()
        val lastUsed = HashMap<String, Long>()
        if (needBaseline.isNotEmpty()) {
            val hist = historicTotals(usm, now)
            for (g in needBaseline) {
                val pkg = g.packageName ?: continue
                val h = hist[pkg]
                var inWindow = 0L
                val dm = res.days[pkg]
                if (dm != null) for (v in dm.values) inWindow += v
                baselines[g.id] = maxOf(0L, (h?.first ?: 0L) - inWindow)
                lastUsed[g.id] = h?.second ?: 0L
            }
        }

        Store.update { d ->
            val ids = d.games.map { it.id }.toSet()
            val games = d.games.toMutableList()
            for (g in detected) if (g.id !in ids && g.id !in d.ignored) games.add(g)
            val tracked = d.tracked.toMutableMap()
            val updated = games.map { g ->
                val pkg = g.packageName ?: return@map g
                val dayMap = (tracked[g.id] ?: emptyMap()).toMutableMap()
                dayMap.keys.removeAll { it >= windowStartKey }
                val fresh = res.days[pkg]
                if (fresh != null) dayMap.putAll(fresh)
                if (dayMap.isEmpty()) tracked.remove(g.id) else tracked[g.id] = dayMap
                var ng = g
                val last = res.lastEnd[pkg] ?: 0L
                if (last > ng.lastPlayedAt) ng = ng.copy(lastPlayedAt = last)
                if (!ng.baselineDone && baselines.containsKey(g.id)) {
                    ng = ng.copy(baselineMs = baselines[g.id] ?: 0L, baselineDone = true)
                    val lu = lastUsed[g.id] ?: 0L
                    if (lu > ng.lastPlayedAt) ng = ng.copy(lastPlayedAt = lu)
                }
                ng
            }
            d.copy(games = updated, tracked = tracked, lastSync = now)
        }
    }
}
