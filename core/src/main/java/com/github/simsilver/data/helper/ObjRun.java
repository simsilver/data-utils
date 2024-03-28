package com.github.simsilver.data.helper;

import java.util.Map;
import java.util.WeakHashMap;

public interface ObjRun extends Runnable {
    Map<ObjRun, Object[]> map = new WeakHashMap<>();

    default Object[] getParams() {
        return map.get(this);
    }

    default void setParams(Object... params) {
        map.put(this, params);
    }

    public abstract void run(Object... params);

    @Override
    default void run() {
        run(getParams());
    }
}
