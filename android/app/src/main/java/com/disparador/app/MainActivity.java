package com.disparador.app;

import android.os.Bundle;
import android.content.Context;
import com.getcapacitor.BridgeActivity;
import java.io.*;

public class MainActivity extends BridgeActivity {
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
                
                copiarAssetPasta(context, "nodejs-project", nodeDir);

                String nodeBinary = context.getApplicationInfo().nativeLibraryDir + "/libnode.so";
                File serverFile = new File(nodeDir, "server.js");

                if (new File(nodeBinary).exists()) {
                    ProcessBuilder pb = new ProcessBuilder(nodeBinary, serverFile.getAbsolutePath());
                    pb.directory(nodeDir);
                    pb.environment().put("PORT", "3000");
                    pb.environment().put("HOME", filesDir.getAbsolutePath());
                    pb.environment().put("ANDROID_FILES_DIR", nodeDir.getAbsolutePath());
                    pb.redirectErrorStream(true);

                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        android.util.Log.d("NODEJS-SERVER", line);
                    }
                } else {
                    android.util.Log.e("NODEJS-SERVER", "Erro: libnode.so nao encontrada.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void copiarAssetPasta(Context context, String path, File destDir) {
        try {
            String[] assets = context.getAssets().list(path);
            if (assets != null && assets.length > 0) {
                destDir.mkdirs();
                for (String asset : assets) {
                    String subPath = path.isEmpty() ? asset : path + "/" + asset;
                    copiarAssetPasta(context, subPath, new File(destDir, asset));
                }
            } else {
                copiarAssetFicheiro(context, path, destDir);
            }
        } catch (Exception ignored) {}
    }

    private void copiarAssetFicheiro(Context context, String assetPath, File destFile) {
        try {
            if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }
            InputStream in = context.getAssets().open(assetPath);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception ignored) {}
    }
}
