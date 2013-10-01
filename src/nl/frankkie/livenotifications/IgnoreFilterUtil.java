package nl.frankkie.livenotifications;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import nl.frankkie.livenotifications.databaserows.IgnoreItem;
import nl.wotuu.database.DatabaseOpenHelper;

/**
 *
 * @author FrankkieNL
 */
public class IgnoreFilterUtil {
    
    public static ArrayList<IgnoreItem> ignoreItems = null;
    public static boolean ignoreItemsInvalidated = false;
    
    public static boolean allowNotification(Context context, AccessibilityEvent event) {
        if (event == null) {
            return false;
        }
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return false;
        }
        //remove Toast-Messages, those don't count !!
        if (event.getPackageName().equals("android")) {
            return false;
        }
        if (event.getClassName().toString().equals("android.widget.Toast")) {
            return false;
        }

        //Database
        if (ignoreItems == null || ignoreItemsInvalidated) {
            refreshIgnoreItemsFromDatabase(context);
        }
        
        PackageManager packageManager = context.getPackageManager();
        String applicationName = "android";
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(event.getPackageName().toString(), 0);
            applicationName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
            for (IgnoreItem item : ignoreItems) {
                if (item.appName.equalsIgnoreCase(applicationName)
                        || item.packageName.equalsIgnoreCase(event.getPackageName().toString())) {
                    return false;
                }
            }
        } catch (PackageManager.NameNotFoundException exception) {
            return false;
        }
        
        return true;
    }
    
    public static void refreshIgnoreItemsFromDatabase(Context context) {
        ignoreItems = new ArrayList<IgnoreItem>();
        DatabaseOpenHelper databaseOpenHelper = DatabaseOpenHelper.createInstance(context);
        Cursor cursor = databaseOpenHelper.readableDatabase.rawQuery("SELECT id FROM ignoreitem", null);
        while (cursor.moveToNext()) {
            IgnoreItem item = new IgnoreItem(cursor.getInt(0));
            item.onLoad();
            ignoreItems.add(item);
        }
        cursor.close();
        if (ignoreItems.size() == 0){
            databaseOpenHelper.makeDefaultItems();
            //retry
            //I hope this won't StackOverflow :P
            refreshIgnoreItemsFromDatabase(context);
        }
        ignoreItemsInvalidated = false;
    }
}
