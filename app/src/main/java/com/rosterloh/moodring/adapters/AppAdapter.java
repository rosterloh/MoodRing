package com.rosterloh.moodring.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rosterloh.moodring.R;

/**
 * @author   Richard Osterloh <richard.osterloh.com>
 * @version  1
 * @since    17/10/2014.
 */
public class AppAdapter extends BaseAdapter {
    private static final String CATEGORY = "com.rosterloh.moodring.LAUNCHER";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final String[] mMoods;
    private final int[] mImages;

    public AppAdapter(final Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);

        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(CATEGORY);

        String[] moods = {
            "Super Happy",
            "Happy",
            "Sheepish",
            "Grumpy",
            "Confused",
            "Shocked",
            "Angry",
            "Cool",
            "Worried"
        } ;
        this.mMoods = moods;
        int[] images = {
            R.drawable.super_happy,
            R.drawable.happy,
            R.drawable.sheepish,
            R.drawable.grumpy,
            R.drawable.confused,
            R.drawable.shocked,
            R.drawable.angry,
            R.drawable.cool,
            R.drawable.worried
        };
        this.mImages = images;
    }

    @Override
    public int getCount() {
        return mMoods.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0; //position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.feature_icon, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.view = view;
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.label = (TextView) view.findViewById(R.id.label);
            view.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.icon.setImageResource(mImages[position]);
        holder.label.setText(mMoods[position]);
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //final Intent intent = new Intent();
                //intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                //intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                //mContext.startActivity(intent);
            }
        });

        return view;
    }

    private class ViewHolder {
        private View view;
        private ImageView icon;
        private TextView label;
    }
}
