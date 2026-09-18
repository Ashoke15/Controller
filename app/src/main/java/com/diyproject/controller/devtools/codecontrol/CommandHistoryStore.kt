package com.diyproject.controller.devtools.codecontrol

import android.content.Context

/**
 * Persists the developer's raw-command history across app restarts — an
 * ordered, deduplicated MRU list backed by SharedPreferences (no
 * database needed for what's realistically a few dozen short strings).
 * Newest first; resending an already-present command moves it to the
 * front instead of duplicating it.
 */
class CommandHistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return raw.split(UNIT_SEPARATOR).filter { it.isNotEmpty() }
    }

    fun record(command: String): List<String> {
        val updated = listOf(command) + load().filterNot { it == command }
        val capped = updated.take(MAX_ENTRIES)
        prefs.edit().putString(KEY_HISTORY, capped.joinToString(UNIT_SEPARATOR)).apply()
        return capped
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private companion object {
        const val PREFS_NAME = "code_control_prefs"
        const val KEY_HISTORY = "raw_command_history"
        const val MAX_ENTRIES = 20
        // A real \n could legitimately be part of a typed command, so a
        // control character the user can't type is used as the on-disk
        // separator instead of comma/newline.
        const val UNIT_SEPARATOR = "\u001F"
    }
}