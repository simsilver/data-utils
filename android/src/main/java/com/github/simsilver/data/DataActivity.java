package com.github.simsilver.data;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.simsilver.data.core.TransCodeUtils;
import com.github.simsilver.data.helper.AHandler;
import com.github.simsilver.data.helper.Task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataActivity extends Activity implements AHandler.MessageProcessor {

    private static final String TAG = "DataActivity";

    private static final int OPEN_TXT_FILE_REQ = 0x1000;
    private static final int CREATE_TXT_FILE_REQ = 0x1001;

    private static final int MSG_SHOW_HEX = 0;
    private static final int MSG_SHOW_TOAST = 1;
    private static final int MSG_ENABLE_LOAD = 2;
    private static final int MSG_TEST_ENCODING = 3;
    private static final int MSG_SHOW_ENCODINGS = 4;
    private static final int MSG_SHOW_ENCODING = 5;

    Uri srcUri, dstUri;
    String fileTitle;

    AHandler<DataActivity> handler;
    ImageButton ibSrc, ibDst;
    TextView tvSrcUri, tvSrcHex;
    TextView tvEncodings, tvPreview;
    Button btnChangeEncoding;

    Uri tmpFile;

    private static WeakReference<Handler> sHandlerRef = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new AHandler<>(this);
        sHandlerRef = new WeakReference<>(handler);
        setContentView(R.layout.activity_data);

        ibSrc = findViewById(R.id.ib_open);
        ibSrc.setOnClickListener(view ->
                chooseSafFile(DataActivity.this, "text/*", OPEN_TXT_FILE_REQ)
        );

        tvSrcUri = findViewById(R.id.tv_src_uri);
        tvSrcHex = findViewById(R.id.tv_src_hex);
        tvEncodings = findViewById(R.id.tv_encodings);
        btnChangeEncoding = findViewById(R.id.btn_change_encoding);
        ibDst = findViewById(R.id.ib_save);
        ibDst.setOnClickListener(view ->
                chooseTargetFile(DataActivity.this, "text/*", "result.txt", CREATE_TXT_FILE_REQ)
        );
        tvPreview = findViewById(R.id.tv_preview);

        tmpFile = Uri.fromFile(new File(getCacheDir(), "tmp.txt"));
        loadUri(null);
    }


    public static void chooseSafFile(Activity activity, String mime, int reqCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);

        activity.startActivityForResult(intent, reqCode);
    }

    public static void chooseTargetFile(Activity activity, String mime, String title, int reqCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_TITLE, title);
        activity.startActivityForResult(intent, reqCode);
    }

    private void loadUri(Uri srcUri) {
        if (srcUri == null) {
            tvSrcUri.setText(R.string.file_name);
            tvSrcHex.setText(R.string.file_content_hex);
        } else {
            tvSrcUri.setText(srcUri.toString());
            ibSrc.setEnabled(false);
            FileToOp toOp = new FileToOp(srcUri, tmpFile);
            new Thread(Task.make(params -> {
                FileToOp ops = (FileToOp) params[0];
                Context context = (Context) params[1];
                String hex = readUriAsHex(context, ops.from);
                sendMessage(MSG_SHOW_HEX, hex);
                sendMessage(MSG_ENABLE_LOAD, true);
                sendMessage(MSG_TEST_ENCODING, ops);
            }, toOp, getApplicationContext())).start();
        }
    }

    private void asyncTestEncoding(FileToOp toOp) {
        new Thread(Task.make(params -> {
            FileToOp ops = (FileToOp) params[0];
            Context context = (Context) params[1];
            StringBuilder sb = new StringBuilder();
            ArrayList<String> encodings = new ArrayList<>();
            boolean result = testEncodings(context, ops.from, ops.to, sb, encodings, false);
            TestResult data = new TestResult();
            data.preview = sb.toString();
            data.encodings = encodings;
            data.result = result;
            sendMessage(MSG_SHOW_ENCODINGS, data);
        }, toOp, getApplicationContext())).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OPEN_TXT_FILE_REQ:
                if (resultCode == RESULT_OK && data != null) {
                    srcUri = data.getData();
                    loadUri(srcUri);
                }
                break;
            case CREATE_TXT_FILE_REQ:
                if (resultCode == RESULT_OK && data != null) {
                    dstUri = data.getData();
                    String encoding = btnChangeEncoding.getText().toString();
                    FileToOp toOp = new FileToOp(srcUri, dstUri);
                    new Thread(Task.make(params -> {
                        FileToOp ops = (FileToOp) params[0];
                        Context con = (Context) params[1];
                        Charset cs = Charset.forName(encoding);
                        saveAsCharsetToUri(con, ops.from, ops.to, cs, null, true);
                    }, toOp, getApplicationContext())).start();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static void sendMessage(int what, Object obj) {
        if (sHandlerRef != null) {
            Handler h = sHandlerRef.get();
            if (h != null) {
                Message.obtain(h, what, obj).sendToTarget();
            }
        }
    }

    private static String readUriAsHex(Context context, Uri srcUri) {
        try (InputStream in = context.getContentResolver().openInputStream(srcUri)) {
            byte[] buffer = new byte[1024];
            if (in != null) {
                int size = in.read(buffer);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    int v = buffer[i] & 0xFF;
                    sb.append(HEX_ARRAY[v >>> 4]).append(HEX_ARRAY[v & 0x0F]);
                }
                return sb.toString();
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private static boolean testEncodings(Context context, Uri srcUri, Uri dstUri, StringBuilder preview, ArrayList<String> list, boolean show) {
        ContentResolver cr = context.getContentResolver();
        boolean success = false;
        try (InputStream in = cr.openInputStream(srcUri);
             OutputStream out = cr.openOutputStream(dstUri, "wt")) {
            if (in != null && out != null) {
                success = TransCodeUtils.testDecodeUriStream(in, out, preview, list);
            }
            if (show) {
                sendMessage(MSG_SHOW_TOAST, "Save File " + (success ? "Done" : "Fail"));
            }
        } catch (IOException e) {
            Log.d(TAG, "readLine " + e);
        }
        return success;
    }

    private static boolean saveAsCharsetToUri(Context context, Uri srcUri, Uri dstUri, Charset cs, StringBuilder preview, boolean show) {
        ContentResolver cr = context.getContentResolver();
        boolean success = false;
        try (InputStream in = cr.openInputStream(srcUri);
             OutputStream out = cr.openOutputStream(dstUri, "wt")) {
            if (in != null && out != null) {
                success = TransCodeUtils.decodeAsCharset(cs, null, 0, in, out, preview);
            }
            if (show) {
                sendMessage(MSG_SHOW_TOAST, "Save File " + (success ? "Done" : "Fail"));
            }
        } catch (IOException e) {
            Log.d(TAG, "readLine " + e);
        }
        return success;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        sHandlerRef = null;
    }

    @Override
    public void onMessages(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_SHOW_HEX:
                tvSrcHex.setText(String.valueOf(msg.obj));
                break;
            case MSG_SHOW_TOAST:
                Toast.makeText(this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                break;
            case MSG_ENABLE_LOAD:
                if (msg.obj instanceof Boolean) {
                    ibSrc.setEnabled((Boolean) msg.obj);
                }
                break;
            case MSG_SHOW_ENCODINGS:
                if (msg.obj instanceof TestResult) {
                    TestResult data = (TestResult) msg.obj;
                    if (!data.encodings.isEmpty()) {
                        btnChangeEncoding.setEnabled(true);
                        btnChangeEncoding.setVisibility(View.VISIBLE);
                        btnChangeEncoding.setText(data.encodings.get(0));
                        btnChangeEncoding.setOnClickListener(v -> {
                            showListMenu(v, data.encodings);
                        });
                    } else {
                        btnChangeEncoding.setOnClickListener(null);
                        btnChangeEncoding.setEnabled(false);
                        btnChangeEncoding.setVisibility(View.INVISIBLE);
                    }
                    ibDst.setVisibility(data.result ? View.VISIBLE : View.INVISIBLE);
                    tvPreview.setText(data.preview);
                }
                break;
            case MSG_SHOW_ENCODING:
                if (msg.obj instanceof TestResult) {
                    TestResult data = (TestResult) msg.obj;
                    ibDst.setVisibility(data.result ? View.VISIBLE : View.INVISIBLE);
                    tvPreview.setText(data.preview);
                }
                break;
            case MSG_TEST_ENCODING:
                if (msg.obj instanceof FileToOp) {
                    asyncTestEncoding((FileToOp) msg.obj);
                }
                break;
        }
    }

    private void showListMenu(View v, ArrayList<String> encodings) {
        ListPopupWindow popupWindow = new ListPopupWindow(this);
        List<HashMap<String, String>> list = new ArrayList<>();
        String name = "Name";
        int layoutId = R.layout.item_text;
        int textViewId = R.id.tv_item_title;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        View item = getLayoutInflater().inflate(layoutId, null);
        TextView tv = item.findViewById(textViewId);
        Paint p = tv.getPaint();
        float width = 0;
        for (String encoding : encodings) {
            HashMap<String, String> map = new HashMap<>();
            map.put(name, encoding);
            list.add(map);
            float w = p.measureText(encoding);
            if (width < w) {
                width = w;
            }
        }
        popupWindow.setAdapter(
                new SimpleAdapter(this,
                        list,
                        layoutId,
                        new String[]{name},
                        new int[]{textViewId}
                ));
        popupWindow.setAnchorView(v);
        popupWindow.setWidth((int) Math.ceil(width + 20 * dm.density));
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String encoding = encodings.get(position);
            btnChangeEncoding.setText(encoding);
            FileToOp toOp = new FileToOp(srcUri, tmpFile);
            new Thread(Task.make(params -> {
                FileToOp ops = (FileToOp) params[0];
                Context context = (Context) params[1];
                StringBuilder sb = new StringBuilder();
                Charset cs = Charset.forName(encoding);
                boolean result = saveAsCharsetToUri(context, ops.from, ops.to, cs, sb, false);
                TestResult data = new TestResult();
                data.preview = sb.toString();
                data.result = result;
                sendMessage(MSG_SHOW_ENCODING, data);
            }, toOp, view.getContext())).start();
            popupWindow.dismiss();
        });
        popupWindow.show();
    }

    static class FileToOp {
        final Uri from, to;

        public FileToOp(Uri from, Uri to) {
            this.from = from;
            this.to = to;
        }

    }

    static class TestResult {
        String preview;
        ArrayList<String> encodings;
        boolean result;
    }
}