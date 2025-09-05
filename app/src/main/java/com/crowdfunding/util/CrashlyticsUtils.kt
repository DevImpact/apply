package com.crowdfunding.util

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashlyticsUtils {
    /**
     * Logs a custom message to Crashlytics.
     */
    fun log(msg: String) = FirebaseCrashlytics.getInstance().log(msg)

    /**
     * Sets a custom key-value pair for the current crash report.
     */
    fun setKey(key: String, value: Any) = when (value) {
        is String -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Int -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Long -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Boolean -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        is Float -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        else -> FirebaseCrashlytics.getInstance().setCustomKey(key, value.toString())
    }

    /**
     * Records a non-fatal exception to Crashlytics.
     */
    fun record(e: Throwable) = FirebaseCrashlytics.getInstance().recordException(e)

    /**
     * Runs a block of code safely. If an Exception occurs, it will be recorded
     * in Crashlytics and optionally re-thrown.
     *
     * @param rethrow If true, the exception will be thrown after being recorded.
     * @param block The block of code to execute.
     */
    inline fun safeRun(rethrow: Boolean = true, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            record(t)
            if (rethrow) throw t
        }
    }
}


/**
 * A Composable modifier that makes an element clickable, with built-in
 * exception handling and debouncing to prevent rapid clicks.
 *
 * Any exception thrown within the onClick lambda will be recorded by Crashlytics.
 *
 * @param debounceMs The minimum time in milliseconds between clicks.
 * @param rethrow If true, the exception will be thrown after being recorded.
 * @param onClick The action to perform on click.
 */
fun Modifier.safeClickable(
    debounceMs: Long = 500L,
    rethrow: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableStateOf(0L) }
    this.clickable {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < debounceMs) return@clickable
        lastClickTime = now
        try {
            onClick()
        } catch (t: Throwable) {
            CrashlyticsUtils.record(t)
            if (rethrow) throw t
        }
    }
}
