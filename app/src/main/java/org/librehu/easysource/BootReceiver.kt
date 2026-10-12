package org.librehu.easysource

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import org.librehu.easysource.data.SourceStore

/** Starts the last source again at boot (option), once the media apps had time to start. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = SourceStore.get(context)
        if (!store.settings.value.resumeAtBoot) return
        val source = store.active().firstOrNull { it.key == store.currentKey } ?: return
        val pending = goAsync()
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                SourceSwitcher.activate(context.applicationContext, source)
            } finally {
                pending.finish()
            }
        }, BOOT_DELAY_MS)
    }

    private companion object {
        const val BOOT_DELAY_MS = 8000L
    }
}
