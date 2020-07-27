package com.dimonvideo.client.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.View;

import androidx.preference.PreferenceManager;

import java.lang.ref.WeakReference;

public class AsyncCountPm extends AsyncTask {
    SharedPreferences sharedPrefs;
    private WeakReference<Context> contextRef;
    private WeakReference<Activity> activity;

    public AsyncCountPm(Activity context) {
        this.contextRef = new WeakReference<>(context);
        this.activity = new WeakReference<>(context);
    }

    @Override
    protected Object doInBackground(Object[] params) {
        Context context = contextRef.get();
        if (context != null) {
            try {
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                // check is logged
                long lastCheckedMillis = sharedPrefs.getLong("dvc_once_day", 0);
                final String password = sharedPrefs.getString("dvc_password", "null");
                View view = ((Activity) context).getWindow().getDecorView().getRootView();
                SharedPreferences.Editor editor;
                editor = sharedPrefs.edit();
                long now = System.currentTimeMillis();
                long diffMillis = now - lastCheckedMillis;
                if (diffMillis >= (360000)) {
                    editor.putLong("dvc_once_day", now);
                    editor.apply();
                    NetworkUtils.checkPassword(context, view, password);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

}