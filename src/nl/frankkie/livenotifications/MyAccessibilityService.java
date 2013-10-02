package nl.frankkie.livenotifications;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import com.sonyericsson.extras.liveview.plugins.livenotifications.LiveNotificationsService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.wotuu.database.DatabaseOpenHelper;

/**
 *
 * @author FrankkieNL
 */
public class MyAccessibilityService extends AccessibilityService {

    @Override
    protected void onServiceConnected() {
        //API16+ !!!!
        //getServiceInfo().eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent arg0) {
        DatabaseOpenHelper.createInstance(this);
        if (!IgnoreFilterUtil.allowNotification(this, arg0)) {
            return;
        }

        //try to get some text from the notification
        String notificationText = getNotificationText(arg0);

        Log.e("LiveNotifications_Accessibilty", arg0.toString());
        //Toast.makeText(this, "Notification from: " + arg0.getPackageName(), Toast.LENGTH_SHORT).show();

        //show on LiveView
        LiveNotificationsService liveViewService = LiveNotificationsService.getInstance();
        if (liveViewService != null) {
            String applicationName = "Unknown Application";
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(arg0.getPackageName().toString(), 0);
                applicationName = getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString();
            } catch (PackageManager.NameNotFoundException ex) {
                Logger.getLogger(MyAccessibilityService.class.getName()).log(Level.SEVERE, null, ex);
            }
            liveViewService.sendAnnounce(getString(R.string.notification), applicationName + " " + getString(R.string.send_a_notification) + "\n" + notificationText);
        }
    }

    @Override
    public void onInterrupt() {
        //do nothin'
    }

    public String getNotificationText(AccessibilityEvent event) {
        //http://stackoverflow.com/questions/9292032/extract-notification-text-from-parcelable-contentview-or-contentintent
        String answer = "";
        try {
            Notification notification = (Notification) event.getParcelableData();
            RemoteViews views = notification.contentView;
            Class secretClass = views.getClass();

            Field outerFields[] = secretClass.getDeclaredFields();
            for (int i = 0; i < outerFields.length; i++) {
                if (!outerFields[i].getName().equals("mActions")) {
                    continue;
                }

                outerFields[i].setAccessible(true);

                ArrayList<Object> actions =
                        (ArrayList<Object>) outerFields[i].get(views);
                for (Object action : actions) {
                    Field innerFields[] = action.getClass().getDeclaredFields();

                    Object value = null;
                    String methodName = null;
                    for (Field field : innerFields) {
                        field.setAccessible(true);
                        if (field.getName().equals("value")) {
                            value = field.get(action);
                        } else if (field.getName().equals("methodName")) {
                            methodName = field.get(action).toString();
                        }
                    }
                    if (methodName.equals("setText")) {
                        if (!value.toString().equals("")) {
                            answer += value.toString() + "\n";
                        }
                    }
                }
                return answer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
