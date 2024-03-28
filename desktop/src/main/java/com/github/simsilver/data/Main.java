package com.github.simsilver.data;

import com.github.simsilver.data.core.TransCodeUtils;
import com.github.simsilver.data.platform.PDUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    private static final String TAG = "Main";

    private static void testUriStream(File srcUri, File dstUri) {
        try (InputStream in = Files.newInputStream(srcUri.toPath());
             OutputStream out = Files.newOutputStream(dstUri.toPath())) {
            ArrayList<String> list = new ArrayList<>();
            boolean result = TransCodeUtils.testDecodeUriStream(in, out, null, list);
            out.close();
            PDUtils.logD(TAG, "Detected Encodings: %s", Arrays.toString(list.toArray()));
        } catch (IOException e) {
            PDUtils.logD(TAG, "readLine " + e);
        }
    }

    public static void main(String[] args) {
        File in = new File("in.txt");
        File out = new File("out.txt");
        testUriStream(in, out);
    }
}