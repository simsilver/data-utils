package com.github.simsilver.data.helper;

public class Task {
    public static Runnable make(ObjRun run, Object... params) {
        run.setParams(params);
        return run;
    }
}
