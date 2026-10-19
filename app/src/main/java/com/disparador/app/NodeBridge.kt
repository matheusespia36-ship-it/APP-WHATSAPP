package com.disparador.app.node

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object NodeBridge {
    private var isStarted = false
    private external fun nativeStart(arguments: Array<String>): Int

    init {
        System.loadLibrary("node")
        System.loadLibrary("native-lib")
    }

    fun start(context: Context) {
        if (isStarted) return
        isStarted = true

        val nodeDir = File(context.filesDir, "nodejs-project")
        copyAssetFolder(context, "nodejs-project", nodeDir)

        Thread {
            val mainScript = File(nodeDir, "bootstrap.js").absolutePath
            val nodeDataDir = File(context.filesDir, "nodejs-data").apply { mkdirs() }.absolutePath
            val tmpDir = File(context.cacheDir, "tmp").apply { mkdirs() }.absolutePath

            Log.i("NodeBridge", "Iniciando Node em: $mainScript")
            nativeStart(arrayOf("node", mainScript, nodeDataDir, tmpDir))
        }.start()
    }

    private fun copyAssetFolder(context: Context, srcName: String, dstDir: File) {
        val assets = context.assets.list(srcName) ?: return
        if (!dstDir.exists()) dstDir.mkdirs()

        for (asset in assets) {
            val srcPath = if (srcName.isEmpty()) asset else "$srcName/$asset"
            val dstFile = File(dstDir, asset)
            val subAssets = context.assets.list(srcPath)

            if (!subAssets.isNullOrEmpty()) {
                copyAssetFolder(context, srcPath, dstFile)
            } else {
                context.assets.open(srcPath).use { input ->
                    FileOutputStream(dstFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
