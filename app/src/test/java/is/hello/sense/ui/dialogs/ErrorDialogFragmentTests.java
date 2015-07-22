package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import is.hello.buruberi.bluetooth.errors.BluetoothGattError;
import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.ui.widget.SenseAlertDialog;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ErrorDialogFragmentTests extends SenseTestCase {
    private static final String MESSAGE = "Could not reverse polarity of the neutron flow.";

    private Activity parent;

    @Before
    public void setUp() {
        parent = Robolectric.setupActivity(Activity.class);
    }


    //region Util

    private static ErrorDialogFragment.Builder newBuilder() {
        return new TestErrorDialogFragment.Builder();
    }

    private static ErrorDialogFragment.Builder newBuilder(@Nullable Throwable e) {
        return new TestErrorDialogFragment.Builder(e);
    }

    private SenseAlertDialog show(@NonNull ErrorDialogFragment fragment) {
        fragment.showAllowingStateLoss(parent.getFragmentManager(), ErrorDialogFragment.TAG);

        SenseAlertDialog dialog = getDialog(fragment);
        assertNotNull(dialog);
        return dialog;
    }

    private static SenseAlertDialog getDialog(@NonNull ErrorDialogFragment fragment) {
        return (SenseAlertDialog) fragment.getDialog();
    }

    private static @Nullable String[] getTrackedError(@NonNull ErrorDialogFragment fragment) {
        return ((TestErrorDialogFragment) fragment).trackedError;
    }

    //endregion


    @Test
    public void messageOnly() throws Exception {
        ErrorDialogFragment dialogFragment = newBuilder()
                .withMessage(StringRef.from(MESSAGE))
                .build();
        SenseAlertDialog dialog = show(dialogFragment);
        assertEquals(MESSAGE, dialog.getMessage().toString());
        assertArrayEquals(new String[] {MESSAGE, null, null, null},
                getTrackedError(dialogFragment));
    }

    @Test
    public void simpleError() throws Exception {
        IllegalStateException e = new IllegalStateException(MESSAGE);
        ErrorDialogFragment dialogFragment = newBuilder(e).build();
        SenseAlertDialog dialog = show(dialogFragment);
        assertEquals(MESSAGE, dialog.getMessage().toString());
        assertArrayEquals(new String[]{MESSAGE, "java.lang.IllegalStateException", null, null},
                getTrackedError(dialogFragment));
    }

    @Test
    public void errorWithReporting() throws Exception {
        ReportingException e = new ReportingException(MESSAGE);
        ErrorDialogFragment dialogFragment = newBuilder(e)
                .withOperation("Testing")
                .build();
        SenseAlertDialog dialog = show(dialogFragment);
        assertEquals("Try something else.", dialog.getMessage().toString());
        assertArrayEquals(new String[]{
                        "Try something else.",
                        "is.hello.sense.ui.dialogs.ErrorDialogFragmentTests.ReportingException",
                        "Unit Testing",
                        "Testing"
                },
                getTrackedError(dialogFragment));
    }

    @Test
    public void nullError() throws Exception {
        ErrorDialogFragment dialogFragment = newBuilder(null).build();
        SenseAlertDialog dialog = show(dialogFragment);
        assertEquals("An unknown error has occurred.", dialog.getMessage().toString());
        assertArrayEquals(new String[]{"An unknown error has occurred.", null, null, null},
                getTrackedError(dialogFragment));
    }

    @Test
    public void fatalBluetoothError() throws Exception {
        BluetoothGattError e = new BluetoothGattError(BluetoothGattError.GATT_STACK_ERROR,
                BluetoothGattError.Operation.CONNECT);
        ErrorDialogFragment dialogFragment = newBuilder(e)
                .withOperation("Testing")
                .build();
        SenseAlertDialog dialog = show(dialogFragment);
        String expectedMessage = "An unknown error occurred with your device's Bluetooth, please try again." +
                "\\n\\nPlease turn airplane mode on for a few seconds, turn it off, and then try again. " +
                "If this problem persists, please visit our support site.";
        assertEquals(expectedMessage, dialog.getMessage().toString());
        assertArrayEquals(new String[]{
                        expectedMessage,
                        "is.hello.buruberi.bluetooth.errors.BluetoothGattError",
                        "CONNECT: GATT_STACK_ERROR",
                        "Testing"
                },
                getTrackedError(dialogFragment));

        Button action = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertEquals("More info", action.getText().toString());
    }

    @Test
    public void addendum() throws Exception {
        IllegalStateException e = new IllegalStateException(MESSAGE);
        ErrorDialogFragment dialogFragment = newBuilder(e)
                .withAddendum(R.string.error_addendum_unstable_stack)
                .build();
        SenseAlertDialog dialog = show(dialogFragment);
        String expectedMessage = MESSAGE + "\\n\\nPlease turn airplane mode on for a few seconds, " +
                "turn it off, and then try again. If this problem persists, please visit our support site.";
        assertEquals(expectedMessage, dialog.getMessage().toString());
        assertArrayEquals(new String[]{expectedMessage, "java.lang.IllegalStateException", null, null},
                getTrackedError(dialogFragment));
    }

    @Test
    public void supportLink() throws Exception {
        IllegalStateException e = new IllegalStateException(MESSAGE);
        ErrorDialogFragment dialogFragment = newBuilder(e)
                .withSupportLink()
                .build();
        SenseAlertDialog dialog = show(dialogFragment);
        String expectedMessage = MESSAGE + "\\n\\nHaving trouble? Email support";
        assertEquals(expectedMessage, dialog.getMessage().toString());
        assertArrayEquals(new String[]{expectedMessage, "java.lang.IllegalStateException", null, null},
                getTrackedError(dialogFragment));
    }


    public static class ReportingException extends IllegalStateException implements Errors.Reporting {
        public ReportingException(String detailMessage) {
            super(detailMessage);
        }

        @Nullable
        @Override
        public String getContextInfo() {
            return "Unit Testing";
        }

        @NonNull
        @Override
        public StringRef getDisplayMessage() {
            return StringRef.from("Try something else.");
        }
    }

    public static class TestErrorDialogFragment extends ErrorDialogFragment {
        @Nullable String[] trackedError;

        @Override
        void trackError(@NonNull String message,
                        @Nullable String errorType,
                        @Nullable String errorContext,
                        @Nullable String errorOperation) {
            this.trackedError = new String[] { message, errorType, errorContext, errorOperation };
        }

        public static class Builder extends ErrorDialogFragment.Builder {
            public Builder() {
            }

            public Builder(@Nullable Throwable e) {
                super(e);
            }

            @Override
            public ErrorDialogFragment build() {
                TestErrorDialogFragment instance = new TestErrorDialogFragment();
                instance.setArguments(arguments);
                return instance;
            }
        }
    }
}
