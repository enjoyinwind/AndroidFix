/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.dodola.rocoosample;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.dodola.rocoofix.RocooFix;
import com.dodola.rocoofix.SignatureChecker;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.findViewById(R.id.btnFixMe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HelloHack hack = new HelloHack();
                Toast.makeText(MainActivity.this, hack.showHello(), Toast.LENGTH_SHORT).show();
            }
        });

        this.findViewById(R.id.btnFixMe2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dexPath = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/rocoo_patch.jar");
                File file = new File(dexPath);
                SignatureChecker checker = new SignatureChecker(MainActivity.this);
                if(checker.verifyJar(file)){
                    RocooFix.applyPatchRuntime(MainActivity.this.getApplicationContext(),
                            dexPath);
                }
            }
        });
    }
}
