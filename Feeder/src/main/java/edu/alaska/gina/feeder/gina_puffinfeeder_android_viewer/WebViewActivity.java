package edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class WebViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle url = getIntent().getExtras();
        String pageUrl = url.getString("url", "https://github.com/404");
        if (!pageUrl.substring(0, 5).equals("file:")) {
            requestWindowFeature(Window.FEATURE_PROGRESS);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }

        setContentView(R.layout.activity_web_view);


        WebView screen = (WebView) findViewById(R.id.info_web_view);
        screen.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                try {
                    setProgress(newProgress * 100);
                    if (newProgress >= 0)
                        setProgressBarIndeterminate(false);
                    if (newProgress >= 100) {
                        setProgressBarVisibility(false);
                        setProgressBarIndeterminateVisibility(false);
                    }
                } catch (NullPointerException e) {
                    Log.d(getString(R.string.app_tag), "ProgressBar NullPointer!\n" + e.getStackTrace());
                }
            }
        });

        setProgressBarVisibility(true);
        setProgressBarIndeterminate(true);
        setProgressBarIndeterminateVisibility(true);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(url.getString("title", "Page not found - GitHub"));
        screen.loadUrl(pageUrl);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
