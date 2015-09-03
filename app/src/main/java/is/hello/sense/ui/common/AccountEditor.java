package is.hello.sense.ui.common;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.Account;

public final class AccountEditor {
    public static final String ARG_WANTS_SKIP_BUTTON = AccountEditor.class.getSimpleName() + ".ARG_WANTS_SKIP_BUTTON";

    public static void setWantsSkipButton(@NonNull SenseFragment fragment, boolean wantsSkipButton) {
        Bundle arguments = fragment.getArguments();
        if (arguments == null) {
            arguments = new Bundle();
            fragment.setArguments(arguments);
        }

        arguments.putBoolean(ARG_WANTS_SKIP_BUTTON, wantsSkipButton);
    }

    public static boolean getWantsSkipButton(@NonNull SenseFragment fragment) {
        final Bundle arguments = fragment.getArguments();
        return (arguments == null || arguments.getBoolean(ARG_WANTS_SKIP_BUTTON, true));
    }

    public static Container getContainer(@NonNull SenseFragment fragment) {
        final Fragment targetFragment = fragment.getTargetFragment();
        if (targetFragment instanceof Container) {
            return (Container) targetFragment;
        } else {
            return (Container) fragment.getActivity();
        }
    }

    public interface Container {
        @NonNull Account getAccount();
        void onAccountUpdated(@NonNull SenseFragment updatedBy);
    }
}
