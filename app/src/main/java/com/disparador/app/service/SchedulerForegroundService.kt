package com.disparador.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.disparador.app.MainActivity
import com.disparador.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SchedulerForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var isExplicitlyStopped = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            isExplicitlyStopped = true
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        acquireLocks()
        _isRunning.value = true

        return START_STICKY
    }

    override fun onDestroy() {
        releaseLocks()
        _isRunning.value = false
        if (!isExplicitlyStopped) {
            Log.w("SCHEDULER-SVC", "Serviço finalizado inesperadamente. Reiniciando...")
            start(applicationContext)
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!isExplicitlyStopped) {
            Log.d("SCHEDULER-SVC", "Tarefa removida dos recentes. Reagendando serviço imediatamente...")
            try {
                val restartServiceIntent = Intent(applicationContext, SchedulerForegroundService::class.java).apply {
                    setPackage(packageName)
                }
                val pendingIntent = PendingIntent.getService(
                    applicationContext,
                    1002,
                    restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1000,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e("SCHEDULER-SVC", "Erro no onTaskRemoved ao reagendar serviço", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Disparador:SchedulerWakeLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
                Log.d("SCHEDULER-SVC", "WakeLock permanente ativo")
            }
        } catch (e: Exception) {
            Log.e("SCHEDULER-SVC", "Erro ao obter WakeLock", e)
        }

        try {
            if (wifiLock == null) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm.createWifiLock(mode, "Disparador:SchedulerWifiLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
                Log.d("SCHEDULER-SVC", "WifiLock permanente ativo")
            }
        } catch (e: Exception) {
            Log.e("SCHEDULER-SVC", "Erro ao obter WifiLock", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("SCHEDULER-SVC", "Erro ao liberar WakeLock", e)
        }

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
            wifiLock = null
        } catch (e: Exception) {
            Log.e("SCHEDULER-SVC", "Erro ao liberar WifiLock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Serviço de Disparos em Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém os envios automáticos e conexão Baileys ativos com prioridade total"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Disparador WhatsApp · Prioridade Total")
            .setContentText("Motor de agendamento e anti-duplicação ativo em 2º plano")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "disparador_scheduler_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.disparador.app.STOP_SERVICE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, SchedulerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isRunning.value = true
        }

        fun stop(context: Context) {
            val intent = Intent(context, SchedulerForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
            _isRunning.value = false
        }
    }
}
