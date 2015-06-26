package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.util.markup.MarkupProcessor;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Legacy markup rendering class. Prefer MarkupString inline for all new code.
 */
@Singleton public final class Markdown {
    private final MarkupProcessor processor;

    @Inject public Markdown(@NonNull MarkupProcessor processor) {
        this.processor = processor;
    }

    public @NonNull CharSequence toSpanned(@Nullable String markup) {
        if (TextUtils.isEmpty(markup)) {
            return "";
        } else {
            return processor.render(markup);
        }
    }

    public @NonNull Observable<CharSequence> render(@Nullable String markdown) {
        if (TextUtils.isEmpty(markdown)) {
            return Observable.just((CharSequence) "")
                             .observeOn(AndroidSchedulers.mainThread());
        }

        Observable<CharSequence> renderTask = Observable.create(s -> {
            try {
                CharSequence rendered = toSpanned(markdown);
                s.onNext(rendered);
                s.onCompleted();
            } catch (Exception e) {
                s.onError(e);
            }
        });
        return renderTask.subscribeOn(Schedulers.computation())
                         .observeOn(AndroidSchedulers.mainThread());
    }

    public void renderInto(@NonNull TextView textView, @Nullable String markdown) {
        render(markdown)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(textView::setText, e -> {
                    Logger.error(getClass().getSimpleName(), "Could not render markdown", e);
                    textView.setText(markdown);
                });
    }
}
