package is.hello.sense.ui.widget;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import is.hello.sense.util.Logger;

/**
 * Custom webview that takes an initial and on complete url.
 * Will make listener callbacks in respect to url loaded.
 */
public class CustomWebViewClient extends WebViewClient{

    private String completionUrl;
    private String initialUrl;
    private Listener listener;

    public CustomWebViewClient(@NonNull final String initialUrl,
                               @NonNull final String completionUrl){
        this.completionUrl = completionUrl;
        this.initialUrl = initialUrl;
    }

    public void setListener(@Nullable final Listener listener){
        this.listener = listener;
    }

    @Override
    public void onReceivedError(final WebView view, final WebResourceRequest request, final WebResourceError error) {
        super.onReceivedError(view, request, error);
        Logger.debug(CustomWebViewClient.class.getName()+"error", error.toString());
    }

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Logger.debug(CustomWebViewClient.class.getName() + " page started", url);
        if(listener != null && url.contains(completionUrl)){
            listener.onCompletionUrlStarted();
        }
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        Logger.debug(CustomWebViewClient.class.getName() + " page finished", url);
        super.onPageFinished(view, url);
        if(listener != null){
            if (url.contains(initialUrl)) {
                listener.onInitialUrlLoaded();
            } else if (url.contains(completionUrl)) {
                listener.onCompletionUrlFinished();
            } else {
                listener.onOtherUrlLoaded();
            }
        }
    }

    public void setInitialUrl(@NonNull final String initialUrl) {
        this.initialUrl = initialUrl;
    }

    public String getInitialUrl() {
        return initialUrl;
    }

    public void setCompletionUrl(@NonNull final String completionUrl) {
        this.completionUrl = completionUrl;
    }

    public interface Listener {
        void onInitialUrlLoaded();
        void onCompletionUrlStarted();
        void onCompletionUrlFinished();
        void onOtherUrlLoaded();
        void onResourceError();
    }
}
