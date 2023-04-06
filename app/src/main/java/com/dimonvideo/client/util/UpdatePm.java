package com.dimonvideo.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.dimonvideo.client.R;

import java.util.concurrent.Executors;

public class UpdatePm {

    public static void update(Context context, View view) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        String is_pm = sharedPrefs.getString("dvc_pm", "off");
        int auth_state = sharedPrefs.getInt("auth_state", 0);
        int pm_unread = sharedPrefs.getInt("pm_unread", 0);
        String password = sharedPrefs.getString("dvc_password", "null");

        if (auth_state > 0) {

            // обновляем счетчик лс
            TextView fab_badge = view.findViewById(R.id.fab_badge);
            Executors.newSingleThreadExecutor().execute(() -> {

                Handler mainHandler = new Handler(Looper.getMainLooper());

                mainHandler.post(() -> {
                    if (!is_pm.equals("off")) NetworkUtils.checkPassword(context, view, password);
                });

                mainHandler.post(() -> {
                    if ((pm_unread > 0) && (!is_pm.equals("off"))) {
                        fab_badge.setVisibility(View.VISIBLE);
                        fab_badge.setText(String.valueOf(pm_unread));
                    } else fab_badge.setVisibility(View.GONE);
                });

            });

        }
    }
}
