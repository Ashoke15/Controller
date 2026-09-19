package com.diyproject.controller.devtools.macros

import android.content.Context
import android.util.Log
import org.json.JSONException

/** Persists the full macro library as one JSON array under one
 *  SharedPreferences key — the same single-key pattern CommandHistoryStore
 *  uses, sized for realistically dozens, not thousands, of macros. */
class MacroStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAll(): List<Macro> {
        val raw = prefs.getString(KEY_MACROS, null) ?: return emptyList()
        return try {
            MacroJson.parseList(raw)
        } catch (e: JSONException) {
            // Corrupt/incompatible data shouldn't crash the screen — the
            // developer loses their saved macros in this edge case, but
            // that's preferable to the app refusing to open.
            Log.e(TAG, "Failed to parse saved macros, starting empty", e)
            emptyList()
        }
    }

    fun saveAll(macros: List<Macro>) {
        prefs.edit().putString(KEY_MACROS, MacroJson.toJsonArrayString(macros)).apply()
    }

    private companion object {
        const val TAG = "MacroStore"
        const val PREFS_NAME = "macro_prefs"
        const val KEY_MACROS = "macro_library"
    }
}