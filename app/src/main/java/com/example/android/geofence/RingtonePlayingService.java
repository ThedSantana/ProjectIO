package com.example.android.geofence;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by toshiba on 1/7/2018.
 */

public class RingtonePlayingService extends Service {

    MediaPlayer mediaPlayer;

    @Nullable

    //Service binding to other component is disabled
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //Called after invoking startService from the GeofenceTransitionService
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this,"on RingtonePlayingService",Toast.LENGTH_SHORT).show();

        //create a media player
        mediaPlayer = MediaPlayer.create(this,R.raw.wakeup);
        //play the media
        mediaPlayer.start();

        return START_NOT_STICKY;
    }
}
