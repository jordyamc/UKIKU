package knf.kuma.commons

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun <T> SharedPreferences.preferenceFlow(
    key: String,
    getValue: (prefs: SharedPreferences, key: String) -> T
): Flow<T> = callbackFlow {
    // emit the current value immediately
    trySend(getValue(this@preferenceFlow, key))

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
        if (changedKey == key) {
            trySend(getValue(prefs, key))
        }
    }

    registerOnSharedPreferenceChangeListener(listener)

    // called when the flow is cancelled / no longer collected
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.conflate()

fun SharedPreferences.intFlow(key: String, defValue: Int): Flow<Int> =
    preferenceFlow(key) { prefs, k -> prefs.getInt(k, defValue) }

fun SharedPreferences.stringFlow(key: String, defValue: String): Flow<String> =
    preferenceFlow(key) { prefs, k -> prefs.getString(k, defValue) ?: defValue }

fun SharedPreferences.booleanFlow(key: String, defValue: Boolean): Flow<Boolean> =
    preferenceFlow(key) { prefs, k -> prefs.getBoolean(k, defValue) }

fun SharedPreferences.floatFlow(key: String, defValue: Float): Flow<Float> =
    preferenceFlow(key) { prefs, k -> prefs.getFloat(k, defValue) }

fun SharedPreferences.longFlow(key: String, defValue: Long): Flow<Long> =
    preferenceFlow(key) { prefs, k -> prefs.getLong(k, defValue) }

fun SharedPreferences.stringSetFlow(key: String, defValue: Set<String>): Flow<Set<String>> =
    preferenceFlow(key) { prefs, k -> prefs.getStringSet(k, defValue) ?: defValue }