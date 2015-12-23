package com.rosterloh.moodring.settings;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 23/12/2015
 */

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class AboutPreference extends ListPreference {

    public AboutPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public AboutPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onClick() {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_about, null);

        // Create dialog
        final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        }).create();

        // Configure buttons
        view.findViewById(R.id.action_facebook).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/227282803964174"));
                final PackageManager packageManager = getContext().getPackageManager();
                final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (list.isEmpty()) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/nordicsemiconductor"));
                }
                getContext().startActivity(intent);
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.action_twitter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/NordicTweets"));
                getContext().startActivity(intent);
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.action_linkedin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("linkedin://company/23302")); // This does not work in LinkedIn 3.3.3 (the current until now)
                final PackageManager packageManager = getContext().getPackageManager();
                final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (list.isEmpty()) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://touch.www.linkedin.com/?dl=no#company/23302"));
                }
                getContext().startActivity(intent);
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.action_youtube).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/user/NordicSemi"));
                getContext().startActivity(intent);
                dialog.dismiss();
            }
        });

        // Obtain version number
        try {
            String versionName = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
            final TextView version = (TextView) view.findViewById(R.id.version);
            version.setText(getContext().getString(R.string.version, versionName));
        } catch (final Exception e) {
            // do nothing
        }

        dialog.show();
    }
}