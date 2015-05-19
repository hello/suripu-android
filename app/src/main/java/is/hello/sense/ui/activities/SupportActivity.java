package is.hello.sense.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.util.Share;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class SupportActivity extends SenseActivity {
    public static final String EXTRA_URL = SupportActivity.class.getName() + ".EXTRA_URL";

    private static final int MSG_SHOW_PROGRESS = 0;
    private static final int SHOW_PROGRESS_DELAY_MS = 2500;

    private WebView webView;
    private View progress;

    private final Handler progressHandler = new Handler(msg -> {
        if (msg.what == MSG_SHOW_PROGRESS) {
            animate(progress)
                    .setDuration(Animation.DURATION_VERY_FAST)
                    .fadeIn()
                    .start();
            progress.setVisibility(View.VISIBLE);
        }
        return true;
    });

    //region Creation

    public static Bundle getArguments(@NonNull Uri uri) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(EXTRA_URL, uri);
        return arguments;
    }

    public static Intent getIntent(@NonNull Context from, @NonNull Uri uri) {
        Intent intent = new Intent(from, SupportActivity.class);
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
        this.progress = findViewById(R.id.activity_help_progress);

        webView.setWebViewClient(new Client());

        WebSettings settings = webView.getSettings();
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
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
        getMenuInflater().inflate(R.menu.support, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.support_share: {
                Share.text(webView.getUrl()).send(this);
                return true;
            }

            case R.id.support_refresh: {
                webView.reload();
                return true;
            }

            case R.id.support_contact: {
                UserSupport.showSupport(this);
                return true;
            }

            case R.id.support_feedback: {
                UserSupport.showEmailFeedback(this);
                return true;
            }

            case R.id.support_acknowledgements: {
                showAcknowledgements();
                return true;
            }

            case android.R.id.home: {
                super.onBackPressed();
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //endregion


    private void showAcknowledgements() {
        webView.loadUrl("file:///android_asset/ACKNOWLEDGEMENTS.txt");
    }

    private void setPageTitle(@Nullable CharSequence title) {
        //noinspection ConstantConditions
        getActionBar().setSubtitle(title);
    }

    private void scheduleShowProgress() {
        progressHandler.removeMessages(MSG_SHOW_PROGRESS);
        progressHandler.sendEmptyMessageDelayed(MSG_SHOW_PROGRESS, SHOW_PROGRESS_DELAY_MS);
    }

    private void hideProgress() {
        progressHandler.removeMessages(MSG_SHOW_PROGRESS);
        animate(progress)
                .setDuration(Animation.DURATION_VERY_FAST)
                .fadeOut(View.GONE)
                .start();
    }

    private class Client extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            scheduleShowProgress();
            setPageTitle(getString(R.string.dialog_loading_message));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            hideProgress();
            setPageTitle(view.getTitle());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            hideProgress();
            setPageTitle(getString(R.string.dialog_error_title));
        }
    }
}
