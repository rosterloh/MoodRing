package com.rosterloh.moodring.widgets;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 23/12/2015
 */

import com.rosterloh.moodring.wifi.WifiNetwork;

public interface ItemClickHelperAdapter {

    void onWifiItemClick(WifiNetwork wifiNetwork);

    void updateUiOnItemDismiss();
}