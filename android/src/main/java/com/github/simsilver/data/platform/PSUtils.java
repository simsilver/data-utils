package com.github.simsilver.data.platform;

import android.util.Log;

import androidx.annotation.Keep;

@Keep
class PSUtils implements PDInterface {
    public void logD(String tag, String format, Object... args) {
        Log.d(tag, String.format(format, args));
    }
}
