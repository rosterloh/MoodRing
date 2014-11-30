package com.rosterloh.moodring.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;

import com.melnykov.fab.FloatingActionButton;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 11/30/2014.
 */
public class DrawerActivity extends ActionBarActivity {

    public interface FabListener {
        public abstract void onFabPressed(BaseFileCab.PasteMode pasteMode);
    }

    private BaseCab mCab; // the current contextual action bar, saves state throughout fragments

    public FloatingActionButton fab; // the floating blue add/paste button
    private FabListener mFabListener; // a callback used to notify DirectoryFragment of fab press
    public BaseFileCab.PasteMode fabPasteMode = BaseFileCab.PasteMode.DISABLED;
    private boolean fabDisabled; // flag indicating whether fab should stay hidden while scrolling
    public boolean shouldAttachFab; // used during config change, tells fragment to reattach to cab
    public boolean pickMode; // flag indicating whether user is picking a file for another app
    public DrawerLayout mDrawerLayout;

    public BaseCab getCab() {
        return mCab;
    }

    public void setCab(BaseCab cab) {
        mCab = cab;
    }

    public void toggleFab(boolean hide) {
        if (fabDisabled) fab.hide(false);
        else if (hide) fab.hide(true);
        else fab.show(true);
    }

    public void disableFab(boolean disable) {
        if (!disable) {
            fab.show(true);
        } else {
            fab.hide(true);
        }
        fabDisabled = disable;
    }

    public void setFabListener(FabListener mFabListener) {
        this.mFabListener = mFabListener;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mCab != null && mCab.isActive())
            outState.putSerializable("cab", mCab);
        outState.putSerializable("fab_pastemode", fabPasteMode);
        outState.putBoolean("fab_disabled", fabDisabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else getFragmentManager().popBackStack();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("cab")) {
                mCab = (BaseCab) savedInstanceState.getSerializable("cab");
                if (mCab instanceof BaseFileCab) {
                    shouldAttachFab = true;
                } else {
                    if (mCab instanceof PickerCab) pickMode = true;
                    mCab.setContext(this).start();
                }
            }
            fabPasteMode = (BaseFileCab.PasteMode) savedInstanceState.getSerializable("fab_pastemode");
            fabDisabled = savedInstanceState.getBoolean("fab_disabled");
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFabListener != null) mFabListener.onFabPressed(fabPasteMode);
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(DrawerActivity.this, fabPasteMode == BaseFileCab.PasteMode.ENABLED ? R.string.paste : R.string.newStr, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    public void reloadNavDrawer(boolean open) {
        ((NavigationDrawerFragment) getFragmentManager().findFragmentByTag("NAV_DRAWER")).reload(open);
    }

    public void reloadNavDrawer() {
        reloadNavDrawer(false);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mCab != null && mCab.isActive()) {
                onBackPressed();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mBP.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        if (mBP != null) mBP.release();
        super.onDestroy();
    }


}
