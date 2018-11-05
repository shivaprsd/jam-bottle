package org.shiva.jambottle;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MOCKYEAR = "org.shiva.jambottle.MOCK_YEAR";
    public static final String EXTRA_RESOURCEDIR = "org.shiva.jambottle.RESOURCE_DIR";
    public static final String APP_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() + "/JAM Bottle/";
    public static final String WEBSITE_URL = "https://shivaprsdv.github.io/jambottle-resources/index.html";
    public static final String STORAGE_ACCESS_ERROR = "Cannot access storage: have you given necessary permissions?";
    public static final String NETWORK_ERROR = "No Internet connection!";
    private static final String[] EASTER_EGG = { "Today", "Ithu nammal polikkum" };
    private static final int JAM_DAY_OF_YEAR = 42;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setLayout(this.getResources().getConfiguration());
        setDaysToGo();

        SharedPreferences pref = this.getPreferences(Context.MODE_PRIVATE);
        boolean first_time = pref.getBoolean(getString(R.string.app_name), true);

        if (first_time && firstTimeSetup()) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(getString(R.string.app_name), false);
            editor.apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.main_about)
                        .setCancelable(true)
                        .create();
                alertDialog.show();
                return true;
            case R.id.website:
                if (checkNetwork()) {
                    Intent open_website = new Intent(Intent.ACTION_VIEW);
                    open_website.setData(Uri.parse(WEBSITE_URL));
                    startActivity(open_website);
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        setLayout(config);
    }

    public void openCalc(View view) {
        Intent open_calc = new Intent(this, CalcActivity.class);
        startActivity(open_calc);
    }

    public void openMock(View view) {
        final CharSequence years[] = new CharSequence[] { "2018", "2017", "2016" };
        final CharSequence year_codes[] = new CharSequence[] { "31", "23", "16" };
        if (!checkNetwork()) return;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Choose year").setItems(years, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int choice) {
                Intent open_mock = new Intent(MainActivity.this, MockActivity.class);
                open_mock.putExtra(MainActivity.EXTRA_MOCKYEAR, year_codes[choice]);
                startActivity(open_mock);
            }
        }).create();
        dialog.show();
    }

    public void openResource(View view) {
        Intent open_res = new Intent(this, FileActivity.class);
        switch (view.getId()) {
            case R.id.qp_button:
                open_res.putExtra(EXTRA_RESOURCEDIR, getString(R.string.qp_btn_label) + "/");
                break;
            case R.id.key_button:
                open_res.putExtra(EXTRA_RESOURCEDIR, getString(R.string.key_btn_label) + "/");
                break;
        }
        startActivity(open_res);
    }

    private void setDaysToGo() {
        int num_of_days = JAM_DAY_OF_YEAR - Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        TextView days_view = findViewById(R.id.days);
        TextView days_to_go_view = findViewById(R.id.days_to_go);

        if (num_of_days == 0) {
            days_view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 80);
            days_view.setText(EASTER_EGG[0]);
            days_to_go_view.setText(EASTER_EGG[1]);
        } else {
            if (num_of_days == 1) days_to_go_view.setText(R.string.day_to_go_label);
            if (num_of_days < 0) num_of_days = 0;
            days_view.setText(String.valueOf(num_of_days));
        }
    }

    private void setLayout(Configuration config) {
        LinearLayout layout = findViewById(R.id.grid);

        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            layout.setOrientation(LinearLayout.VERTICAL);
        } else if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layout.setOrientation(LinearLayout.HORIZONTAL);
        }
    }

    private boolean copyFileFromAssets(String in_filename, File out_file) {
        AssetManager assetManager = getApplicationContext().getAssets();
        FileChannel in_chan = null, out_chan = null;
        try {
            AssetFileDescriptor in_afd = assetManager.openFd(in_filename);
            FileInputStream in_stream = in_afd.createInputStream();
            in_chan = in_stream.getChannel();
            FileOutputStream out_stream = new FileOutputStream(out_file);
            out_chan = out_stream.getChannel();
            in_chan.transferTo(in_afd.getStartOffset(), in_afd.getLength(), out_chan);
            return true;
        } catch (IOException e) {
            Toast.makeText(this, STORAGE_ACCESS_ERROR, Toast.LENGTH_SHORT).show();
            return false;
        } finally {
            try {
                if (in_chan != null) {
                    in_chan.close();
                }
                if (out_chan != null) {
                    out_chan.close();
                }
            } catch (IOException e) {}
        }
    }

    private boolean copyAsserDir(String directory) {
        final String asset_dir = directory.replace(" ", "").replace("/", "");
        try {
            String[] asset_files = getApplicationContext().getAssets().list(asset_dir);
            /*Toast.makeText(this, "Copying resources...", Toast.LENGTH_SHORT).show();*/
            for (String asset : asset_files) {
                File target = new File(APP_DIRECTORY + directory + asset);

                if (!copyFileFromAssets(asset_dir + "/" + asset, target))
                    return false;
            }
            /*Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();*/
            return true;
        } catch (IOException e) {
            Toast.makeText(this, STORAGE_ACCESS_ERROR, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean makeDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdir()) {
            Toast.makeText(this, STORAGE_ACCESS_ERROR, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean firstTimeSetup() {
        final String qp_dir = getString(R.string.qp_btn_label) + "/";
        final String key_dir = getString(R.string.key_btn_label) + "/";

        return makeDirectory(APP_DIRECTORY) && makeDirectory(APP_DIRECTORY + qp_dir)
                && makeDirectory(APP_DIRECTORY + key_dir)
                && copyAsserDir(qp_dir) && copyAsserDir(key_dir);
    }

    private boolean checkNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null || cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isConnected()) {
            Toast.makeText(this, NETWORK_ERROR, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}