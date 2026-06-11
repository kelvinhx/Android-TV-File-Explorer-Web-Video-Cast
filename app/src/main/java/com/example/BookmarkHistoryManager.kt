package com.example

import android.content.Context
import org.json.JSONArray

object BookmarkHistoryManager {
    private const val PREFS_NAME = "nexus_browser_prefs"
    private const val KEY_BOOKMARKS = "browser_bookmarks"
    private const val KEY_HISTORY = "browser_history"

    fun getBookmarks(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addBookmark(context: Context, url: String) {
        val current = getBookmarks(context).toMutableList()
        if (!current.contains(url)) {
            current.add(0, url)
            saveBookmarks(context, current)
        }
    }

    fun removeBookmark(context: Context, url: String) {
        val current = getBookmarks(context).toMutableList()
        if (current.remove(url)) {
            saveBookmarks(context, current)
        }
    }

    private fun saveBookmarks(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistory(context: Context, url: String) {
        if (url.trim().isEmpty() || url.startsWith("about:") || url.startsWith("data:") || url.contains("/api/proxy")) return
        val current = getHistory(context).toMutableList()
        current.remove(url) // avoid duplication, move to top
        current.add(0, url)
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        saveHistory(context, current)
    }

    fun clearHistory(context: Context) {
        saveHistory(context, emptyList())
    }

    private fun saveHistory(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}
