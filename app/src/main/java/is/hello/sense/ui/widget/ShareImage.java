package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

public final class ShareImage implements Observable.OnSubscribe<ShareImage.Result> {
    private final Context context;
    private final Timeline timeline;
    private final Resources resources;

    private final int width;
    private final int height;

    private final int scoreWidth;
    private final int scoreHeight;
    private final int scoreTextHeight;
    private final int scoreLabelTextHeight;

    private final int baseSegmentHeight;
    private final int footerHeight;
    private final int footerTextHeight;
    private final int footerInset;

    private final String scoreLabel;

    private final String leftFooterText;
    private final String rightFooterText;
    private final Drawable footerLogo;

    private final Paint fillPaint = new Paint();
    private final Paint shadowedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint scoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint footerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF ovalRect = new RectF();

    //region Creation

    public static Observable<Result> forTimeline(@NonNull Context context, @NonNull Timeline timeline) {
        return Observable.create(new ShareImage(context, timeline))
                         .subscribeOn(Schedulers.computation())
                         .observeOn(AndroidSchedulers.mainThread());
    }

    private ShareImage(@NonNull Context context,
                       @NonNull Timeline timeline) {
        this.context = context;
        this.timeline = timeline;

        this.resources = context.getResources();
        Paint.FontMetricsInt fontMetrics = new Paint.FontMetricsInt();
        this.width = resources.getDimensionPixelSize(R.dimen.share_image_width);
        this.height = resources.getDimensionPixelSize(R.dimen.share_image_height);

        this.scoreWidth = resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_width);
        this.scoreHeight = resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_height);

        this.footerHeight = resources.getDimensionPixelSize(R.dimen.share_image_footer_height);
        this.footerInset = resources.getDimensionPixelSize(R.dimen.share_image_footer_inset);

        shadowedFillPaint.setColor(Color.WHITE);
        shadowedFillPaint.setShadowLayer(20, 0, 0, resources.getColor(R.color.share_image_shadow));

        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        labelTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        labelTextPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_section_heading));
        labelTextPaint.setColor(resources.getColor(R.color.text_section_header));
        labelTextPaint.getFontMetricsInt(fontMetrics);
        this.scoreLabelTextHeight = fontMetrics.top + fontMetrics.descent;

        this.scoreLabel = resources.getString(R.string.sleep_score).toUpperCase();

        scoreTextPaint.setTextAlign(Paint.Align.CENTER);
        scoreTextPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_big_score));
        scoreTextPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        scoreTextPaint.getFontMetricsInt(fontMetrics);
        this.scoreTextHeight = fontMetrics.top + fontMetrics.descent;

        footerTextPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_body));
        footerTextPaint.setColor(resources.getColor(R.color.text_light));
        footerTextPaint.getFontMetricsInt(fontMetrics);
        this.footerTextHeight = fontMetrics.top + fontMetrics.descent;

        int minItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        this.baseSegmentHeight = Math.max(minItemHeight, height / Styles.TIMELINE_HOURS_ON_SCREEN) * 4;

        this.leftFooterText = DateFormat.getLongDateFormat(context)
                                        .format(timeline.getDate().getMillis());
        this.rightFooterText = UserSupport.COMPANY_URL;

        this.footerLogo = resources.getDrawable(R.drawable.share_company_logo);
    }

    //endregion


    //region Rendering

    @Override
    public void call(Subscriber<? super Result> s) {
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        onDraw(canvas);

        s.onNext(new Result(timeline, image));
        s.onCompleted();
    }

    private void onDraw(@NonNull Canvas canvas) {
        int minX = 0,
            maxX = width,
            midX = maxX / 2;
        int minY = 0,
            maxY = height - footerHeight,
            midY = maxY / 2;


        // Background

        fillPaint.setColor(Color.WHITE);
        canvas.drawRect(minX, minY, maxX, maxY, fillPaint);


        // Segments

        if (!Lists.isEmpty(timeline.getSegments())) {
            int segmentMinY = minY;
            for (TimelineSegment segment : timeline.getSegments()) {
                if (segment.getDuration() <= 120) {
                    continue;
                }

                int itemHeight = (int) ((segment.getDuration() / 3600f) * baseSegmentHeight);
                int segmentMaxY = segmentMinY + itemHeight;

                int sleepDepth = segment.getSleepDepth();
                int colorRes = Styles.getSleepDepthColorRes(sleepDepth, false);
                fillPaint.setColor(resources.getColor(colorRes));

                float percentage = sleepDepth / 100f;
                float halfWidth = Math.round(maxX * percentage) / 2f;
                canvas.drawRect(midX - halfWidth, segmentMinY,
                                midX + halfWidth, segmentMaxY,
                                fillPaint);

                segmentMinY = segmentMaxY;

                if (segmentMaxY >= maxY) {
                    break;
                }
            }
        }

        fillPaint.setColor(resources.getColor(R.color.share_image_tint));
        canvas.drawRect(minX, minY, maxX, maxY, fillPaint);


        // Score

        ovalRect.set(midX - scoreWidth / 2f, midY - scoreHeight / 2f,
                     midX + scoreWidth / 2f, midY + scoreHeight / 2f);
        canvas.drawOval(ovalRect, shadowedFillPaint);

        int score = timeline.getScore();
        String scoreText = Integer.toString(score);
        scoreTextPaint.setColor(Styles.getSleepScoreColor(context, score));
        float scoreTextY = midY - scoreTextHeight / 2f - scoreLabelTextHeight;
        canvas.drawText(scoreText, midX, scoreTextY, scoreTextPaint);

        canvas.drawText(scoreLabel, midX, (midY + scoreTextHeight / 2f) + scoreLabelTextHeight, labelTextPaint);


        //region Footer

        canvas.drawRect(minX, maxY, maxX, maxY + footerHeight, shadowedFillPaint);

        float footerTextY = maxY + ((footerHeight / 2f) - footerTextHeight / 2f);

        footerTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(leftFooterText, minX + footerInset, footerTextY, footerTextPaint);

        int footerMaxX = maxX - footerInset;
        int footerMidY = maxY + (footerHeight / 2);
        int logoWidth = footerLogo.getIntrinsicWidth();
        int logoHeight = footerLogo.getIntrinsicHeight();
        footerLogo.setBounds(
            footerMaxX - logoWidth,
            footerMidY - logoHeight / 2,
            footerMaxX,
            footerMidY + logoHeight / 2
        );
        footerLogo.draw(canvas);

        footerTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(rightFooterText, footerMaxX - logoWidth - footerInset, footerTextY, footerTextPaint);
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
