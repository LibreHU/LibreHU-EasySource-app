package org.librehu.easysource.bridge

import android.content.Context
import org.librehu.easysource.R
import org.librehu.easysource.SourceSwitcher
import org.librehu.easysource.data.SourceStore

/**
 * The sources for LibreHU's phone app (through LibreHU-service's bridge, [BridgeKitProvider]): the source playing
 * (switched from the phone), each source on or off and its place in the order, and the switch settings. Same labels
 * and same store as the app's screen; sources are added on the head unit.
 */
class BridgeProvider : BridgeKitProvider() {
    override fun pages(context: Context): List<KitPage> {
        val store = SourceStore.get(context)
        val sources = store.sources.value
        val st = store.settings.value
        return listOf(
            page(context, "sources", context.getString(R.string.app_name), "source") {
                val active = store.active()
                if (active.isNotEmpty()) {
                    choice("current", s(R.string.sources), store.currentKey ?: "", listOf("" to "—") + active.map { it.key to it.label }) { k ->
                        active.firstOrNull { it.key == k }?.let { SourceSwitcher.activate(context, it) }
                    }
                }
                section(s(R.string.sources))
                for ((i, src) in sources.withIndex()) {
                    val id = src.key.replace(Regex("[^A-Za-z0-9]"), "_")
                    switch("on_$id", src.label, src.enabled) { v -> store.setEnabled(src.key, v) }
                    if (i > 0) button("up_$id", "↑ ${src.label}") { store.move(src.key, -1) }
                }
                section(s(R.string.switching))
                slider("delay", context.getString(R.string.confirm_delay, st.confirmDelayS), st.confirmDelayS, 1, 8, unit = "s") { v ->
                    store.updateSettings { it.copy(confirmDelayS = v) }
                }
                switch("open_app", s(R.string.open_app), st.openApp, s(R.string.open_app_hint)) { v -> store.updateSettings { it.copy(openApp = v) } }
                switch("resume_boot", s(R.string.resume_boot), st.resumeAtBoot) { v -> store.updateSettings { it.copy(resumeAtBoot = v) } }
            },
        )
    }
}
