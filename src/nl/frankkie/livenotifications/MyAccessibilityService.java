package nl.frankkie.livenotifications;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import com.sonyericsson.extras.liveview.plugins.livenotifications.LiveNotificationsService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Gebruiker
 */
public class MyAccessibilityService extends AccessibilityService {

    String[] ignoreList = new String[]{"Systeem-UI", "Google Zoeken", "Google Search", "System-UI"};

    @Override
    protected void onServiceConnected() {
        //API16+ !!!!
        //getServiceInfo().eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent arg0) {
        if (arg0 == null) {
            return;
        }
        if (arg0.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return;
        }
        //remove Toast-Messages, those don't count !!
        if (arg0.getPackageName().equals("android")) {
            return;
        }
        if (arg0.getClassName().toString().equals("android.widget.Toast")) {
            return;
        }

        Log.e("LiveNotifications_Accessibilty", arg0.toString());
        //Toast.makeText(this, "Notification from: " + arg0.getPackageName(), Toast.LENGTH_SHORT).show();

        //show on LiveView
        LiveNotificationsService liveViewService = LiveNotificationsService.getInstance();
        if (liveViewService != null) {
            String applicationName = "Unknown Application";
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(arg0.getPackageName().toString(), 0);
                applicationName = getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString();
                for (String ignore : ignoreList) {
                    if (applicationName.equalsIgnoreCase(ignore)) {
                        return;
                    }
                }
            } catch (PackageManager.NameNotFoundException ex) {
                Logger.getLogger(MyAccessibilityService.class.getName()).log(Level.SEVERE, null, ex);
            }
            liveViewService.sendAnnounce(getString(R.string.notification), applicationName + " " + getString(R.string.send_a_notification));
        }
    }

    @Override
    public void onInterrupt() {
        //do nothin'
    }
}
