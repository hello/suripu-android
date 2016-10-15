package is.hello.sense.mvp.view.expansions;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.util.Map;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.CustomWebViewClient;

public class ExpansionsAuthView extends PresenterView {
    private final WebView webView;
    private final CustomWebViewClient customWebViewClient;
    private final ProgressBar progressBar;

    public ExpansionsAuthView(@NonNull final Activity activity,
                              @NonNull final CustomWebViewClient customWebViewClient) {
        super(activity);
        this.progressBar = (ProgressBar) findViewById(R.id.expansions_auth_layout_progress);
        this.webView = (WebView) findViewById(R.id.expansions_auth_layout_web_view);
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        this.webView.getSettings().setLoadWithOverviewMode(true);
        this.customWebViewClient = customWebViewClient;
        this.webView.setWebViewClient(customWebViewClient);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.expansions_auth_layout;
    }

    @Override
    public void releaseViews() {
        customWebViewClient.setListener(null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        webView.destroy();
    }

    public void loadlInitialUrl(@NonNull final Map<String, String> headers){
        webView.loadUrl(customWebViewClient.getInitialUrl(), headers);
    }

    public void reloadCurrentUrl() {
        webView.reload();
    }

    public void showProgress(final boolean show) {
        progressBar.setVisibility(show ? VISIBLE : INVISIBLE);
    }

    public boolean loadPreviousUrl() {
        if(webView.canGoBack()){
            webView.goBack();
            return true;
        } else {
            return false;
        }
    }
}
