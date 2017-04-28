/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.dodola.rocoosample;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import com.dodola.rocoofix.RocooFix;

import java.io.File;

/**
 * Created by sunpengfei on 16/5/24.
 */
public class RocooApplication extends Application {
    private static final String TAG = RocooApplication.class.getSimpleName();
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //打补丁
        RocooFix.init(this);
        boolean isMain = Thread.currentThread() == Looper.getMainLooper().getThread();
//        System.out.println("isMain=" + isMain);
//        try {
//            Thread.currentThread().sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("isMain=" + isMain);
//        String dexPath = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/rocoo_patch.jar");
//        if (!new File(dexPath).exists()) {
//            Log.e(TAG, dexPath + " is null");
//            return;
//        }
//        RocooFix.applyPatch(this, dexPath);
    }
}
