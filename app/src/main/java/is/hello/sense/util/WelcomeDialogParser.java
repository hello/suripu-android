package is.hello.sense.util;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import is.hello.sense.ui.dialogs.WelcomeDialog;

public class WelcomeDialogParser {
    //region Constants

    /**
     * Placeholder for a missing resource.
     */
    public static final int MISSING_RES = 0;


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
     * A <code>@string</code> resource reference. Optional.
     */
    private static final String ATTR_ITEM_TITLE_RES = "title";

    /**
     * A <code>@string</code> resource reference. Required.
     */
    private static final String ATTR_ITEM_MESSAGE_RES = "message";

    //endregion

    private final Resources resources;
    private final @XmlRes int xmlRes;

    public WelcomeDialogParser(@NonNull Resources resources,
                               @XmlRes int xmlRes) {
        this.resources = resources;
        this.xmlRes = xmlRes;
    }


    //region Parsing

    private int getResourceAttribute(@NonNull XmlResourceParser document,
                                     @NonNull String attributeName,
                                     boolean required) throws IOException {
        int resValue = document.getAttributeResourceValue(null, attributeName, MISSING_RES);
        if (required && resValue == MISSING_RES) {
            throw new IOException("Missing `" + attributeName + "` attribute");
        }
        return resValue;
    }

    private WelcomeDialog.Item parseItem(@NonNull XmlResourceParser document) throws IOException {
        int diagramRes = getResourceAttribute(document, ATTR_ITEM_DIAGRAM_RES, false);
        int titleRes = getResourceAttribute(document, ATTR_ITEM_TITLE_RES, false);
        int messageRes = getResourceAttribute(document, ATTR_ITEM_MESSAGE_RES, true);
        return new WelcomeDialog.Item(diagramRes, titleRes, messageRes);
    }

    private WelcomeDialog.Item[] parseDialog(@NonNull XmlResourceParser document) throws XmlPullParserException, IOException {
        List<WelcomeDialog.Item> parsedItems = new ArrayList<>();
        boolean lookingForItems = false;
        for (int event = document.getEventType(); event != XmlPullParser.END_DOCUMENT; event = document.next()) {
            if (event == XmlPullParser.START_TAG) {
                switch (document.getName()) {
                    case TAG_ROOT: {
                        lookingForItems = true;
                        break;
                    }

                    case TAG_ITEM: {
                        if (!lookingForItems) {
                            throw new IOException("Unexpected `" + TAG_ITEM + "` outside of `" + TAG_ROOT + "`");
                        }
                        parsedItems.add(parseItem(document));
                        break;
                    }

                    default: {
                        throw new IOException("Unknown element `" + document.getName() + "`");
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (TAG_ROOT.equals(document.getName())) {
                    lookingForItems = false;
                }
            }
        }

        WelcomeDialog.Item[] items = new WelcomeDialog.Item[parsedItems.size()];
        parsedItems.toArray(items);
        return items;
    }

    public WelcomeDialog.Item[] parse() throws XmlPullParserException, IOException {
        XmlResourceParser document = resources.getXml(xmlRes);
        return parseDialog(document);
    }

    //endregion
}
