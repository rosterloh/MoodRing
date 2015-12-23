package com.rosterloh.moodring.widgets;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 23/12/2015
 */

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import com.rosterloh.moodring.wifi.WifiRecyclerAdapter;

public class SimpleItemTouchHelperCallBack extends ItemTouchHelper.Callback {

    private WifiRecyclerAdapter wifiRecyclerAdapter = null;
    private int type;

    public SimpleItemTouchHelperCallBack(WifiRecyclerAdapter wifiadapter){
        wifiRecyclerAdapter = wifiadapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

        wifiRecyclerAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        wifiRecyclerAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

}
