package org.shiva.jambottle;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class FileActivity extends AppCompatActivity {
    private static final String PDF_OPEN_ERROR = "No PDF Reader installed!";
    private static final String ONLINE_KEYS_URL =
            "https://raw.githubusercontent.com/shivaprsdv/jambottle-resources/master/AnswerKeys/";
    private ArrayList<String> file_list;
    private ArrayAdapter<String> list_adapter;
    private boolean is_refreshing = false;
    private boolean is_in_foreground;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        final String resource_dir = getIntent().getStringExtra(MainActivity.EXTRA_RESOURCEDIR);
        this.setTitle(resource_dir.replace("/", ""));
        populateListView(resource_dir);
    }

    @Override
    protected void onResume() {
        super.onResume();
        is_in_foreground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        is_in_foreground = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.getTitle().toString().equals(getString(R.string.key_btn_label))) {
            getMenuInflater().inflate(R.menu.file_menu, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.refresh || is_refreshing)
            return false;
        is_refreshing = true;

        if (downloadFile(ONLINE_KEYS_URL, "answer_keys.txt", "")) {
            showToast("Checking for new solutions online..");

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    File private_dir = getExternalFilesDir(null);
                    if (private_dir != null) {
                        File new_list_file = new File(private_dir.getAbsolutePath(), "answer_keys.txt");
                        updateListView(new_list_file);
                        new_list_file.delete();
                        is_refreshing = false;
                    }
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } else {
            is_refreshing = false;
        }
        return true;
    }

    private void populateListView(final String resource_dir) {
        file_list = getFiles(MainActivity.APP_DIRECTORY + resource_dir);
        if (file_list == null)
            return;
        Collections.sort(file_list, Collections.<String>reverseOrder());

        list_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                                                new ArrayList<>(file_list)) {
            @Override @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView item_view = (TextView) view;
                final int download_icon = (file_list.contains(item_view.getText().toString())) ? 0
                                            : R.drawable.ic_file_download_black_24sp;

                item_view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pdf_icon_red_24sp, 0, download_icon, 0);
                item_view.setCompoundDrawablePadding(28);
                item_view.setPadding(32, 32, 32, 32);
                return view;
            }
        };
        ListView view = findViewById(R.id.file_view);
        view.setAdapter(list_adapter);

        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String item = parent.getItemAtPosition(position).toString();

                if (!file_list.contains(item)) {
                    if (downloadFile(ONLINE_KEYS_URL, item,
                        MainActivity.APP_DIRECTORY + resource_dir)) {

                        registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                file_list.add(item);
                                list_adapter.notifyDataSetChanged();
                            }
                        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                    }
                }
                else
                    openPDf(MainActivity.APP_DIRECTORY + resource_dir + item);
            }
        });
    }

    private void updateListView(File new_list_file) {
        ArrayList<String> new_list = readFileByLines(new_list_file);
        if (new_list == null)
            return;

        ArrayList<String> old_list = new ArrayList<>();
        for (int i = 0; i < list_adapter.getCount(); ++i)
            old_list.add(list_adapter.getItem(i));
        new_list.removeAll(old_list);

        if (!new_list.isEmpty()) {
            list_adapter.addAll(new_list);
            showToast("New files available, tap to download.");
        } else {
            showToast("No new solutions available.");
        }
    }

    private ArrayList<String> getFiles(String path) {
        ArrayList<String> file_list = new ArrayList<>();
        File[] files = new File(path).listFiles();

        if (files != null && files.length != 0) {
            for (File f: files)
                file_list.add(f.getName());
            return file_list;
        }
        return null;
    }

    private void openPDf(String file_path) {
        File file = new File(file_path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/pdf");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, "Open PDF"));
        } else {
            showToast(PDF_OPEN_ERROR);
        }
    }

    private boolean downloadFile(String url, String file_name, String path) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null || cm.getActiveNetworkInfo() == null
                || !cm.getActiveNetworkInfo().isConnected() || downloadManager == null) {
            showToast(MainActivity.NETWORK_ERROR);
            return false;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url + file_name));
        request.setVisibleInDownloadsUi(false)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                                       | DownloadManager.Request.NETWORK_MOBILE).setAllowedOverRoaming(false);
        if (path.isEmpty()) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                    .setDestinationInExternalFilesDir(this, null, file_name);
        } else {
            File destination = new File(path, file_name);
            if (destination.exists())
                return true;
            showToast("Downloading " + file_name);
            request.setTitle("Downloading " + file_name).setDestinationUri(Uri.fromFile(destination));
        }
        downloadManager.enqueue(request);
        return true;
    }

    private ArrayList<String> readFileByLines(File file) {
        try {
            Scanner s = new Scanner(file);
            ArrayList<String> list = new ArrayList<>();

            while (s.hasNextLine()) {
                list.add(s.nextLine());
            }
            s.close();
            return list;
        } catch (Exception e) {}
        return null;
    }

    public void showToast(String msg) {
        if (is_in_foreground)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}