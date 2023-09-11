package com.qihoo.superbrain.voicewake2.tool

import android.content.Context
import android.util.Log
import java.io.*

class FileUtil {

    companion object {
        private const val TAG = "FileUtil"

        fun copyFolderFromAssetsToSDCard(
            context: Context,
            sourceFolder: String,
            destinationPath: String
        ) {
            try {
                val files = context.assets.list(sourceFolder)
                if (files == null || files.isEmpty()) {
                    // 文件夹为空，直接返回
                    return
                }
                // 创建目标文件夹
                val destinationFolder = File(destinationPath)
                if (!destinationFolder.exists()) {
                    destinationFolder.mkdirs()
                }
                for (file in files) {
                    val sourceFilePath = sourceFolder + File.separator + file
                    val destinationFilePath = destinationPath + File.separator + file
                    try {
                        // 如果是文件，则拷贝文件
                        if (file.contains(".")) {
                            copyFolderFromAssetsToSDCard(
                                context,
                                sourceFilePath,
                                destinationFilePath
                            )
                        } else {
                            // 如果是文件夹，则递归调用拷贝文件夹函数
                            val inputStream = context.assets.open(sourceFilePath)
                            copyFile(inputStream, destinationFilePath)
                            inputStream.close()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(TAG, "文件拷贝失败：" + e.message)
                    }
                }
                Log.d(TAG, "文件夹拷贝成功！")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "无法获取文件列表：" + e.message)
            }
        }

        @Throws(IOException::class)
        private fun copyFile(inputStream: InputStream, destinationPath: String) {
            val outputStream: OutputStream = FileOutputStream(destinationPath)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
        }
    }

}