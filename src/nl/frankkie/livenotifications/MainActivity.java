package nl.frankkie.livenotifications;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import nl.wotuu.database.DatabaseOpenHelper;

public class MainActivity extends Activity {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatabaseOpenHelper.createInstance(this);
        setContentView(R.layout.main);

        Button btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                try {
                    startActivity(intent);
                    Toast.makeText(MainActivity.this,getString(R.string.hint_message),Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    failed();
                    e.printStackTrace();
                }
            }
        });
        
        findViewById(R.id.btn_ignore_list).setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                Intent i = new Intent();
                i.setClass(MainActivity.this, IgnoreListActivity.class);
                startActivity(i);
            }
        });
    }

    public void failed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.failed_message));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
                //remove dialog
            }
        });
        builder.create().show();
    }
}
