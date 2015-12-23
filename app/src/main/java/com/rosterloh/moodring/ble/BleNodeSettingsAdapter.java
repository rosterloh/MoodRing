package com.rosterloh.moodring.ble;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 22/12/2015
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.rosterloh.moodring.R;

import java.util.ArrayList;

public class BleNodeSettingsAdapter extends ArrayAdapter<String> {

    private Context context;
    private ArrayList<String> list;

    public BleNodeSettingsAdapter(Context context, int txtViewResourceId, ArrayList<String> list) {
        super(context, txtViewResourceId, list);
        this.context = context;
        this.list = list;
    }
    @Override
    public View getDropDownView(int position, View cnvtView, ViewGroup prnt) {
        return getCustomView(position, cnvtView, prnt);
    }
    @Override public View getView(int pos, View cnvtView, ViewGroup prnt) {

        return getCustomView(pos, cnvtView, prnt);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View spinnerView = inflater.inflate(R.layout.custom_spinner, parent, false);

        TextView main_text = (TextView) spinnerView.findViewById(R.id.tv_dropdown);
        main_text.setText(list.get(position));
        return spinnerView;
    }

    @Override
    public String getItem(int position) {
        return list.get(position);
    }
}
