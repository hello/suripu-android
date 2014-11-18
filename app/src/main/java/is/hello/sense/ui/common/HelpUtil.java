package is.hello.sense.ui.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

public class HelpUtil {
    private static final String HELP_URL = "https://docs.google.com/document/d/1OEIDKSq6iBgH47-cctQ4TDom-iSlmkxFJfQ5rvegbO8/edit?usp=sharing";

    public static void showHelp(@NonNull Context from) {
        from.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_URL)));
    }
}
