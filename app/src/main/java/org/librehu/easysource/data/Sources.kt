package org.librehu.easysource.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class SourceType {
    /** An app with its media session (music, radio, video). */
    APP,

    /** The head unit's AUX / AV input, through LibreHU-service's audio source. */
    AUX,
}

data class Source(
    val type: SourceType,
    /** Package of an [SourceType.APP] source. */
    val packageName: String = "",
    val label: String,
    val enabled: Boolean = true,
) {
    val key: String get() = if (type == SourceType.AUX) "aux" else packageName

    fun toJson(): JSONObject =
        JSONObject()
            .put("type", type.name)
            .put("pkg", packageName)
            .put("label", label)
            .put("enabled", enabled)

    companion object {
        fun fromJson(o: JSONObject) =
            Source(
                type = SourceType.entries.firstOrNull { it.name == o.optString("type") } ?: SourceType.APP,
                packageName = o.optString("pkg"),
                label = o.optString("label"),
                enabled = o.optBoolean("enabled", true),
            )
    }
}

data class SwitchSettings(
    /** Seconds without a key before the source shown in the picker is activated. */
    val confirmDelayS: Int = 3,
    /** Bring the app of the source to the front (otherwise it plays in the background when it can). */
    val openApp: Boolean = false,
    /** Start the last source again when the head unit starts. */
    val resumeAtBoot: Boolean = true,
)

/** Ordered list of sources, the current one and the switch settings. */
class SourceStore private constructor(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("sources", Context.MODE_PRIVATE)

    private val sourcesState = MutableStateFlow(load())
    val sources: StateFlow<List<Source>> = sourcesState

    private val settingsState = MutableStateFlow(loadSettings())
    val settings: StateFlow<SwitchSettings> = settingsState

    var currentKey: String?
        get() = prefs.getString(PREF_CURRENT, null)
        set(value) = prefs.edit().putString(PREF_CURRENT, value).apply()

    /** Enabled sources whose app is still installed. */
    fun active(): List<Source> = sourcesState.value.filter { it.enabled && (it.type == SourceType.AUX || isInstalled(it.packageName)) }

    fun save(list: List<Source>) {
        sourcesState.value = list
        prefs.edit().putString(PREF_SOURCES, JSONArray(list.map { it.toJson() }).toString()).apply()
    }

    fun updateSettings(transform: (SwitchSettings) -> SwitchSettings) {
        val s = transform(settingsState.value)
        settingsState.value = s
        prefs
            .edit()
            .putInt("delay", s.confirmDelayS)
            .putBoolean("open", s.openApp)
            .putBoolean("boot", s.resumeAtBoot)
            .apply()
    }

    fun move(
        key: String,
        delta: Int,
    ) {
        val list = sourcesState.value.toMutableList()
        val i = list.indexOfFirst { it.key == key }
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        list.add(j, list.removeAt(i))
        save(list)
    }

    fun remove(key: String) = save(sourcesState.value.filterNot { it.key == key })

    fun add(source: Source) {
        if (sourcesState.value.none { it.key == source.key }) save(sourcesState.value + source)
    }

    fun setEnabled(
        key: String,
        enabled: Boolean,
    ) = save(sourcesState.value.map { if (it.key == key) it.copy(enabled = enabled) else it })

    /** Media apps: those with a MediaBrowserService (as Android Auto lists them) and the music category. */
    fun mediaApps(): List<Source> {
        val pm = context.packageManager
        val pkgs = LinkedHashSet<String>()
        pm.queryIntentServices(Intent(MEDIA_BROWSER), 0).forEach { pkgs += it.serviceInfo.packageName }
        pm
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
                0,
            ).forEach { pkgs += it.activityInfo.packageName }
        return pkgs.filter { it != context.packageName }.map { Source(SourceType.APP, it, label(it)) }
    }

    /** Every app with a launcher icon, for sources the media lookup does not find. */
    fun launchableApps(): List<Source> {
        val pm = context.packageManager
        return pm
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .map { Source(SourceType.APP, it, label(it)) }
            .sortedBy { it.label.lowercase() }
    }

    fun label(pkg: String): String =
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }

    fun isInstalled(pkg: String): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun load(): List<Source> {
        prefs.getString(PREF_SOURCES, null)?.let { json ->
            runCatching {
                val a = JSONArray(json)
                return List(a.length()) { Source.fromJson(a.getJSONObject(it)) }
            }
        }
        return defaults()
    }

    /** First start: LibreHU FM, Bluetooth audio, AUX, then the media apps found. */
    private fun defaults(): List<Source> {
        val list = ArrayList<Source>()
        if (isInstalled(FM)) list += Source(SourceType.APP, FM, label(FM))
        if (isInstalled(BLUETOOTH)) list += Source(SourceType.APP, BLUETOOTH, "Bluetooth")
        list += Source(SourceType.AUX, label = "AUX")
        for (s in mediaApps()) if (list.none { it.key == s.key }) list += s
        return list
    }

    private fun loadSettings() =
        SwitchSettings(
            confirmDelayS = prefs.getInt("delay", 3),
            openApp = prefs.getBoolean("open", false),
            resumeAtBoot = prefs.getBoolean("boot", true),
        )

    companion object {
        const val FM = "org.librehu.fm"

        /** A2DP sink of Android's Bluetooth app: its media session plays the phone's music. */
        const val BLUETOOTH = "com.android.bluetooth"
        const val MEDIA_BROWSER = "android.media.browse.MediaBrowserService"
        private const val PREF_SOURCES = "sources"
        private const val PREF_CURRENT = "current"

        @Volatile
        private var instance: SourceStore? = null

        fun get(context: Context): SourceStore =
            instance ?: synchronized(this) {
                instance ?: SourceStore(context.applicationContext).also { instance = it }
            }
    }
}
