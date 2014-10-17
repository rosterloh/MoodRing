package com.rosterloh.moodring;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.rosterloh.moodring.profile.BleManager;
import com.rosterloh.moodring.profile.BleProfileService;

import static com.rosterloh.moodring.util.LogUtils.LOGI;
import static com.rosterloh.moodring.util.LogUtils.makeLogTag;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 17/10/2014
 */
public class MoodService extends BleProfileService implements MoodManagerCallbacks {
    private static final String TAG = makeLogTag(MoodService.class);
    public static final String BROADCAST_MOOD_READ = "com.rosterloh.moodring.BROADCAST_HTS_MEASUREMENT";
    public static final String EXTRA_TEMPERATURE = "com.rosterloh.moodring.EXTRA_TEMPERATURE";

    private final static String ACTION_DISCONNECT = "com.rosterloh.moodring.ACTION_DISCONNECT";

    private final static int NOTIFICATION_ID = 267;
    private final static int OPEN_ACTIVITY_REQ = 0;
    private final static int DISCONNECT_REQ = 1;

    private MoodManager mManager;
    private boolean mBound;

    private final LocalBinder mBinder = new RSCBinder();

    /**
     * This local binder is an interface for the bound activity to operate with the Mood Ring
     */
    public class RSCBinder extends LocalBinder {
        // empty
    }

    @Override
    protected LocalBinder getBinder() {
        return mBinder;
    }

    @Override
    protected BleManager<MoodManagerCallbacks> initializeManager() {
        return mManager = new MoodManager();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DISCONNECT);
        registerReceiver(mDisconnectActionBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        // when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
        cancelNotification();
        unregisterReceiver(mDisconnectActionBroadcastReceiver);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        mBound = true;
        return super.onBind(intent);
    }

    @Override
    public void onRebind(final Intent intent) {
        mBound = true;
        // when the activity rebinds to the service, remove the notification
        cancelNotification();

        // read the battery level when back in the Activity
        if (isConnected())
            mManager.readBatteryLevel();
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        mBound = false;
        // when the activity closes we need to show the notification that user is connected to the sensor
        createNotifcation(R.string.mood_notification_connected_message, 0);
        return super.onUnbind(intent);
    }

    @Override
    public void onMoodValueReceived(final double value) {
        final Intent broadcast = new Intent(BROADCAST_MOOD_READ);
        broadcast.putExtra(EXTRA_TEMPERATURE, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    /**
     * Creates the notification
     *
     * @param messageResId
     *            the message resource id. The message must have one String parameter,<br />
     *            f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>
     * @param defaults
     *            signals that will be used to notify the user
     */
    private void createNotifcation(final int messageResId, final int defaults) {
        final Intent parentIntent = new Intent(this, MoodRingActivity.class);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Intent targetIntent = new Intent(this, MoodRingActivity.class);

        final Intent disconnect = new Intent(ACTION_DISCONNECT);
        final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);

        // both activities above have launchMode="singleTask" in the AndoridManifest.xml file, so if the task is already running, it will be resumed
        final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[] { parentIntent, targetIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
        final Notification.Builder builder = new Notification.Builder(this).setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(messageResId, getDeviceName()));
        builder.setSmallIcon(R.drawable.ic_stat_notify);
        builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);
        builder.addAction(R.drawable.ic_action_bluetooth, getString(R.string.mood_notification_action_disconnect), disconnectAction);

        final Notification notification = builder.build();
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Cancels the existing notification. If there is no active notification this method does nothing
     */
    private void cancelNotification() {
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    /**
     * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
     */
    private BroadcastReceiver mDisconnectActionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            LOGI(TAG, "Disconnect action pressed");
            if (isConnected())
                getBinder().disconnect();
            else
                stopSelf();
        }
    };
}
