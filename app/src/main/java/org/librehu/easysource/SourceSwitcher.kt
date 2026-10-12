package org.librehu.easysource

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import org.librehu.easysource.data.Source
import org.librehu.easysource.data.SourceStore
import org.librehu.easysource.data.SourceType

/**
 * Makes a source the one heard: pauses the other media sessions, switches LibreHU-service's audio input (Android /
 * AUX), then starts the source's app (its media session, or the app itself when it has none yet).
 */
object SourceSwitcher {
    private const val TAG = "EasySource"
    private const val SERVICE = "org.librehu.service"
    private const val ACTION_AUDIO_SOURCE = "org.librehu.service.AUDIO_SOURCE"
    private const val PLAY_DELAY_MS = 1500L

    private val main = Handler(Looper.getMainLooper())

    fun activate(
        context: Context,
        source: Source,
    ) {
        val store = SourceStore.get(context)
        store.currentKey = source.key
        val sessions = sessions(context)
        // Everything else stops: the source chosen is the only one heard.
        for (c in sessions) if (c.packageName != source.packageName && c.isPlaying()) c.transportControls.pause()
        audioInput(context, aux = source.type == SourceType.AUX)
        if (source.type == SourceType.AUX) return
        val controller = sessions.firstOrNull { it.packageName == source.packageName }
        val launch = context.packageManager.getLaunchIntentForPackage(source.packageName)
        if (launch != null && (store.settings.value.openApp || controller == null)) {
            try {
                context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
            } catch (e: Exception) {
                Log.w(TAG, "launch ${source.packageName}: ${e.message}")
            }
        }
        if (controller != null) {
            controller.transportControls.play()
        } else {
            // The app creates its session once started: play then, through its session or the media key.
            main.postDelayed({
                val c = sessions(context).firstOrNull { it.packageName == source.packageName }
                if (c != null) c.transportControls.play() else mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
            }, PLAY_DELAY_MS)
        }
    }

    /** Next / previous enabled source after the current one. */
    fun neighbour(
        context: Context,
        delta: Int,
        from: String? = SourceStore.get(context).currentKey,
    ): Source? {
        val list = SourceStore.get(context).active()
        if (list.isEmpty()) return null
        val i = list.indexOfFirst { it.key == from }
        val j = if (i < 0) (if (delta > 0) 0 else list.size - 1) else Math.floorMod(i + delta, list.size)
        return list[j]
    }

    /** Source heard now: the playing session if it is one of the list, else the last one chosen. */
    fun current(context: Context): Source? {
        val list = SourceStore.get(context).active()
        val playing = sessions(context).firstOrNull { it.isPlaying() }?.packageName
        return list.firstOrNull { it.packageName == playing && it.type == SourceType.APP }
            ?: list.firstOrNull { it.key == SourceStore.get(context).currentKey }
    }

    /** True when Android lets this app see the media sessions (notification access). */
    fun hasSessionAccess(context: Context): Boolean =
        try {
            context
                .getSystemService(MediaSessionManager::class.java)
                .getActiveSessions(ComponentName(context, MediaListener::class.java))
            true
        } catch (_: SecurityException) {
            false
        }

    private fun sessions(context: Context): List<MediaController> =
        try {
            context
                .getSystemService(MediaSessionManager::class.java)
                .getActiveSessions(ComponentName(context, MediaListener::class.java))
        } catch (_: SecurityException) {
            emptyList()
        }

    private fun MediaController.isPlaying() = playbackState?.state == PlaybackState.STATE_PLAYING

    /** LibreHU-service: audio chip input (AUX or Android). Without the service nothing happens. */
    private fun audioInput(
        context: Context,
        aux: Boolean,
    ) {
        val intent =
            Intent(ACTION_AUDIO_SOURCE)
                .setPackage(SERVICE)
                .setClassName(SERVICE, "$SERVICE.LibreHuService")
                .putExtra("source", if (aux) "aux" else "android")
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // In the background (boot), Android 8+ only allows a foreground service start.
            runCatching { context.startForegroundService(intent) }.onFailure { Log.i(TAG, "audio source: ${e.message}") }
        }
    }

    private fun mediaKey(
        context: Context,
        code: Int,
    ) {
        val am = context.getSystemService(AudioManager::class.java)
        val now = SystemClock.uptimeMillis()
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
    }
}
