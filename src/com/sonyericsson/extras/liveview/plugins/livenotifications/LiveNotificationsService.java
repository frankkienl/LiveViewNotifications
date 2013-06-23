/*
 * Copyright (c) 2010 Sony Ericsson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.sonyericsson.extras.liveview.plugins.livenotifications;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import java.lang.reflect.Method;

/**
 * Hello World service.
 *
 * Will send a notification to LiveView every time a button is pressed on the
 * device.
 */
public class LiveNotificationsService extends AbstractPluginService {

    // Our handler.
    private Handler mHandler = null;
    // Counter
    private int mCounter = 1;
    // Is loop running?
    private boolean mWorkerRunning = false;
    // Preferences - update interval
    private static final String UPDATE_INTERVAL = "updateInterval";
    private long mUpdateInterval = 60000;
    private static LiveNotificationsService instance;

    public static LiveNotificationsService getInstance() {
        return instance;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        instance = this;
        // Create handler.
        if (mHandler == null) {
            mHandler = new Handler();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // ... 
        // Do plugin specifics.
        // ...
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // ... 
        // Do plugin specifics.
        // ...
    }

    /**
     * Plugin is just sending notifications.
     */
    protected boolean isSandboxPlugin() {
        return false;
    }

    /**
     * Must be implemented. Starts plugin work, if any.
     */
    protected void startWork() {
        // Check if plugin is enabled.
        if (!mWorkerRunning && mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
            mWorkerRunning = true;
            //scheduleTimer();
        }
    }

    /**
     * Must be implemented. Stops plugin work, if any.
     */
    protected void stopWork() {
        mHandler.removeCallbacks(mAnnouncer);
        mWorkerRunning = false;
    }

    /**
     * Must be implemented.
     *
     * PluginService has done connection and registering to the LiveView
     * Service.
     *
     * If needed, do additional actions here, e.g. starting any worker that is
     * needed.
     */
    protected void onServiceConnectedExtended(ComponentName className, IBinder service) {
    }

    /**
     * Must be implemented.
     *
     * PluginService has done disconnection from LiveView and service has been
     * stopped.
     *
     * Do any additional actions here.
     */
    protected void onServiceDisconnectedExtended(ComponentName className) {
    }

    /**
     * Must be implemented.
     *
     * PluginService has checked if plugin has been enabled/disabled.
     *
     * The shared preferences has been changed. Take actions needed.
     */
    protected void onSharedPreferenceChangedExtended(SharedPreferences pref, String key) {
        if (key.equals(UPDATE_INTERVAL)) {
            long value = Long.parseLong(pref.getString("updateInterval", "60"));
            mUpdateInterval = value * 1000;

            Log.d(PluginConstants.LOG_TAG, "Preferences changed - update interval: " + mUpdateInterval);
        }
    }

    protected void startPlugin() {
        Log.d(PluginConstants.LOG_TAG, "startPlugin");
        startWork();
    }

    protected void stopPlugin() {
        Log.d(PluginConstants.LOG_TAG, "stopPlugin");
        stopWork();
    }

    protected void button(String buttonType, boolean doublepress, boolean longpress) {
        Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType + ", doublepress " + doublepress + ", longpress " + longpress);
    }

    protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
        Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx + ", height " + displayHeigthPx);
    }

    protected void onUnregistered() {
        Log.d(PluginConstants.LOG_TAG, "onUnregistered");
        stopWork();
    }

    protected void openInPhone(String openInPhoneAction) {
        Log.d(PluginConstants.LOG_TAG, "openInPhone: " + openInPhoneAction);

        try {
            Object sbservice = getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showsb;
            if (Build.VERSION.SDK_INT >= 17) {
                showsb = statusbarManager.getMethod("expandNotificationsPanel");
            } else {
                showsb = statusbarManager.getMethod("expand");
            }
            showsb.invoke(sbservice);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Open in browser.
//        final Uri uri = Uri.parse(openInPhoneAction);
//        final Intent browserIntent = new Intent();
//        browserIntent.setData(uri);
//        browserIntent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
//        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(browserIntent);
    }

    protected void screenMode(int mode) {
        Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now " + ((mode == 0) ? "OFF" : "ON"));
    }

    public void sendAnnounce(String header, String body) {
        try {
            if (mWorkerRunning && (mLiveViewAdapter != null) && mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
                mLiveViewAdapter.sendAnnounce(mPluginId, mMenuIcon, header, body, System.currentTimeMillis(), "expand_statusbar");
                Log.d(PluginConstants.LOG_TAG, "Announce sent to LiveView");
            } else {
                Log.d(PluginConstants.LOG_TAG, "LiveView not reachable");
            }
        } catch (Exception e) {
            Log.e(PluginConstants.LOG_TAG, "Failed to send announce", e);
        }
    }

    /**
     * Schedules a timer.
     */
    private void scheduleTimer() {
        if (mWorkerRunning) {
            mHandler.postDelayed(mAnnouncer, mUpdateInterval);
        }
    }
    /**
     * The runnable used for posting to handler
     */
    private Runnable mAnnouncer = new Runnable() {
        @Override
        public void run() {
            try {
                //sendAnnounce("Hello", "Hello world number " + mCounter++);
            } catch (Exception re) {
                Log.e(PluginConstants.LOG_TAG, "Failed to send image to LiveView.", re);
            }

            //scheduleTimer();
        }
    };
}