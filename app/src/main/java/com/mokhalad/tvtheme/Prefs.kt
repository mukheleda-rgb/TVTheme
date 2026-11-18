package com.mokhalad.tvtheme

import android.content.Context

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("tvtheme", 0)

    // قراءة متسامحة مع اختلاف النوع المخزَّن (نص / منطقي) حتى لا يحدث ClassCastException
    fun s(c: Context, k: String, d: String): String = when (val v = p(c).all[k]) {
        null -> d
        is String -> v
        is Boolean -> if (v) "1" else "0"
        else -> v.toString()
    }

    fun setS(c: Context, k: String, v: String) = p(c).edit().putString(k, v).apply()

    fun b(c: Context, k: String, d: Boolean = true): Boolean = when (val v = p(c).all[k]) {
        null -> d
        is Boolean -> v
        is String -> v == "1" || v.equals("true", true)
        else -> d
    }

    fun setB(c: Context, k: String, v: Boolean) = p(c).edit().putBoolean(k, v).apply()
}
