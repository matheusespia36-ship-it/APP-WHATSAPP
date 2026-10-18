package com.disparador.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import com.getcapacitor.BridgeActivity;
import java.io.*;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "NODEJS-SERVER";
    private static final String CHANNEL_ID = "disparador_keep_alive";
    private PowerManager.WakeLock wakeLock;

    static {
        try {
            System.loadLibrary("node");
            System.loadLibrary("native-lib");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Bibliotecas nativas nao carregadas (ambiente de desenvolvimento/emulador)", e);
        }
    }

    public native int startNodeWithArguments(String[] arguments);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prioridade total: Foreground Service + WakeLock para não suspender Baileys e agendador
        ForegroundKeepAliveService.startService(this);
        acquireWakeLock();
        createNotificationChannel();
        requestIgnoreBatteryOptimizations();

        iniciarNodeJSInternal();
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Disparador:KeepAliveWakeLock");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire();
                Log.d(TAG, "WakeLock adquirido com sucesso. Prioridade total de 2o plano ativa.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao adquirir WakeLock", e);
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Nao foi possivel abrir solicitacao de bateria automaticamente", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Serviço Disparador em Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mantém o motor WhatsApp Baileys rodando em segundo plano sem interrupções.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void iniciarNodeJSInternal() {
        new Thread(() -> {
            try {
                Context context = getApplicationContext();
                File filesDir = context.getFilesDir();
                File nodeDir = new File(filesDir, "nodejs-project");

                File serverFile = new File(nodeDir, "bootstrap.js");
                deleteRecursive(nodeDir);
                Log.d(TAG, "Extraindo assets do Node.js para o armazenamento interno...");
                copiarAssetPasta(context, "nodejs-project", nodeDir);

                Log.d(TAG, "Iniciando thread nativa do Node.js via JNI...");
                String[] nodeArgs = new String[]{
                    "node",
                    serverFile.getAbsolutePath()
                };

                int exitCode = startNodeWithArguments(nodeArgs);
                Log.d(TAG, "Processo Node.js finalizado com codigo: " + exitCode);

            } catch (Throwable e) {
                Log.e(TAG, "Erro ao iniciar Node.js via JNI ou execução em fallback", e);
            }
        }).start();
    }

    private void copiarAssetPasta(Context context, String path, File destDir) {
        try {
            String[] assets = context.getAssets().list(path);
            if (assets != null && assets.length > 0) {
                if (!destDir.exists()) destDir.mkdirs();
                for (String asset : assets) {
                    String subPath = path.isEmpty() ? asset : path + "/" + asset;
                    copiarAssetPasta(context, subPath, new File(destDir, asset));
                }
            } else {
                copiarAssetFicheiro(context, path, destDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao copiar pasta: " + path, e);
        }
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) {
                for (File c : filhos) deleteRecursive(c);
            }
        }
        f.delete();
    }

    private void copiarAssetFicheiro(Context context, String assetPath, File destFile) {
        try {
            if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }
            InputStream in = context.getAssets().open(assetPath);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao copiar ficheiro: " + assetPath, e);
        }
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
