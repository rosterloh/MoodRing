/*
 * MoodRingActivity is the main Health Thermometer activity. It implements {@link MoodManagerCallbacks} to receive callbacks from {@link MoodManager} class.
 * @author   Richard Osterloh <richard.osterloh@gmail.com>
 * @version  1
 * @since    17/10/2014
 */
package com.rosterloh.moodring;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import com.rosterloh.moodring.profile.BleProfileServiceReadyActivity;
import com.rosterloh.moodring.util.AnalyticsManager;

import static com.rosterloh.moodring.util.LogUtils.LOGD;
import static com.rosterloh.moodring.util.LogUtils.makeLogTag;

public class MoodRingActivity extends Activity, BleProfileServiceReadyActivity<MoodService.RSCBinder> {
    private static final String TAG = makeLogTag(MoodRingActivity.class);
    private static final String SCREEN_LABEL = "MoodRing";
    private static final String UTILS_CATEGORY = "com.rosterloh.moodring.UTILS";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsManager.initializeAnalyticsTracker(getApplicationContext());
        setContentView(R.layout.activity_moods);

        // ensure that Bluetooth exists
        if (!ensureBLEExists())
            finish();

        final DrawerLayout drawer = mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // Set the drawer toggle as the DrawerListener
        drawer.setDrawerListener(mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close));

        // setup plug-ins in the drawer
        //setupPluginsInDrawer((ViewGroup) drawer.findViewById(R.id.plugin_container));

        // configure the app grid
        final GridView grid = (GridView) findViewById(R.id.grid);
        grid.setAdapter(new AppAdapter(this));
        grid.setEmptyView(findViewById(android.R.id.empty));

        getActionBar().setDisplayHomeAsUpEnabled(true);

        AnalyticsManager.sendScreenView(SCREEN_LABEL);
        LOGD("Tracker", SCREEN_LABEL);
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
}
