/*
 * MoodRingActivity is the main Health Thermometer activity. It implements {@link MoodManagerCallbacks} to receive callbacks from {@link MoodManager} class.
 * @author   Richard Osterloh <richard.osterloh@gmail.com>
 * @version  1
 * @since    17/10/2014
 */
package com.rosterloh.moodring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import com.rosterloh.moodring.adapters.AppAdapter;
import com.rosterloh.moodring.profile.BleProfileService;
import com.rosterloh.moodring.profile.BleProfileServiceReadyActivity;
import com.rosterloh.moodring.util.AnalyticsManager;

import java.util.UUID;

import static com.rosterloh.moodring.util.LogUtils.LOGD;
import static com.rosterloh.moodring.util.LogUtils.makeLogTag;

public class MoodRingActivity extends BleProfileServiceReadyActivity<MoodService.MoodBinder> {
    private static final String TAG = makeLogTag(MoodRingActivity.class);
    private static final String SCREEN_LABEL = "MoodRing";
    private static final String UTILS_CATEGORY = "com.rosterloh.moodring.UTILS";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreateView(final Bundle savedInstanceState) {
        AnalyticsManager.initializeAnalyticsTracker(getApplicationContext());
        setContentView(R.layout.activity_moods);

        final DrawerLayout drawer = mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // Set the drawer toggle as the DrawerListener
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawer.setDrawerListener(mDrawerToggle);

        // setup plug-ins in the drawer
        //setupPluginsInDrawer((ViewGroup) drawer.findViewById(R.id.plugin_container));

        // configure the app grid
        final GridView grid = (GridView) findViewById(R.id.grid);
        grid.setAdapter(new AppAdapter(this));
        grid.setEmptyView(findViewById(android.R.id.empty));

        AnalyticsManager.sendScreenView(SCREEN_LABEL);
        LOGD("Tracker", SCREEN_LABEL);
    }

    @Override
    protected void onInitialize(final Bundle savedInstanceState) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, makeIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void setDefaultUI() {
        // restore default UI elements before reconnecting
    }

    @Override
    protected int getLoggerProfileTitle() {
        return R.string.app_name;
    }

    @Override
    protected int getDefaultDeviceName() {
        return R.string.default_device_name;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.mood_about_text;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        //getMenuInflater().inflate(R.menu.help, menu);
        return true;
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return true;
    }

    private boolean ensureBLEExists() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    protected Class<? extends BleProfileService> getServiceClass() {
        return MoodService.class;
    }

    @Override
    protected UUID getFilterUUID() {
        return MoodManager.MOOD_SERVICE_UUID;
    }

    @Override
    protected void onServiceBinded(final MoodService.MoodBinder binder) {
        // not used
    }

    @Override
    protected void onServiceUnbinded() {
        // not used
    }

    @Override
    public void onServicesDiscovered(final boolean optionalServicesFound) {
        // not used
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();

            if (MoodService.BROADCAST_MOOD_READ.equals(action)) {
                final double batt = intent.getDoubleExtra(MoodService.EXTRA_TEMPERATURE, 0);
                // Update GUI

            } else if (MoodService.BROADCAST_BATTERY_LEVEL.equals(action)) {

            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MoodService.BROADCAST_MOOD_READ);
        intentFilter.addAction(MoodService.BROADCAST_BATTERY_LEVEL);
        return intentFilter;
    }
}
