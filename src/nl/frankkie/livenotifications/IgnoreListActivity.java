package nl.frankkie.livenotifications;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author FrankkieNL
 */
public class IgnoreListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //To change body of generated methods, choose Tools | Templates.
        initUI();
    }
    
    public void initUI(){
        setContentView(R.layout.main);
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
    }
    
    public void addApp(){
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
        for (ResolveInfo info : apps){
            String appName = info.loadLabel(manager).toString();
            items.add(appName);
        }
        //
        b.setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
                String packageName = apps.get(arg1).resolvePackageName;
                addPackagenameToList(packageName);
                Toast.makeText(IgnoreListActivity.this, packageName, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public void addPackagename(){
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.ignore_add_packagename));
        final EditText ed = new EditText(this);
        b.setView(ed);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
                String packageName = ed.getText().toString();
                addPackagenameToList(packageName);
                Toast.makeText(IgnoreListActivity.this, packageName, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public void addPackagenameToList(String packageName){
        //TODO
    }
}
