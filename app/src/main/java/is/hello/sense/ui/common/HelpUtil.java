package is.hello.sense.ui.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.api.model.Enums;
import is.hello.sense.util.Analytics;

public class HelpUtil {
    private static final String HELP_URL = "https://docs.google.com/document/d/1OEIDKSq6iBgH47-cctQ4TDom-iSlmkxFJfQ5rvegbO8/edit?usp=sharing";

    public static void showHelp(@NonNull Context from, @NonNull Step step) {
        Analytics.event(Analytics.EVENT_HELP, Analytics.createProperties(Analytics.PROP_HELP_STEP, step.toString()));
        from.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_URL)));
    }


    public static enum Step {
        ONBOARDING_INFO,
        ONBOARDING_SETUP_SENSE,
        ONBOARDING_WIFI_SCAN,
        ONBOARDING_SIGN_INTO_WIFI,
        ONBOARDING_PILL_PLACEMENT,
        SETTINGS;


        public static Step fromString(@Nullable String string) {
            return Enums.fromString(string, values(), ONBOARDING_INFO);
        }
    }
}
