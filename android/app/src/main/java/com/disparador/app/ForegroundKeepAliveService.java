package com.disparador.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

public class ForegroundKeepAliveService extends Service {

    private static final String TAG = "DISPARADOR-SERVICE";
    public static final String CHANNEL_ID = "disparador_keep_alive_channel";
    public static final int NOTIFICATION_ID = 2024;
    public static final String ACTION_STOP = "com.disparador.app.ACTION_STOP";

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private boolean isExplicitlyStopped = false;

    public static void startService(Context context) {
        try {
            Intent intent = new Intent(context, ForegroundKeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            Log.d(TAG, "ForegroundKeepAliveService iniciado com sucesso");
        } catch (Exception e) {
            Log.e(TAG, "Falha ao iniciar ForegroundKeepAliveService", e);
        }
    }

    public static void stopService(Context context) {
        try {
            Intent intent = new Intent(context, ForegroundKeepAliveService.class);
            intent.setAction(ACTION_STOP);
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao parar ForegroundKeepAliveService", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireLocks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            isExplicitlyStopped = true;
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);
        acquireLocks();

        // START_STICKY garante que se o sistema matar o serviço em memória extrema,
        // ele será recriado automaticamente assim que a memória for liberada.
        return START_STICKY;
    }

    private void acquireLocks() {
        try {
            if (wakeLock == null) {
                PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Disparador:PersistentWakeLock");
                    wakeLock.setReferenceCounted(false);
                }
            }
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
                Log.d(TAG, "WakeLock permanente adquirido (CPU ativa 24/7)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao adquirir WakeLock", e);
        }

        try {
            if (wifiLock == null) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    int mode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q 
                        ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY 
                        : WifiManager.WIFI_MODE_FULL_HIGH_PERF;
                    wifiLock = wm.createWifiLock(mode, "Disparador:PersistentWifiLock");
                    wifiLock.setReferenceCounted(false);
                }
            }
            if (wifiLock != null && !wifiLock.isHeld()) {
                wifiLock.acquire();
                Log.d(TAG, "WifiLock permanente adquirido (Rede sem suspensão)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao adquirir WifiLock", e);
        }
    }

    private void releaseLocks() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            wakeLock = null;
        } catch (Exception ignored) {}

        try {
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
            }
            wifiLock = null;
        } catch (Exception ignored) {}
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Motor Disparador em Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Garante prioridade máxima para o agendador e conexão WhatsApp funcionarem continuamente.");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_MAX);
        }

        int iconRes = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
        if (iconRes == 0) {
            iconRes = android.R.drawable.ic_dialog_info;
        }

        builder.setContentTitle("Disparador WhatsApp · Prioridade Total")
            .setContentText("Motor de mensagens e conexão ativa em segundo plano")
            .setSmallIcon(iconRes)
            .setContentIntent(pendingIntent)
            .setOngoing(true);

        return builder.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (!isExplicitlyStopped) {
            Log.d(TAG, "App removido dos recentes. Agendando reinício imediato do serviço...");
            try {
                Intent restartServiceIntent = new Intent(getApplicationContext(), ForegroundKeepAliveService.class);
                restartServiceIntent.setPackage(getPackageName());
                PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    1,
                    restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
                );
                AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                if (alarmService != null) {
                    alarmService.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 1000,
                        restartServicePendingIntent
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro no onTaskRemoved ao reagendar serviço", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        releaseLocks();
        if (!isExplicitlyStopped) {
            Log.w(TAG, "Serviço destruído inesperadamente. Tentando reiniciar...");
            startService(getApplicationContext());
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
