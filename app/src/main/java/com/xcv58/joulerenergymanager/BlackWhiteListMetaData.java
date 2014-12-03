package com.xcv58.joulerenergymanager;

import java.util.HashMap;

/**
 * Created by xcv58 on 12/3/14.
 */
public class BlackWhiteListMetaData {
    private int globalPriority = 10;
    private int level = 100;

    private boolean rateLimitFlag;
    private HashMap<String, Integer> map;
    private static final int SET_RATE_LIMIT = 1;
    private static final int NO_RATE_LIMIT = 2;

    private int notificationId;

    private boolean isBrightnessSet;
    private int previousBrightness;
    private int previousBrightnessMode;

    public BlackWhiteListMetaData() {
        map = new HashMap<String, Integer>();
        rateLimitFlag = false;
        notificationId = 64;
        isBrightnessSet =false;
    }

    public int getGlobalPriority() {
        return globalPriority;
    }

    public int setGlobalPriority(int priority) {
        this.globalPriority = priority;
        return globalPriority;
    }

    public boolean isLevelChanged(int newLevel) {
        if (newLevel == level) {
            return false;
        }
        level = newLevel;
        return true;
    }

    public boolean alreadySet(String packagename) {
        Integer i = map.get(packagename);
        if (i == null) {
            return false;
        }
        return i == SET_RATE_LIMIT;
    }

    public void setRateLimit(String packagename) {
        map.put(packagename, SET_RATE_LIMIT);
    }

    public void removeRateLimit(String packagename) {
        map.put(packagename, NO_RATE_LIMIT);
    }

    public boolean isRateLimited() {
        return rateLimitFlag;
    }

    public boolean setRateLimitFlag(boolean b) {
        rateLimitFlag = b;
        return rateLimitFlag;
    }

    public int getNotificationId() {
        return notificationId++;
    }

    public boolean isBrightnessSet() {
        return isBrightnessSet;
    }

    public void resetBrightness() {
        isBrightnessSet = false;
    }

    public void setBrightness() {
        isBrightnessSet = true;
    }

    public void setPreviousBrightness(int brightness, int mode) {
        previousBrightness = brightness;
        previousBrightnessMode = mode;
    }

    public int getPreviousBrightness() {
        return previousBrightness;
    }

    public int getPreviousBrightnessMode() {
        return previousBrightnessMode;
    }

    public int getLowBrightness() {
        return previousBrightness / Math.max((int)Math.log(previousBrightness) - 2, 1);
    }
}
