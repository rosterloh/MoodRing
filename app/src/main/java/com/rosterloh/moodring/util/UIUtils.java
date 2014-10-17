package com.rosterloh.moodring.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 17/10/2014
 */
public class UIUtils {
    public static void showToastLong(Context cxt, int resid) {
        Toast.makeText(cxt, resid, Toast.LENGTH_LONG).show();
    }

    public static void showToastShort(Context cxt, int resid) {
        Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
    }

    public static void showToastLong(Context cxt, String msg) {
        Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show();
    }

    public static void showToastShort(Context cxt, String msg) {
        Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
    }

    public interface OnMultiChoiceDialogListener {
        void onClick(boolean[] selected);
    }

    public static Dialog createMultiChoiceDialog(Context cxt,
                                                 CharSequence[] keys, boolean[] values, Integer titleId,
                                                 Integer iconId, final OnMultiChoiceDialogListener listener) {
        final boolean[] res;

        if (values == null) {
            res = new boolean[keys.length];
        } else {
            res = values;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);

        if (iconId != null) {
            builder.setIcon(iconId);
        }

        if (titleId != null) {
            builder.setTitle(titleId);
        }

        builder.setMultiChoiceItems(keys, values,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton, boolean isChecked) {
                        res[whichButton] = isChecked;
                    }
                });

        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        listener.onClick(res);
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    public static void showDialog(Context cxt, int titleid, int msgid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(titleid);
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static void showDialog(Context cxt, int titleid, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(titleid);
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static void showConfirmationDialog(Context cxt, int msgid, DialogInterface.OnClickListener oklistener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        // builder.setTitle(cxt.getPackageName());
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, oklistener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static void showConfirmationDialog(Context cxt, int msgid, DialogInterface.OnClickListener oklistener,
                                              int titleid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        // builder.setTitle(cxt.getPackageName());
        builder.setTitle(titleid);
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, oklistener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }
}
