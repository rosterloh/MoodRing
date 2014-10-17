package com.rosterloh.moodring;

import com.rosterloh.moodring.profile.BleManagerCallbacks;

/**
 * /**
 * Interface {@link MoodManagerCallbacks} must be implemented by {@link MoodRingActivity} in order to receive callbacks from {@link com.rosterloh.moodring.MoodManager}
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 17/10/2014
 */
public interface MoodManagerCallbacks extends BleManagerCallbacks {
    /**
     * Called when Mood Ring value has been received
     *
     * @param value
     *            the new value
     */
    public void onMoodValueReceived(double value);
}
