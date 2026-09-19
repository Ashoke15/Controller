package com.diyproject.controller.devtools.macros

import org.json.JSONArray
import org.json.JSONObject

/**
 * Manual JSON (de)serialization via org.json — built into Android, so
 * this avoids pulling in a JSON library dependency for what's a small,
 * infrequently-written list. If the macro library grows large or needs
 * schema migrations, a real serialization library becomes worth it;
 * for now this keeps the footprint minimal.
 */
object MacroJson {

    fun toJsonArrayString(macros: List<Macro>): String {
        val arr = JSONArray()
        macros.forEach { arr.put(macroToJson(it)) }
        return arr.toString()
    }

    /** Throws org.json.JSONException on malformed input — callers decide
     *  the fallback (MacroStore treats it as "no macros saved yet"). */
    fun parseList(raw: String): List<Macro> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i -> macroFromJson(arr.getJSONObject(i)) }
    }

    private fun macroToJson(macro: Macro): JSONObject = JSONObject().apply {
        put("id", macro.id)
        put("name", macro.name)
        put("createdAtMs", macro.createdAtMs)
        put("updatedAtMs", macro.updatedAtMs)
        val stepsArr = JSONArray()
        macro.steps.forEach { step ->
            stepsArr.put(
                JSONObject().apply {
                    put("id", step.id)
                    put("command", step.command)
                    put("delayAfterMs", step.delayAfterMs)
                }
            )
        }
        put("steps", stepsArr)
    }

    private fun macroFromJson(obj: JSONObject): Macro {
        val stepsArr = obj.getJSONArray("steps")
        val steps = (0 until stepsArr.length()).map { i ->
            val s = stepsArr.getJSONObject(i)
            MacroStep(
                id = s.getLong("id"),
                command = s.getString("command"),
                delayAfterMs = s.getLong("delayAfterMs")
            )
        }
        return Macro(
            id = obj.getString("id"),
            name = obj.getString("name"),
            steps = steps,
            createdAtMs = obj.getLong("createdAtMs"),
            // optLong falls back gracefully for macros saved before
            // updatedAtMs existed, rather than crashing on load.
            updatedAtMs = obj.optLong("updatedAtMs", obj.getLong("createdAtMs"))
        )
    }
}