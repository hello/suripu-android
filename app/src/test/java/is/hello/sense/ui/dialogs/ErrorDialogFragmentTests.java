package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.util.Operation;
import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.ui.widget.SenseAlertDialog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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

    private static ErrorDialogFragment.Builder newBuilder(@Nullable Throwable e, @NonNull Resources resources) {
        return new TestErrorDialogFragment.Builder(e, resources);
    }

    private SenseAlertDialog show(@NonNull ErrorDialogFragment fragment) {
        fragment.showAllowingStateLoss(parent.getFragmentManager(), ErrorDialogFragment.TAG);

        final SenseAlertDialog dialog = getDialog(fragment);
        assertThat(dialog, is(notNullValue()));
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
        final ErrorDialogFragment dialogFragment = newBuilder()
                .withMessage(StringRef.from(MESSAGE))
                .build();
        final SenseAlertDialog dialog = show(dialogFragment);
        assertThat(dialog.getMessage().toString(), is(equalTo(MESSAGE)));
        assertThat(getTrackedError(dialogFragment), is(equalTo(new String[] {
                MESSAGE,
                null,
                null,
                null,
        })));
    }

    @Test
    public void simpleError() throws Exception {
        final IllegalStateException e = new IllegalStateException(MESSAGE);
        final ErrorDialogFragment dialogFragment = newBuilder(e, getResources()).build();
        final SenseAlertDialog dialog = show(dialogFragment);
        assertThat(dialog.getMessage().toString(), is(equalTo(MESSAGE)));
        assertThat(getTrackedError(dialogFragment), is(equalTo(new String[] {
                MESSAGE,
                "java.lang.IllegalStateException",
                null,
                null,
        })));
    }

    @Test
    public void errorWithReporting() throws Exception {
        final ReportingException e = new ReportingException(MESSAGE);
        final ErrorDialogFragment dialogFragment = newBuilder(e, getResources())
                .withOperation("Testing")
                .build();
        final SenseAlertDialog dialog = show(dialogFragment);
        assertThat(dialog.getMessage().toString(), is(equalTo("Try something else.")));
        assertThat(getTrackedError(dialogFragment), is(equalTo(new String[] {
                "Try something else.",
                "is.hello.sense.ui.dialogs.ErrorDialogFragmentTests.ReportingException",
                "Unit Testing",
                "Testing",
        })));
    }

    @Test
    public void nullError() throws Exception {
        final ErrorDialogFragment dialogFragment = newBuilder(null, getResources()).build();
        final SenseAlertDialog dialog = show(dialogFragment);
        assertThat(dialog.getMessage().toString(), is(equalTo("An unknown error has occurred.")));
        assertThat(getTrackedError(dialogFragment), is(equalTo(new String[] {
                "An unknown error has occurred.",
                null,
                null,
                null,
        })));
    }

    @Test
    public void fatalBluetoothError() throws Exception {
        final GattException e = new GattException(GattException.GATT_STACK_ERROR,
                                                  Operation.CONNECT);
        final ErrorDialogFragment dialogFragment = newBuilder(e, getResources())
                .withOperation("Testing")
                .build();
        final SenseAlertDialog dialog = show(dialogFragment);
        final String expectedMessage =
                "An unknown error occurred with your device's Bluetooth, please try again." +
                "\\n\\nPlease turn airplane mode on for a few seconds, turn it off, and then try again. " +
                "If this problem persists, please visit our support site.";
        assertThat(dialog.getMessage().toString(), is(equalTo(expectedMessage)));
        assertThat(getTrackedError(dialogFragment), is(equalTo(new String[] {
                expectedMessage,
                "is.hello.buruberi.bluetooth.errors.GattException",
                "CONNECT: GATT_STACK_ERROR",
                "Testing",
        })));

        final Button action = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(action.getText().toString(), is(equalTo("More info")));
    }

    @Test
    public void addendum() throws Exception {
        final IllegalStateException e = new IllegalStateException(MESSAGE);
        final ErrorDialogFragment dialogFragment = newBuilder(e, getResources())
                .withAddendum(R.string.error_addendum_unstable_stack)
                .build();
        final SenseAlertDialog dialog = show(dialogFragment);
        final String expectedMessage = MESSAGE + "\\n\\nPlease turn airplane mode on for a few seconds, " +
                "turn it off, and then try again. If this problem persists, please visit our support site.";
        assertThat(dialog.getMessage().toString(), is(equalTo(expectedMessage)));
        assertThat(getTrackedError(dialogFragment), is(equalTo(new String[] {
                expectedMessage,
                "java.lang.IllegalStateException",
                null,
                null,
        })));
    }

    @Test
    public void supportLink() throws Exception {
        IllegalStateException e = new IllegalStateException(MESSAGE);
        ErrorDialogFragment dialogFragment = newBuilder(e, getResources())
                .withSupportLink()
                .build();
        SenseAlertDialog dialog = show(dialogFragment);
        String expectedMessage = MESSAGE + "\\n\\nHaving trouble? Contact support";
        assertThat(dialog.getMessage().toString(), is(equalTo(expectedMessage)));
        assertThat(getTrackedError(dialogFragment), is(equalTo(new String[] {
                expectedMessage,
                "java.lang.IllegalStateException",
                null,
                null,
        })));
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
                        @Nullable String errorOperation,
                        boolean isWarning) {
            this.trackedError = new String[] { message, errorType, errorContext, errorOperation };
        }

        public static class Builder extends ErrorDialogFragment.Builder {
            public Builder() {
            }

            public Builder(@Nullable Throwable e, @NonNull Resources resources) {
                super(e, resources);
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
