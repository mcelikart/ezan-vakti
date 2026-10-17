package com.example.ui

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Timings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NotificationHelper {
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotificationsForToday(context: Context, timings: Timings) {
        // İptal edildi: Kullanıcı 10 dakika kala bildirim göstermesin istedi.
    }
}
