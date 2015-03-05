package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.util.Styles;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public final class ShareImageGenerator implements Observable.OnSubscribe<ShareImageGenerator.Result> {
    private static final int SEGMENT_MIN_DURATION = 120;

    private final Context context;
    private final Timeline timeline;
    private final Resources resources;

    private final int width;
    private final int height;
    private final Rect bounds;

    private final int scoreWidth;
    private final int scoreHeight;
    private final int scoreTextHeight;
    private final int scoreLabelHeight;

    private final int baseSegmentHeight;

    private final Rect footerBounds;
    private final int footerTextHeight;
    private final int footerInset;

    private final String scoreLabel;

    private final String leftFooterText;
    private final String rightFooterText;
    private final Drawable footerLogo;

    private final Paint fillPaint = new Paint();
    private final Paint shadowedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scoreLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint scoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint footerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF ovalRect = new RectF();

    //region Creation

    public static Observable<Result> forTimeline(@NonNull Context context, @NonNull Timeline timeline) {
        return Observable.create(new ShareImageGenerator(context, timeline))
                         .subscribeOn(Schedulers.computation())
                         .observeOn(AndroidSchedulers.mainThread());
    }

    private ShareImageGenerator(@NonNull Context context,
                                @NonNull Timeline timeline) {
        this.context = context;
        this.timeline = timeline;
        this.resources = context.getResources();

        Paint.FontMetricsInt fontMetrics = new Paint.FontMetricsInt();


        // Score

        this.scoreWidth = resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_width);
        this.scoreHeight = resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_height);

        this.scoreLabel = resources.getString(R.string.sleep_score).toUpperCase();

        scoreLabelPaint.setTextAlign(Paint.Align.CENTER);
        scoreLabelPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        scoreLabelPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_section_heading));
        scoreLabelPaint.setColor(resources.getColor(R.color.text_section_header));
        scoreLabelPaint.getFontMetricsInt(fontMetrics);
        this.scoreLabelHeight = fontMetrics.top + fontMetrics.descent;

        scoreTextPaint.setTextAlign(Paint.Align.CENTER);
        scoreTextPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_big_score));
        scoreTextPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        scoreTextPaint.getFontMetricsInt(fontMetrics);
        this.scoreTextHeight = fontMetrics.top + fontMetrics.descent;


        // Footer

        int footerHeight = resources.getDimensionPixelSize(R.dimen.share_image_footer_height);
        this.footerInset = resources.getDimensionPixelSize(R.dimen.share_image_footer_inset);

        footerTextPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_body));
        footerTextPaint.setColor(resources.getColor(R.color.text_light));
        footerTextPaint.getFontMetricsInt(fontMetrics);
        this.footerTextHeight = fontMetrics.top + fontMetrics.descent;

        this.leftFooterText = DateFormat.getLongDateFormat(context)
                                        .format(timeline.getDate().getMillis());
        this.rightFooterText = UserSupport.COMPANY_URL;

        this.footerLogo = resources.getDrawable(R.drawable.share_company_logo);


        // Shared

        this.width = resources.getDimensionPixelSize(R.dimen.share_image_width);
        this.height = resources.getDimensionPixelSize(R.dimen.share_image_height);

        shadowedFillPaint.setColor(Color.WHITE);
        shadowedFillPaint.setShadowLayer(20, 0, 0, resources.getColor(R.color.share_image_shadow));

        this.bounds = new Rect(0, 0, width, height - footerHeight);
        this.footerBounds = new Rect(footerInset, height - footerHeight, width - footerInset, height);


        // Segments

        int minItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        this.baseSegmentHeight = Math.max(minItemHeight, bounds.height() / Styles.TIMELINE_HOURS_ON_SCREEN) * 4;
    }

    //endregion


    //region Drawing

    @Override
    public void call(Subscriber<? super Result> s) {
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        onDraw(canvas);

        s.onNext(new Result(timeline, image));
        s.onCompleted();
    }

    private int getSegmentHeight(@NonNull TimelineSegment segment) {
        return (int) ((segment.getDuration() / 3600f) * baseSegmentHeight);
    }


    private void drawSegments(@NonNull Canvas canvas) {
        if (!Lists.isEmpty(timeline.getSegments())) {
            int segmentMinY = bounds.top;
            for (TimelineSegment segment : timeline.getSegments()) {
                if (segment.getDuration() < SEGMENT_MIN_DURATION) {
                    continue;
                }

                int segmentMaxY = segmentMinY + getSegmentHeight(segment);

                int sleepDepth = segment.getSleepDepth();
                int colorRes = Styles.getSleepDepthColorRes(sleepDepth, false);
                fillPaint.setColor(resources.getColor(colorRes));

                float percentage = sleepDepth / 100f;
                float halfWidth = Math.round(bounds.right * percentage) / 2f;
                canvas.drawRect(bounds.centerX() - halfWidth, segmentMinY,
                                bounds.centerX() + halfWidth, segmentMaxY,
                                fillPaint);

                segmentMinY = segmentMaxY;

                if (segmentMaxY >= bounds.bottom) {
                    break;
                }
            }
        }

        fillPaint.setColor(resources.getColor(R.color.share_image_tint));
        canvas.drawRect(bounds, fillPaint);
    }

    private void drawScore(@NonNull Canvas canvas) {
        ovalRect.set(bounds.centerX() - scoreWidth / 2f, bounds.centerY() - scoreHeight / 2f,
                     bounds.centerX() + scoreWidth / 2f, bounds.centerY() + scoreHeight / 2f);
        canvas.drawOval(ovalRect, shadowedFillPaint);

        int score = timeline.getScore();
        String scoreText = Integer.toString(score);
        scoreTextPaint.setColor(Styles.getSleepScoreColor(context, score));
        float scoreTextY = bounds.centerY() - scoreTextHeight / 2f - scoreLabelHeight;
        canvas.drawText(scoreText, bounds.centerX(), scoreTextY, scoreTextPaint);

        canvas.drawText(scoreLabel, bounds.centerX(), (bounds.centerY() + scoreTextHeight / 2f) + scoreLabelHeight, scoreLabelPaint);
    }

    private void drawFooter(@NonNull Canvas canvas) {
        fillPaint.setColor(Color.WHITE);
        canvas.drawRect(0, footerBounds.top, width, footerBounds.bottom, fillPaint);

        float footerTextY = footerBounds.centerY() - (footerTextHeight / 2f);

        footerTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(leftFooterText, footerBounds.left, footerTextY, footerTextPaint);

        int logoWidth = footerLogo.getIntrinsicWidth();
        int logoHeight = footerLogo.getIntrinsicHeight();
        footerLogo.setBounds(
            footerBounds.right - logoWidth,
            footerBounds.centerY() - logoHeight / 2,
            footerBounds.right,
            footerBounds.centerY() + logoHeight / 2
        );
        footerLogo.draw(canvas);

        footerTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(rightFooterText, footerBounds.right - logoWidth - footerInset, footerTextY, footerTextPaint);
    }

    private void onDraw(@NonNull Canvas canvas) {
        fillPaint.setColor(Color.WHITE);
        canvas.drawRect(bounds, fillPaint);

        drawSegments(canvas);
        drawScore(canvas);
        drawFooter(canvas);
    }

    //endregion


    public static final class Result {
        public final Timeline timeline;
        public final Bitmap bitmap;

        public Result(@NonNull Timeline timeline,
                      @NonNull Bitmap bitmap) {
            this.timeline = timeline;
            this.bitmap = bitmap;
        }
    }
}
