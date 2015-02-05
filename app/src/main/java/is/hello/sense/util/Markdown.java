package is.hello.sense.util;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.widget.TextView;

import org.markdownj.MarkdownProcessor;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

    public @NonNull Observable<CharSequence> renderWithEmphasisColor(int color, @Nullable String markdown) {
        return render(markdown).map(maybeSpanned -> {
            if (maybeSpanned instanceof Spanned) {
                Spanned spanned = (Spanned) maybeSpanned;
                SpannableString result = new SpannableString(spanned.toString());

                MetricAffectingSpan[] spans = spanned.getSpans(0, spanned.length(), MetricAffectingSpan.class);
                ColorStateList colors = ColorStateList.valueOf(color);
                for (MetricAffectingSpan span : spans) {
                    int spanStart = spanned.getSpanStart(span);
                    int spanEnd = spanned.getSpanEnd(span);
                    int spanFlags = spanned.getSpanFlags(span);

                    if (span instanceof TextAppearanceSpan) {
                        TextAppearanceSpan appearanceSpan = (TextAppearanceSpan) span;
                        if (appearanceSpan.getTextStyle() != Typeface.NORMAL) {
                            TextAppearanceSpan updatedSpan = new TextAppearanceSpan(appearanceSpan.getFamily(), appearanceSpan.getTextStyle(), appearanceSpan.getTextSize(), colors, colors);
                            result.setSpan(updatedSpan, spanStart, spanEnd, spanFlags);

                            continue;
                        }
                    } else if (span instanceof StyleSpan) {
                        StyleSpan styleSpan = (StyleSpan) span;
                        result.setSpan(styleSpan, spanStart, spanEnd, spanFlags);
                        result.setSpan(new ForegroundColorSpan(color), spanStart, spanEnd, spanFlags);

                        continue;
                    }

                    result.setSpan(span, spanned.getSpanStart(span), spanned.getSpanEnd(span), spanned.getSpanFlags(span));
                }

                return result;
            } else {
                return maybeSpanned;
            }
        });
    }

    public void renderEmphasisInto(@NonNull TextView textView, int color, @Nullable String markdown) {
        renderWithEmphasisColor(color, markdown)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(textView::setText, e -> {
                    Logger.error(getClass().getSimpleName(), "Could not render markdown", e);
                    textView.setText(markdown);
                });
    }
}
