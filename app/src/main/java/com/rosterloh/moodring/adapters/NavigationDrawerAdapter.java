package com.rosterloh.moodring.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 11/30/2014.
 */
public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.ShortcutViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    @Override
    public void onClick(View view) {
        mListener.onClick((Integer) view.getTag());
    }

    @Override
    public boolean onLongClick(View view) {
        mListener.onLongClick((Integer) view.getTag());
        return false;
    }

    public interface ClickListener {
        public abstract void onClick(int index);

        public abstract boolean onLongClick(int index);
    }

    public static int resolveColor(Context context, int color) {
        TypedArray a = context.obtainStyledAttributes(new int[]{color});
        int resId = a.getColor(0, context.getResources().getColor(R.color.red_500));
        a.recycle();
        return resId;
    }

    public NavigationDrawerAdapter(Activity context, ClickListener listener) {
        mContext = context;
        mItems = new ArrayList<>();
        mListener = listener;
        selectionColor = context.getResources().getColor(R.color.cabinet_color);
        bodyText = resolveColor(context, R.attr.body_text);
        TypedArray a = context.obtainStyledAttributes(new int[]{color});
        int resId = a.getColor(0, context.getResources().getColor(R.color.red_500));
        a.recycle();
        return resId;
    }

    private Activity mContext;
    private List<String> mItems;
    private int mCheckedPos = -1;
    private ClickListener mListener;
    private int selectionColor;
    private int bodyText;

    public static class ShortcutViewHolder extends RecyclerView.ViewHolder {

        public ShortcutViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView;
        }

        TextView title;
    }

    @Override
    public ShortcutViewHolder onCreateViewHolder(ViewGroup parent, int index) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_drawer, parent, false);
        return new ShortcutViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ShortcutViewHolder holder, int index) {
        //Pins.Item item = mItems.get(index);
        holder.title.setTag(index);
        holder.title.setOnClickListener(this);
        holder.title.setOnLongClickListener(this);
        holder.title.setActivated(mCheckedPos == index);
        holder.title.setTextColor(mCheckedPos == index ? selectionColor : bodyText);

        if (mCheckedPos == index) {
            holder.title.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        } else {
            holder.title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }

        //holder.title.setText(R.string.root);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}
