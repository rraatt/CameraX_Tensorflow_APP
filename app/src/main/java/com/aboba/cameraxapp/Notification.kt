package com.aboba.cameraxapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Notification(private val applicationContext: Context) {
  private val channelID = "CameraXApp"


  init {
    val name = channelID
    val descriptionText = "CameraXApp notification channel"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(channelID, name, importance).apply {
      description = descriptionText
    }
    // Register the channel with the system.
    val notificationManager: NotificationManager =
      applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)

  }


  fun sendNotification(notificationTitle: String, notificationContent: String)
  {
    val builder = NotificationCompat.Builder(applicationContext, channelID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(notificationTitle)
      .setContentText(notificationContent)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(applicationContext)) {
      // notificationId is a unique int for each notification that you must define.
      if (ActivityCompat.checkSelfPermission(
          applicationContext,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
      }
      notify(1, builder.build())
    }


  }
}