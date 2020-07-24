package com.dimonvideo.client;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String filename = remoteMessage.getData().get("title");
            String text = remoteMessage.getData().get("text");

            if (action != null && !action.isEmpty() && action.equals("delete")) {



            }

            if (action != null && !action.isEmpty() && action.equals("new")) {
                Log.d("tag", "Refreshed token: " + filename);


            }

        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("tag", "Refreshed token: " + token);
    }
}
