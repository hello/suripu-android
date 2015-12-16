package is.hello.sense.debug;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.DelegatingTimelineService;
import is.hello.sense.api.NonsenseTimelineService;
import is.hello.sense.api.TimelineService;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class TimelineSourceActivity extends InjectionActivity {
    @Inject DelegatingTimelineService delegatingTimelineService;
    @Inject OkHttpClient httpClient;
    @Inject Gson gson;

    private EditText hostText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline_source);

        this.hostText = (EditText) findViewById(R.id.activity_timeline_source_nonsense_host);
        hostText.setOnEditorActionListener(new EditorActionHandler(EditorInfo.IME_ACTION_DONE,
                                                                   this::useNonsense));

        final Button useHost = (Button) findViewById(R.id.activity_timeline_source_use_nonsense);
        Views.setSafeOnClickListener(useHost, ignored -> useNonsense());

        final Button useDefault = (Button) findViewById(R.id.activity_timeline_source_use_api);
        Views.setSafeOnClickListener(useDefault, ignored -> useApi());

        final TimelineService realTimelineService = delegatingTimelineService.getDelegate();
        if (realTimelineService instanceof NonsenseTimelineService) {
            final String host = ((NonsenseTimelineService) realTimelineService).getHost();
            hostText.setText(host);
        }
    }

    public void useNonsense() {
        final String url = hostText.getText().toString();
        if (TextUtils.isEmpty(url) || !URLUtil.isValidUrl(url)) {
            animatorFor(hostText)
                    .withDuration(Anime.DURATION_FAST)
                    .withInterpolator(new AccelerateInterpolator())
                    .scale(1.2f)
                    .andThen()
                    .withInterpolator(new LinearInterpolator())
                    .scale(0.8f)
                    .andThen()
                    .withInterpolator(new DecelerateInterpolator())
                    .scale(1f)
                    .start();
            return;
        }

        final NonsenseTimelineService nonsense = new NonsenseTimelineService(httpClient, gson, url);
        delegatingTimelineService.setDelegate(nonsense);

        setResult(RESULT_OK);
        finish();
    }

    public void useApi() {
        delegatingTimelineService.reset();

        setResult(RESULT_OK);
        finish();
    }
}
