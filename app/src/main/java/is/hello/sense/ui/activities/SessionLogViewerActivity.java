package is.hello.sense.ui.activities;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.SessionLogger;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class SessionLogViewerActivity extends SenseActivity {
    private WebView webView;
    private ProgressBar activityIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_log_viewer);

        this.webView = (WebView) findViewById(R.id.activity_session_log_viewer_web_view);
        webView.setWebViewClient(new Client());

        this.activityIndicator = (ProgressBar) findViewById(R.id.activity_session_log_viewer_activity);

        if (savedInstanceState == null) {
            reload();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        webView.destroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        webView.saveState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session_log_viewer, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_reload) {
            reload();
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }


    public void reload() {
        String logFilePath = SessionLogger.getLogFilePath(this);
        String url = "file://" + Uri.encode(logFilePath, "/.");
        webView.loadUrl(url);
    }


    private class Client extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            animate(activityIndicator)
                    .fadeIn()
                    .start();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            animate(activityIndicator)
                    .fadeOut(View.GONE)
                    .start();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            animate(activityIndicator)
                    .fadeOut(View.GONE)
                    .start();

            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .setMessage(StringRef.from(description))
                    .setContextInfo(failingUrl)
                    .create();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        }
    }
}
