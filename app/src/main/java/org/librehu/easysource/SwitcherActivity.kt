package org.librehu.easysource

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import org.librehu.easysource.data.Source
import org.librehu.easysource.data.SourceStore
import org.librehu.easysource.ui.CarColors
import org.librehu.easysource.ui.CarPalette
import org.librehu.easysource.ui.CarTheme
import org.librehu.easysource.ui.SwitcherOverlay
import org.librehu.easysource.ui.ThemeFollower

/**
 * Source picker over the current app, for the steering wheel: every MODE / SRC press (this activity started again)
 * or ▶▶ / ◀◀ moves to the next / previous source; the one shown becomes active after a few seconds without a key,
 * or at once with OK / play. Touch works too.
 */
class SwitcherActivity : ComponentActivity() {
    private val sources = mutableStateOf<List<Source>>(emptyList())
    private val selected = mutableIntStateOf(0)

    /** Seconds left before the selection is activated, shown as a countdown. */
    private val remaining = mutableIntStateOf(0)

    private val theme =
        lazy {
            ThemeFollower(this) { dark, accent ->
                CarColors.palette = CarPalette.of(dark, if (accent != 0) Color(accent) else null)
            }
        }

    private val tick =
        object : Runnable {
            override fun run() {
                if (remaining.intValue <= 1) {
                    confirm()
                } else {
                    remaining.intValue -= 1
                    window.decorView.postDelayed(this, 1000)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.value.start()
        val store = SourceStore.get(this)
        sources.value = store.active()
        if (sources.value.isEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        val current = SourceSwitcher.current(this)?.key ?: store.currentKey
        val i = sources.value.indexOfFirst { it.key == current }
        // The first press shows the next source: pressing MODE means "something else".
        selected.intValue = if (i < 0) 0 else (i + step(intent)).mod(sources.value.size)
        setContent {
            CarTheme {
                SwitcherOverlay(
                    sources = sources.value,
                    selected = selected.intValue,
                    remaining = remaining.intValue,
                    onSelect = { index ->
                        selected.intValue = index
                        confirm()
                    },
                    onDismiss = ::finish,
                )
            }
        }
        restartCountdown()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        move(step(intent))
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_TAB -> {
                move(1)
                true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> {
                move(-1)
                true
            }

            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> {
                confirm()
                true
            }

            else -> {
                super.onKeyDown(keyCode, event)
            }
        }

    override fun onDestroy() {
        window.decorView.removeCallbacks(tick)
        if (theme.isInitialized()) theme.value.stop()
        super.onDestroy()
    }

    private fun step(intent: Intent?) = if (intent?.action == ACTION_PREVIOUS) -1 else 1

    private fun move(delta: Int) {
        val n = sources.value.size
        if (n == 0) return
        selected.intValue = (selected.intValue + delta).mod(n)
        restartCountdown()
    }

    private fun restartCountdown() {
        window.decorView.removeCallbacks(tick)
        remaining.intValue =
            SourceStore
                .get(this)
                .settings.value.confirmDelayS
        window.decorView.postDelayed(tick, 1000)
    }

    private fun confirm() {
        window.decorView.removeCallbacks(tick)
        sources.value.getOrNull(selected.intValue)?.let { SourceSwitcher.activate(this, it) }
        finish()
    }

    companion object {
        /** Next source (also what starting this activity without an action does). */
        const val ACTION_NEXT = "org.librehu.easysource.action.NEXT"
        const val ACTION_PREVIOUS = "org.librehu.easysource.action.PREVIOUS"
    }
}
