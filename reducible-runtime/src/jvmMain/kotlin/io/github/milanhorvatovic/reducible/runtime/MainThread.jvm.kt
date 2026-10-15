package io.github.milanhorvatovic.reducible.runtime

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Allows same-turn reduction when Main.immediate needs no dispatch from the caller: Main's
 * own thread, or any thread when Main never dispatches. Missing Main or unsupported immediate
 * returns false.
 */
internal actual fun isMainThread(): Boolean =
    try {
        !Dispatchers.Main.immediate.isDispatchNeeded(EmptyCoroutineContext)
    } catch (_: UnsupportedOperationException) {
        false
    } catch (_: IllegalStateException) {
        false
    }
