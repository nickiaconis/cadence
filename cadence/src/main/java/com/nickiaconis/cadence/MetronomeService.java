package com.nickiaconis.cadence;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class MetronomeService extends Service {

    private final int NO_ACTION = 0;
    private final int START_ACTION = 1;
    private final int STOP_ACTION = 2;
    private final int TEMPO_ACTION = 4;

    private final int MILLIS_PER_MINUTE = 60 * 1000;
    private final int PORTION_OF_BEAT_TO_VIBRATE = 4;
    private final int NANOS_PER_MILLIS = 1_000_000;

    private final int NOTIFICATION_ID = 421;

    private boolean mIsRunning;
    private int mBeatsPerMinute;
    private NotificationCompat.Builder mNotificationBuilder;
    private Vibrator mVibrator;
    private Thread mPulseThread = new Thread() {
        private final int PORTION_TO_SLEEP = 2;

        private long computeDelayMillis() {
            return (long)MILLIS_PER_MINUTE / (long)mBeatsPerMinute;
        }

        private long computeDelayNanos() {
            return computeDelayMillis() * (long)NANOS_PER_MILLIS;
        }

        public void run() {
            long wakeTime = System.nanoTime();

            while (mIsRunning) {
                long now = System.nanoTime();

                // Spin CPU unless it's time to wake up
                if (now >= wakeTime) {
                    long nanosToDelay = computeDelayNanos();
                    wakeTime += nanosToDelay;

                    // Vibrate
                    long vibrateTime = computeDelayMillis() / (long)PORTION_OF_BEAT_TO_VIBRATE;
                    mVibrator.vibrate(vibrateTime);

                    // If we've lagged behind, jump ahead
                    if (wakeTime + nanosToDelay < now) {
                        wakeTime = now + nanosToDelay;
                        // TODO: show notification that we've lagged maybe?
                    }

                    // Sleep for a portion of the time between now and next wake
                    try {
                        long nanosUntilNextWake = Math.max(0, wakeTime - System.nanoTime());
                        long nanosToSleep = nanosUntilNextWake / (long) PORTION_TO_SLEEP;
                        long millisToSleep = nanosToSleep / (long)NANOS_PER_MILLIS;
                        sleep(millisToSleep);
                    } catch (InterruptedException e) {}
                }
            }
        }
    };

    public MetronomeService() {
    }

    @Override
    public void onCreate() {
        mIsRunning = false;
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_stat_cadence)
                .setLargeIcon(
                        BitmapFactory.decodeResource(
                                getResources(),
                                R.drawable.ic_launcher
                        )
                )
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(
                                        this,
                                        MetronomeActivity.class
                                ),
                                0
                        )
                )
                .setOngoing(true)
                .addAction(
                        android.R.drawable.ic_media_pause,
                        getResources().getString(R.string.stop),
                        PendingIntent.getService(
                                this,
                                0,
                                new Intent(
                                        this,
                                        MetronomeService.class
                                )
                                        .setAction(Constants.ACTION.STOP_ACTION),
                                0
                        )
                );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int action = NO_ACTION;

        // Determine what action to take
        switch (intent.getAction()) {
            case Constants.ACTION.TOGGLE_ACTION:
                if (!mIsRunning) {
                    action = START_ACTION | TEMPO_ACTION;
                    break;
                }
            case Constants.ACTION.STOP_ACTION:
                action = STOP_ACTION;
                break;
            case Constants.ACTION.SET_TEMPO_ACTION:
                action = TEMPO_ACTION;
                break;
        }

        // Set the tempo when starting or when setting a new tempo
        if ((action & TEMPO_ACTION) > 0) {
            int newTempo = intent.getIntExtra(Constants.KEY.TEMPO_KEY, -1);
            if (newTempo != -1) {
                mBeatsPerMinute = newTempo;
                updateNotificationIfRunning();
            }
        }

        // Start the service
        if ((action & START_ACTION) > 0) {
            startForeground(NOTIFICATION_ID, buildNotification());
            startPulsing();
        }

        // Stop the service
        if ((action & STOP_ACTION) > 0) {
            stopPulsing();
            stopForeground(true);
            stopSelf();
        }

        return mIsRunning ? START_STICKY : START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startPulsing() {
        if (Build.VERSION.SDK_INT >= 11) {
            if (!mVibrator.hasVibrator()) {
                Toast.makeText(getApplicationContext(), R.string.must_have_vibrator, Toast.LENGTH_LONG);
                return;
            }
        }
        mIsRunning = true;
        mPulseThread.start();
    }

    private void stopPulsing() {
        mIsRunning = false;
        try {
            mPulseThread.join();
        } catch (InterruptedException e) {}
    }

    private Notification buildNotification() {
        mNotificationBuilder.setContentText(
                String.format(
                        getResources().getString(R.string.running_at_bpm),
                        mBeatsPerMinute
                )
        );
        return mNotificationBuilder.build();
    }

    private void updateNotificationIfRunning() {
        if (mIsRunning) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification()
            );
        }
    }
}
