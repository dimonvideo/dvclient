package com.dimonvideo.client.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.dimonvideo.client.R;

public class UpdatePm {

    public static void update(Context context, String razdel, View view) {


        String is_pm = AppController.getInstance().isPm();
        int auth_state = AppController.getInstance().isAuth();
        int pm_unread = AppController.getInstance().isPmUnread();
        String password = AppController.getInstance().userPassword();

        if (auth_state > 0) {

            // обновляем счетчик лс
            TextView fab_badge = view.findViewById(R.id.fab_badge);
            AppController.getInstance().getExecutor().execute(() -> {

                Handler mainHandler = new Handler(Looper.getMainLooper());

                mainHandler.post(() -> {
                    if (!is_pm.equals("off")) NetworkUtils.checkPassword(context, password, razdel);
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
