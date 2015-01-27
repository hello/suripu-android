package is.hello.sense.ui.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.dialogs.WelcomeDialog;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public enum WelcomeDialogs {
    TEST(R.xml.welcome_test);

    private static final int MISSING_RES = 0;

    /**
     * The root element of a welcome dialog document.
     */
    private static final String TAG_ROOT = "dialog";

    /**
     * Describes a single item in a welcome dialog.
     */
    private static final String TAG_ITEM = "item";


    /**
     * A <code>@drawable</code> resource reference. Optional.
     */
    private static final String ATTR_ITEM_DIAGRAM_RES = "diagram";

    /**
     * A <code>@string</code> resource reference. Required.
     */
    private static final String ATTR_ITEM_TITLE_RES = "title";

    /**
     * A <code>@string</code> resource reference. Required.
     */
    private static final String ATTR_ITEM_MESSAGE_RES = "message";


    private final @XmlRes int documentRes;
    private WelcomeDialogs(@XmlRes int documentRes) {
        this.documentRes = documentRes;
    }


    private String getPreferenceKey() {
        return "welcome_dialog_" + toString().toLowerCase() + "_shown";
    }

    private int getResourceAttribute(@NonNull XmlResourceParser document,
                                     @NonNull String attributeName,
                                     boolean required) throws IOException {
        int resValue = document.getAttributeResourceValue(null, attributeName, MISSING_RES);
        if (required && resValue == MISSING_RES) {
            throw new IOException("Missing `" + attributeName + "` attribute");
        }
        return resValue;
    }

    private void markShown(@NonNull Context context) {

    }

    public boolean shouldShow(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        return preferences.getBoolean(getPreferenceKey(), false);
    }

    private WelcomeDialog.Item[] parseItemDocument(@NonNull XmlResourceParser document) throws XmlPullParserException, IOException {
        List<WelcomeDialog.Item> parsedItems = new ArrayList<>();
        boolean isLookingForItems = false;
        for (int event = document.getEventType(); event != XmlPullParser.END_DOCUMENT; event = document.next()) {
            if (event == XmlPullParser.START_TAG) {
                switch (document.getName()) {
                    case TAG_ROOT: {
                        isLookingForItems = true;
                        break;
                    }

                    case TAG_ITEM: {
                        if (!isLookingForItems) {
                            throw new IOException("Unexpected `item` outside of `dialog`");
                        }

                        int diagramRes = getResourceAttribute(document, ATTR_ITEM_DIAGRAM_RES, false);
                        int titleRes = getResourceAttribute(document, ATTR_ITEM_TITLE_RES, true);
                        int messageRes = getResourceAttribute(document, ATTR_ITEM_MESSAGE_RES, true);
                        parsedItems.add(new WelcomeDialog.Item(diagramRes, titleRes, messageRes));

                        break;
                    }

                    default: {
                        throw new IOException("Unknown element `" + document.getName() + "`");
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (TAG_ROOT.equals(document.getName())) {
                    isLookingForItems = false;
                }
            }
        }

        WelcomeDialog.Item[] items = new WelcomeDialog.Item[parsedItems.size()];
        parsedItems.toArray(items);
        return items;
    }

    public boolean showFrom(@NonNull Activity activity) {
        if (shouldShow(activity)) {
            return false;
        }

        XmlResourceParser resourceParser = activity.getResources().getXml(documentRes);
        try {
            WelcomeDialog.Item[] items = parseItemDocument(resourceParser);
            WelcomeDialog welcomeDialog = WelcomeDialog.newInstance(items);
            welcomeDialog.show(activity.getFragmentManager(), WelcomeDialog.TAG);

            markShown(activity);
        } catch (XmlPullParserException | IOException e) {
            Logger.error(getClass().getSimpleName(), "Could not parse welcome document", e);
            return false;
        }

        return true;
    }
}
