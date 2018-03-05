package com.example.android.geofence;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by toshiba on 9/1/2017.
 */

public class GeofenceTransitionService extends IntentService {

    private static final String TAG = GeofenceTransitionService.class.getSimpleName();
    public static final int GEOFENCE_NOTIFICATION_ID = 0;
    MediaPlayer mediaPlayer;

    public GeofenceTransitionService(){

        super(TAG);
    }

//    @Override
//    public void onCreate() {
//
//        mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.wakeup );
//        //play audio
//
//        mediaPlayer.start();
//
//    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Retrieve the Geofencing intent
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        //Handle errors
        if( geofencingEvent.hasError()){
            String errorMsg = getErrorString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMsg);
            return;
        }


        //Retrieve GeofenceTransition
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        //Check for the transition type
        if( geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            //Get the geofence that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            //Create a detail message with Geofences received
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences);
            //Send notification details as a String
            sendNotification(geofenceTransitionDetails);

            //call the ringtone service to play the alarm
            Intent serviceIntent = new Intent(this, RingtonePlayingService.class);
            this.startService(serviceIntent);

            Toast.makeText(this,"Has entered the geofennce",Toast.LENGTH_SHORT).show();
        }





    }

    //Create a detailed message with Geofences received
    private String getGeofenceTransitionDetails(int geoFenceTransition, List<Geofence> triggeringGeofences){
        //get the ID of each geofence trigerred
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();
        for (Geofence geofence: triggeringGeofences){

            triggeringGeofencesList.add(geofence.getRequestId());

        }

        String status = null;
        if( geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
            status = "Entering";
        else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
            status = "Exiting";
        return status + TextUtils.join(", ", triggeringGeofencesList);

    }

    //Send notification
    private void sendNotification (String msg){
        Log.i(TAG, "sendNotification: " + msg);

        //Intent to start the main Activity
        Intent notificationIntent = MainActivity.makeNotificationIntent(
                getApplicationContext(), msg
        );

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        //Creating and sending Notification
        NotificationManager notificationMng =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationMng.notify(
                GEOFENCE_NOTIFICATION_ID,
                createNotification(msg, notificationPendingIntent)
        );

    }

    //Create a notification
    private Notification createNotification(String msg, PendingIntent notificationPendingIntent){
        NotificationCompat.Builder notificationBuilder  = new NotificationCompat.Builder(this);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_action_location)
                .setColor(Color.RED)
                .setContentTitle(msg)
                .setContentText("Geofence Notification!")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
         return notificationBuilder.build();

    }

    //Handle errors
    private static String getErrorString(int errorCode){
        switch (errorCode){

            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many Goefences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";


        }
    }




}
