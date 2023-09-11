package com.qihoo.superbrain.voicewake2.tool;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    private static final String TAG = "ZipUtil";

    public static void unzipFromAssetsToSDCard(Context context, String zipFileName, String destinationPath) {
        try {
            InputStream inputStream = context.getAssets().open(zipFileName);
            unzip(inputStream, destinationPath);
            inputStream.close();
            Log.d(TAG, "ZIP解压成功！");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "ZIP解压失败：" + e.getMessage());
        }
    }

    private static void unzip(InputStream inputStream, String destinationPath) throws IOException {
        File folder = new File(destinationPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
        ZipEntry entry;

        while ((entry = zipInputStream.getNextEntry()) != null) {
            String entryName = entry.getName();
            File file = new File(destinationPath + File.separator + entryName);

            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                // 如果是文件，则解压文件
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[1024];
                int count;

                while ((count = zipInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count);
                }

                outputStream.close();
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();
    }
}
 
