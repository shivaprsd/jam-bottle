package org.shiva.jambottle;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.PopupMenu;

public class CalcActivity extends AppCompatActivity {
    public static final Integer FULLSCREEN = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private boolean topbar_visible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        WebView calc_view = findViewById(R.id.calcview);
        calc_view.getSettings().setJavaScriptEnabled(true);
        calc_view.setWebChromeClient(new WebChromeClient(){
            public void onCloseWindow(WebView view){
                super.onCloseWindow(view);
                CalcActivity.this.finish();
            }
        });
        calc_view.loadUrl("file:///android_asset/ScientificCalculator/Calculator.html");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WebView calc_view = findViewById(R.id.calcview);
            calc_view.setSystemUiVisibility(FULLSCREEN);
        }
    }

    public void showPopup(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.calc_menu);

        MenuItem toggle_top_bar = popup.getMenu().findItem(R.id.toggle_top_bar);
        if(topbar_visible) {
            toggle_top_bar.setTitle(R.string.calc_hide_topbar);
        } else {
            toggle_top_bar.setTitle(R.string.calc_show_topbar);
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                WebView calc_view = findViewById(R.id.calcview);

                switch (item.getItemId()) {
                    case R.id.toggle_top_bar:
                        calc_view.evaluateJavascript("window.toggleHelpBar();", null);
                        topbar_visible = !topbar_visible;
                        return true;
                    case R.id.instruct:
                        openInstructions();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    public void openInstructions() {
        Intent open_instruct = new Intent(this, InstructActivity.class);
        startActivity(open_instruct);
    }
}