package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import org.markdownj.MarkdownProcessor;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

@Singleton public final class Markdown {
    private final MarkdownProcessor processor = new MarkdownProcessor();

    @Inject public Markdown() {

    }

    public @NonNull String toHtml(@Nullable String markdown) {
        if (TextUtils.isEmpty(markdown))
            return "";

        return processor.markdown(markdown);
    }

    public @NonNull CharSequence toSpanned(@Nullable String markdown) {
        String html = toHtml(markdown);
        if (TextUtils.isEmpty(html))
            return "";

        Spanned renderedHtml = Html.fromHtml(html);
        return renderedHtml.subSequence(0, TextUtils.getTrimmedLength(renderedHtml));
    }

    public @NonNull Observable<CharSequence> render(@Nullable String markdown) {
        if (TextUtils.isEmpty(markdown))
            return Observable.just("");

        return Observable.create((Observable.OnSubscribe<CharSequence>) s -> {
            try {
                CharSequence rendered = toSpanned(markdown);
                s.onNext(rendered);
                s.onCompleted();
            } catch (Exception e) {
                s.onError(e);
            }
        });
    }
}
