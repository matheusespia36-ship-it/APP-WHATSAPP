package com.disparador.app;

import android.os.Bundle;
import android.content.Context;
import com.getcapacitor.BridgeActivity;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

                File serverFile = new File(nodeDir, "server.js");
                descompactarAssetZip(context, "nodejs-project.zip", nodeDir);

                String nodeBinary = context.getApplicationInfo().nativeLibraryDir + "/libnode.so";

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
                android.util.Log.e("NODEJS-SERVER", "Erro: " + e.getMessage(), e);
            }
        }).start();
    }

    private void descompactarAssetZip(Context context, String assetZipName, File destDir) throws IOException {
        if (!destDir.exists()) destDir.mkdirs();
        InputStream is = context.getAssets().open(assetZipName);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        byte[] buffer = new byte[8192];
        while ((ze = zis.getNextEntry()) != null) {
            File file = new File(destDir, ze.getName());
            if (ze.isDirectory()) {
                file.mkdirs();
            } else {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
            }
            zis.closeEntry();
        }
        zis.close();
    }
}
