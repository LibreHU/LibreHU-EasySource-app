package org.librehu.easysource

import android.service.notification.NotificationListenerService

/**
 * Notification access, only so that Android hands out the media sessions (MediaSessionManager.getActiveSessions):
 * pause the other sources and play the one chosen without opening its app.
 */
class MediaListener : NotificationListenerService()
