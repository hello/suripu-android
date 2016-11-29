package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable;

//todo erase this activity after testing
public class TestActivity extends SenseActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        final ImageView test1 = (ImageView) findViewById(R.id.activity_test_iv);
        final ImageView test2 = (ImageView) findViewById(R.id.activity_test_iv2);
        final ImageView test3 = (ImageView) findViewById(R.id.activity_test_iv3);
        final ImageView test4 = (ImageView) findViewById(R.id.activity_test_iv4);
        final ImageView test5 = (ImageView) findViewById(R.id.activity_test_iv5);

        setUp(test1);
        setUp(test2);
        setUp(test3);
        setUp(test4);
        setUp(test5);

    }

    private void setUp(final ImageView imageView) {
        final SleepScoreIconDrawable drawable = new SleepScoreIconDrawable(this);
        final ViewTreeObserver viewTreeObserver = imageView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                drawable.setHeight(imageView.getMeasuredHeight());
                drawable.setWidth(imageView.getMeasuredWidth());
                drawable.setIsSelected(true);
                drawable.setText("80");
                return true;
            }
        });
        imageView.setBackground(drawable);

    }
}
