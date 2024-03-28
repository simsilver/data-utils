package com.github.simsilver.data.platform;

class PSUtils implements PDInterface {
    public void logD(String tag, String format, Object... args) {
        System.out.println(tag + ": " + String.format(format, args));
    }
}
