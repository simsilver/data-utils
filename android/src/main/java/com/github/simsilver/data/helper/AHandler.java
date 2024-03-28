package com.github.simsilver.data.helper;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public class AHandler<T extends Activity & AHandler.MessageProcessor> extends Handler {
    final WeakReference<T> actRef;

    public AHandler(T activity) {
        super(activity.getMainLooper());
        actRef = new WeakReference<>(activity);
    }

    public void run(Runnable runnable) {
        runDelay(runnable, 0);
    }

    public void runDelay(Runnable runnable, long ms) {
        T activity = actRef.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            super.postDelayed(runnable, ms);
        }
    }


    @Override
    public void handleMessage(@NonNull Message msg) {
        T activity = actRef.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.onMessages(msg);
        }
    }

    public interface MessageProcessor {
        void onMessages(@NonNull Message msg);
    }
}
