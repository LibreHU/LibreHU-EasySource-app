package org.librehu.easysource

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import org.librehu.easysource.ui.CarColors
import org.librehu.easysource.ui.CarPalette
import org.librehu.easysource.ui.CarTheme
import org.librehu.easysource.ui.SettingsScreen
import org.librehu.easysource.ui.ThemeFollower

/** Sources (order, on / off, add), switch settings and how to put EasySource on a steering wheel key. */
class MainActivity : ComponentActivity() {
    private val theme =
        lazy {
            ThemeFollower(this) { dark, accent ->
                CarColors.palette = CarPalette.of(dark, if (accent != 0) Color(accent) else null)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.value.start()
        setContent {
            CarTheme {
                SettingsScreen(
                    openSwitcher = { startActivity(Intent(this, SwitcherActivity::class.java)) },
                    openNotificationAccess = {
                        runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        theme.value.stop()
        super.onDestroy()
    }
}
