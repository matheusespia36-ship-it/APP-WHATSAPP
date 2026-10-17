package com.disparador.app;

import android.os.Bundle;
import android.content.Context;
import android.util.Log;
import com.getcapacitor.BridgeActivity;
import java.io.*;

public class MainActivity extends BridgeActivity {

    static {
        System.loadLibrary("node");
        System.loadLibrary("native-lib");
    }

    public native int startNodeWithArguments(String[] arguments);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iniciarNodeJSInternal();
    }

    private void iniciarNodeJSInternal() {
        new Thread(() -> {
            try {
                Context context = getApplicationContext();
                File filesDir = context.getFilesDir();
                File nodeDir = new File(filesDir, "nodejs-project");

                File serverFile = new File(nodeDir, "bootstrap.js");
                    if (true) {
                       deleteRecursive(nodeDir);
                    Log.d("NODEJS-SERVER", "Extraindo assets do Node.js para o armazenamento interno...");
                    copiarAssetPasta(context, "nodejs-project", nodeDir);
                }

                Log.d("NODEJS-SERVER", "Iniciando thread nativa do Node.js via JNI...");
                String[] nodeArgs = new String[]{
                    "node",
                    serverFile.getAbsolutePath()
                };

                int exitCode = startNodeWithArguments(nodeArgs);
                Log.d("NODEJS-SERVER", "Processo Node.js finalizado com codigo: " + exitCode);

            } catch (Exception e) {
                Log.e("NODEJS-SERVER", "Erro fatal ao iniciar Node.js via JNI", e);
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
            Log.e("NODEJS-SERVER", "Erro ao copiar pasta: " + path, e);
        }
    }

    private void deleteRecursive(File f) {
    if (f.isDirectory()) {
        File[] filhos = f.listFiles();
        if (filhos != null) for (File c : filhos) deleteRecursive(c);
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
            Log.e("NODEJS-SERVER", "Erro ao copiar ficheiro: " + assetPath, e);
        }
    }
}
