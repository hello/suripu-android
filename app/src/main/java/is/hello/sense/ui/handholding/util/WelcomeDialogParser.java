package is.hello.sense.ui.handholding.util;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import is.hello.sense.ui.handholding.WelcomeDialogFragment;

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
    private static final String ATTR_ITEM_DIAGRAM_VIDEO = "diagramVideo";

    /**
     * A <code>@color</code> resource reference. Optional.
     */
    private static final String ATTR_ITEM_DIAGRAM_FILL_COLOR = "diagramFillColor";

    /**
     * A boolean. Optional. Defaults to true.
     */
    private static final String ATTR_ITEM_SCALE_DIAGRAM = "scaleDiagram";

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

    private int getColorAttribute(@NonNull XmlResourceParser document,
                                  @NonNull String attributeName,
                                  boolean required) throws IOException {
        final String value = document.getAttributeValue(null, attributeName);
        if (value == null) {
            if (required) {
                throw new IOException("Missing `" + attributeName + "` attribute");
            } else {
                return Color.TRANSPARENT;
            }
        }

        if (value.startsWith("#")) {
            return Color.parseColor(value);
        } else if (value.startsWith("@")) {
            int resValue = document.getAttributeResourceValue(null, attributeName, MISSING_RES);
            if (resValue == MISSING_RES) {
                throw new IOException("Malformed `" + attributeName + "` attribute");
            }

            return resources.getColor(resValue);
        } else {
            throw new IOException("Malformed `" + attributeName + "` attribute");
        }
    }

    private String getStringAttribute(@NonNull XmlResourceParser document,
                                      @NonNull String attributeName,
                                      boolean required) throws IOException {
        final String value = document.getAttributeValue(null, attributeName);
        if (value == null) {
            if (required) {
                throw new IOException("Missing `" + attributeName + "` attribute");
            } else {
                return null;
            }
        }

        if (value.startsWith("@")) {
            int resValue = document.getAttributeResourceValue(null, attributeName, MISSING_RES);
            if (resValue == MISSING_RES) {
                throw new IOException("Malformed `" + attributeName + "` attribute");
            }

            return resources.getString(resValue);
        } else {
            return value;
        }
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

    private WelcomeDialogFragment.Item parseItem(@NonNull XmlResourceParser document) throws IOException {
        final int diagramRes = getResourceAttribute(document, ATTR_ITEM_DIAGRAM_RES, false);
        final int titleRes = getResourceAttribute(document, ATTR_ITEM_TITLE_RES, false);
        final int messageRes = getResourceAttribute(document, ATTR_ITEM_MESSAGE_RES, true);
        final String diagramVideo = getStringAttribute(document, ATTR_ITEM_DIAGRAM_VIDEO, false);
        final boolean scaleDiagram = document.getAttributeBooleanValue(null, ATTR_ITEM_SCALE_DIAGRAM, true);
        final int diagramFillColor = getColorAttribute(document, ATTR_ITEM_DIAGRAM_FILL_COLOR, false);
        return new WelcomeDialogFragment.Item(diagramRes,
                                              titleRes,
                                              messageRes,
                                              scaleDiagram,
                                              diagramVideo,
                                              diagramFillColor);
    }

    private ArrayList<WelcomeDialogFragment.Item> parseDialog(@NonNull XmlResourceParser document) throws XmlPullParserException, IOException {
        ArrayList<WelcomeDialogFragment.Item> parsedItems = new ArrayList<>();
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

        return parsedItems;
    }

    public ArrayList<WelcomeDialogFragment.Item> parse() throws XmlPullParserException, IOException {
        XmlResourceParser document = resources.getXml(xmlRes);
        return parseDialog(document);
    }

    //endregion
}
