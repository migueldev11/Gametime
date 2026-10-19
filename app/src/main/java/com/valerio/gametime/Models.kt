package com.valerio.gametime

import org.json.JSONArray
import org.json.JSONObject

data class Game(
    val id: String,
    val name: String,
    val packageName: String?,
    val addedAt: Long,
    val offsetMs: Long = 0L,
    val baselineMs: Long = 0L,
    val baselineDone: Boolean = false,
    val lastPlayedAt: Long = 0L,
    val notes: String = "",
    val favorite: Boolean = false
)

data class AppData(
    val games: List<Game> = emptyList(),
    // tempo medido pelo Android (recalculado a cada sync): jogo -> (dia yyyy-MM-dd -> ms)
    val tracked: Map<String, Map<String, Long>> = emptyMap(),
    // sessões registradas manualmente: jogo -> (dia -> ms)
    val manual: Map<String, Map<String, Long>> = emptyMap(),
    val ignored: Set<String> = emptySet(),
    val weeklyGoalHours: Int = 10,
    val autoDetect: Boolean = true,
    val lastSync: Long = 0L
)

fun AppData.rawMs(g: Game): Long =
    g.baselineMs + (tracked[g.id]?.values?.sum() ?: 0L) + (manual[g.id]?.values?.sum() ?: 0L)

fun AppData.totalMs(g: Game): Long = maxOf(0L, rawMs(g) + g.offsetMs)

fun AppData.dayMs(id: String, day: String): Long =
    (tracked[id]?.get(day) ?: 0L) + (manual[id]?.get(day) ?: 0L)

fun AppData.periodMs(g: Game, days: Int): Long {
    var sum = 0L
    for (d in Dates.lastDays(days)) sum += dayMs(g.id, d)
    return sum
}

fun AppData.allDayMs(day: String): Long {
    var sum = 0L
    for (g in games) sum += dayMs(g.id, day)
    return sum
}

fun AppData.allPeriodMs(days: Int): Long {
    var sum = 0L
    for (d in Dates.lastDays(days)) sum += allDayMs(d)
    return sum
}

object Json {
    private fun mapToJson(m: Map<String, Map<String, Long>>): JSONObject {
        val o = JSONObject()
        for ((k, v) in m) {
            val inner = JSONObject()
            for ((d, ms) in v) inner.put(d, ms)
            o.put(k, inner)
        }
        return o
    }

    private fun jsonToMap(o: JSONObject?): Map<String, Map<String, Long>> {
        val out = HashMap<String, Map<String, Long>>()
        if (o == null) return out
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val inner = o.getJSONObject(k)
            val m = HashMap<String, Long>()
            val ik = inner.keys()
            while (ik.hasNext()) {
                val d = ik.next()
                m[d] = inner.getLong(d)
            }
            out[k] = m
        }
        return out
    }

    fun encode(d: AppData): String {
        val o = JSONObject()
        o.put("v", 1)
        o.put("goal", d.weeklyGoalHours)
        o.put("auto", d.autoDetect)
        o.put("lastSync", d.lastSync)
        val gs = JSONArray()
        for (g in d.games) {
            val j = JSONObject()
            j.put("id", g.id)
            j.put("name", g.name)
            j.put("pkg", g.packageName ?: JSONObject.NULL)
            j.put("added", g.addedAt)
            j.put("offset", g.offsetMs)
            j.put("base", g.baselineMs)
            j.put("baseDone", g.baselineDone)
            j.put("last", g.lastPlayedAt)
            j.put("notes", g.notes)
            j.put("fav", g.favorite)
            gs.put(j)
        }
        o.put("games", gs)
        o.put("tracked", mapToJson(d.tracked))
        o.put("manual", mapToJson(d.manual))
        val ig = JSONArray()
        for (s in d.ignored) ig.put(s)
        o.put("ignored", ig)
        return o.toString()
    }

    fun decode(s: String): AppData {
        val o = JSONObject(s)
        val games = ArrayList<Game>()
        val arr = o.optJSONArray("games") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val g = arr.getJSONObject(i)
            games.add(
                Game(
                    id = g.getString("id"),
                    name = g.getString("name"),
                    packageName = if (g.isNull("pkg")) null else g.getString("pkg"),
                    addedAt = g.optLong("added"),
                    offsetMs = g.optLong("offset"),
                    baselineMs = g.optLong("base"),
                    baselineDone = g.optBoolean("baseDone"),
                    lastPlayedAt = g.optLong("last"),
                    notes = g.optString("notes", ""),
                    favorite = g.optBoolean("fav")
                )
            )
        }
        val ignored = HashSet<String>()
        val igArr = o.optJSONArray("ignored")
        if (igArr != null) for (i in 0 until igArr.length()) ignored.add(igArr.getString(i))
        return AppData(
            games = games,
            tracked = jsonToMap(o.optJSONObject("tracked")),
            manual = jsonToMap(o.optJSONObject("manual")),
            ignored = ignored,
            weeklyGoalHours = o.optInt("goal", 10),
            autoDetect = o.optBoolean("auto", true),
            lastSync = o.optLong("lastSync")
        )
    }
}
