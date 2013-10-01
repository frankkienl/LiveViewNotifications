package nl.frankkie.livenotifications;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static nl.frankkie.livenotifications.IgnoreFilterUtil.ignoreItems;
import nl.frankkie.livenotifications.databaserows.IgnoreItem;
import nl.wotuu.database.DatabaseOpenHelper;

/**
 *
 * @author FrankkieNL
 */
public class IgnoreListActivity extends Activity {

    LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //To change body of generated methods, choose Tools | Templates.
        DatabaseOpenHelper.createInstance(this);
        initUI();
    }

    public void initUI() {
        setContentView(R.layout.ignore_list);
        findViewById(R.id.btn_ignore_add_app).setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                addApp();
            }
        });
        findViewById(R.id.btn_ignore_add_packagename).setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                addPackagename();
            }
        });
        loadIgnoreList();
    }

    public void loadIgnoreList() {
        listContainer = (LinearLayout) findViewById(R.id.ignore_list_container);
        refreshIgnoreList();
    }

    public void refreshIgnoreList() {
        IgnoreFilterUtil.refreshIgnoreItemsFromDatabase(this);
        listContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (final IgnoreItem item : ignoreItems) {
            View row = inflater.inflate(R.layout.ignore_list_row, listContainer, false);
            TextView tv1 = (TextView) row.findViewById(R.id.ignore_list_row_tv);
            tv1.setText(item.appName + " - " + item.packageName);
            TextView tv2 = (TextView) row.findViewById(R.id.ignore_list_row_tv2);
            tv2.setText(item.description);
            final TextView tvHidden = (TextView) row.findViewById(R.id.ignore_list_row_tv_hidden);
            tvHidden.setText("" + item.id); //yes, it has to be a string
            row.findViewById(R.id.ignore_list_row_delete).setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    try {
                        IgnoreItem item = new IgnoreItem(Integer.parseInt(tvHidden.getText().toString()));
                        item.onDelete();
                        refreshIgnoreList();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            listContainer.addView(row);
        }
    }

    public void addApp() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.ignore_add_app));
        //Get list of apps
        //https://github.com/frankkienl/OuyaLauncherFrankkieNL/blob/master/FrankkieOuyaLauncher/src/main/java/nl/frankkie/ouyalauncher/MainActivity.java
        PackageManager manager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        ArrayList<String> items = new ArrayList<String>();
        for (ResolveInfo info : apps) {
            String appName = info.loadLabel(manager).toString();
            items.add(appName);
        }
        //
        b.setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                String packageName = apps.get(arg1).activityInfo.packageName;
                addToList(packageName, apps.get(arg1).loadLabel(getPackageManager()).toString(), "");
                Toast.makeText(IgnoreListActivity.this, packageName, Toast.LENGTH_SHORT).show();
            }
        });
        b.create().show();
    }

    public void addPackagename() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.ignore_add_packagename));
        final EditText ed = new EditText(this);
        b.setView(ed);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                String packageName = ed.getText().toString();
                addToList(packageName);
                Toast.makeText(IgnoreListActivity.this, packageName, Toast.LENGTH_SHORT).show();
            }
        });
        b.create().show();        
    }

    public void addToList(String packageName){
         PackageManager packageManager = getPackageManager();
        String applicationName = "android";
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            applicationName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
        } catch (Exception e){
            //ignore use default
        }
        addToList(packageName, applicationName, "");
    }
    
    public void addToList(String packageName, String appName, String description) {
        IgnoreItem ignoreItem = new IgnoreItem();
        ignoreItem.appName = appName;
        ignoreItem.description = description;
        ignoreItem.packageName = packageName;
        ignoreItem.onInsert();
        refreshIgnoreList();
    }
}
