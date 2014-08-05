package com.rosterloh.moodring;

import java.util.HashMap;

/**
 * Subset of Attributes used in this application
 * Created by richard.osterloh on 05/08/2014.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String BLEND_SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e";
    public static String DEV_INFO_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String BLEND_TX = "713d0003-503e-4c75-ba94-3148f18d941e";
    public static String BLEND_RX = "713d0002-503e-4c75-ba94-3148f18d941e";

    static {
        // Services.
        attributes.put(BLEND_SERVICE, "Red Bear Labs Service");
        attributes.put(DEV_INFO_SERVICE, "Device Information Service");
        // Characteristics.
        attributes.put(BLEND_TX, "Blend TX");
        attributes.put(BLEND_RX, "Blend RX");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
