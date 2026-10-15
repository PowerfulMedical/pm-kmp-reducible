@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.milanhorvatovic.reducible.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class MainThreadTest {
    @Test
    fun membership_is_true_only_on_the_main_dispatchers_thread() =
        runBlocking {
            Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
                val mainThread = withContext(dispatcher) { Thread.currentThread() }
                val main = ThreadMainDispatcher(dispatcher, mainThread)
                Dispatchers.setMain(main)
                try {
                    assertFalse(isMainThread())
                    withContext(Dispatchers.Main) {
                        assertTrue(isMainThread())
                    }
                } finally {
                    Dispatchers.resetMain()
                }
            }
        }

    @Test
    fun membership_is_true_on_any_thread_when_main_never_dispatches() =
        runBlocking {
            val callerThread = Thread.currentThread()
            Dispatchers.setMain(UnconfinedTestDispatcher())
            try {
                assertTrue(isMainThread())
                withContext(Dispatchers.Default) {
                    assertNotSame(callerThread, Thread.currentThread())
                    assertTrue(isMainThread())
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun membership_is_false_when_immediate_is_unsupported() {
        val main =
            object : MainCoroutineDispatcher() {
                override val immediate: MainCoroutineDispatcher
                    get() = throw UnsupportedOperationException("no immediate dispatcher")

                override fun dispatch(
                    context: CoroutineContext,
                    block: Runnable,
                ) = Dispatchers.Default.dispatch(context, block)
            }
        Dispatchers.setMain(main)
        try {
            assertFalse(isMainThread())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun membership_is_false_when_no_main_dispatcher_is_installed() {
        assertFalse(isMainThread())
    }
}

private class ThreadMainDispatcher(
    private val dispatcher: CoroutineDispatcher,
    private val thread: Thread,
) : MainCoroutineDispatcher() {
    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = Thread.currentThread() !== thread

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) = dispatcher.dispatch(context, block)
}
