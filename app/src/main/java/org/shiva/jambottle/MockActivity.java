package org.shiva.jambottle;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MockActivity extends AppCompatActivity {
    public static final Integer FULLSCREEN = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    public static final String MOCK_LINK=
            "https://www.digialm.com//OnlineAssessment/index.html?1048@@M";
    public static final String ONLOAD_JS= "viewport = document.querySelector('meta[name=viewport]');" +
            "viewport.setAttribute('content','width=device-width,initial-scale=1.0,user-scalable=yes');";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock);

        WebView mock_view = findViewById(R.id.mockview);
        mock_view.getSettings().setJavaScriptEnabled(true);
        mock_view.getSettings().setBuiltInZoomControls(true);
        mock_view.getSettings().setDisplayZoomControls(false);
        mock_view.clearCache(true);

        mock_view.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(ONLOAD_JS, null);
            }
        });
        mock_view.loadUrl(MOCK_LINK + getIntent().getStringExtra(MainActivity.EXTRA_MOCKYEAR));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WebView mock_view = findViewById(R.id.mockview);
            mock_view.setSystemUiVisibility(FULLSCREEN);
        }
    }
}
