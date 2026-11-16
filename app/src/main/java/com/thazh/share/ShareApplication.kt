package com.thazh.share

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.thazh.share.data.AppPreferencesRepository
import com.thazh.share.data.ReceivedFilesRepository

class ShareApplication : Application() {

    lateinit var preferencesRepository: AppPreferencesRepository
        private set

    lateinit var receivedFilesRepository: ReceivedFilesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = AppPreferencesRepository(this)
        receivedFilesRepository = ReceivedFilesRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONNECTION_STATUS,
                getString(R.string.notification_channel_connection),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_connection_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RECEIVED_FILES,
                getString(R.string.notification_channel_received_files),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_received_files_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INCOMING_CONNECTION,
                getString(R.string.notification_channel_incoming_connection),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_incoming_connection_desc)
                enableVibration(true)
            }
        )
    }

    companion object {
        const val CHANNEL_CONNECTION_STATUS = "connection_status"
        const val CHANNEL_RECEIVED_FILES = "received_files"
        const val CHANNEL_INCOMING_CONNECTION = "incoming_connection"
    }
}
