package com.flyscale.ecserver.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bian on 2018/12/6.
 */

public class ThreadPool {

    public static ExecutorService getInstance() {
        return SingletonHolder.fixedThreadPool;
    }

    private static class SingletonHolder {
        private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
    }
}
