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
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.util.Styles;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public final class ShareImage implements Observable.OnSubscribe<ShareImage.Result> {
    private final Context context;
    private final Timeline timeline;

    private final int width;
    private final int height;

    private final int scoreWidth;
    private final int scoreHeight;

    private final int baseSegmentHeight;

    private final Paint fillPaint = new Paint();
    private final Paint scoreFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF ovalRect = new RectF();
    private final Rect textRect = new Rect();

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

        Resources resources = context.getResources();
        this.width = resources.getDimensionPixelSize(R.dimen.share_image_width);
        this.height = resources.getDimensionPixelSize(R.dimen.share_image_height);

        this.scoreWidth = resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_width);
        this.scoreHeight = resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_height);

        scoreFillPaint.setColor(Color.WHITE);
        scoreFillPaint.setShadowLayer(20, 0, 0, 0x80009CFF);

        scoreTextPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_big_score));
        scoreTextPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));

        int minItemHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        this.baseSegmentHeight = Math.max(minItemHeight, height / Styles.TIMELINE_HOURS_ON_SCREEN);
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
            maxY = height,
            midY = maxY / 2;

        fillPaint.setColor(Color.WHITE);
        canvas.drawRect(minX, minY, maxX, maxY, fillPaint);

        if (!Lists.isEmpty(timeline.getSegments())) {
            int segmentMinY = minY;
            for (TimelineSegment segment : timeline.getSegments()) {
                int itemHeight = (int) ((segment.getDuration() / 3600f) * baseSegmentHeight);
                int segmentMaxY = segmentMinY + itemHeight;

                int sleepDepth = segment.getSleepDepth();
                int colorRes = Styles.getSleepDepthColorRes(sleepDepth, segment.isBeforeSleep());
                fillPaint.setColor(context.getResources().getColor(colorRes));

                float percentage = sleepDepth / 100f;
                float halfWidth = Math.round(maxX * percentage) / 2f;
                canvas.drawRect(midX - halfWidth, segmentMinY, midX + halfWidth, segmentMaxY, fillPaint);

                segmentMinY = segmentMaxY;

                if (segmentMaxY >= maxY) {
                    break;
                }
            }
        }

        ovalRect.set(midX - scoreWidth / 2f, midY - scoreHeight / 2f,
                midX + scoreWidth / 2f, midY + scoreHeight / 2f);
        canvas.drawOval(ovalRect, scoreFillPaint);

        int score = timeline.getScore();
        scoreTextPaint.setColor(Styles.getSleepScoreColor(context, score));
        String scoreText = Integer.toString(score);
        scoreTextPaint.getTextBounds(scoreText, 0, scoreText.length(), textRect);

        float scoreX = midX - textRect.centerX();
        float scoreY = midY - textRect.centerY();
        canvas.drawText(scoreText, scoreX, scoreY, scoreTextPaint);
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
