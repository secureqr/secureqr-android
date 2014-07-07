package at.rieder.secureqr.app.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import at.rieder.secureqr.app.R;

public final class HelpActivity extends Activity {

    private static final String BASE_URL = "file:///android_asset/help/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        WebView webView = (WebView) findViewById(R.id.help_contents);

        if (savedInstanceState == null) {
            webView.loadUrl(BASE_URL + "index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }
}
