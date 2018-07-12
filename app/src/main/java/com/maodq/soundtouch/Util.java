package com.maodq.soundtouch;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {
    public static String copyAssets(Context context, String filename) {
        AssetManager assetManager = context.getResources().getAssets();
        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(filename);
            String outFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            String copyName = "test.wav";
            File outFile = new File(outFileName, copyName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
