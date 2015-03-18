package is.hello.sense.ui.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.util.Share;

public class HelpActivity extends SenseActivity {
    public static final String EXTRA_URL = HelpActivity.class.getName() + ".EXTRA_URL";

    private WebView webView;
    private ProgressBar progress;

    //region Creation

    public static Bundle getArguments(@NonNull Uri uri) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(EXTRA_URL, uri);
        return arguments;
    }

    public static Intent getIntent(@NonNull Activity from, @NonNull Uri uri) {
        Intent intent = new Intent(from, HelpActivity.class);
        intent.putExtras(getArguments(uri));
        return intent;
    }

    //endregion


    //region Lifecycle

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        this.webView = (WebView) findViewById(R.id.activity_help_web_view);
        this.progress = (ProgressBar) findViewById(R.id.activity_help_progress);

        webView.setWebViewClient(new Client());

        WebSettings settings = webView.getSettings();
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccess(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            Uri uri = getIntent().getParcelableExtra(EXTRA_URL);
            webView.loadUrl(uri.toString());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        webView.destroy();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    //endregion


    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help_share: {
                Share.text(webView.getUrl()).send(this);
                return true;
            }

            case R.id.help_contact: {
                UserSupport.showSupport(this);
                return true;
            }

            case R.id.help_feedback: {
                UserSupport.showEmailFeedback(this);
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //endregion


    private void setPageTitle(@Nullable CharSequence title) {
        //noinspection ConstantConditions
        getActionBar().setSubtitle(title);
    }

    private class Client extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progress.setVisibility(View.VISIBLE);
            setPageTitle(null);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progress.setVisibility(View.GONE);
            setPageTitle(view.getTitle());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            setPageTitle(getString(R.string.dialog_error_title));
            progress.setVisibility(View.GONE);
        }
    }
}
