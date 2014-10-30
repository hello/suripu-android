package is.hello.sense.ui.common;

import android.app.Fragment;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.Account;

public class AccountEditingFragment extends Fragment {
    protected Container getContainer() {
        if (getTargetFragment() != null) {
            return (Container) getTargetFragment();
        } else {
            return (Container) getActivity();
        }
    }

    public static interface Container {
        @NonNull Account getAccount();
        void onAccountUpdated(@NonNull AccountEditingFragment updatedBy);
    }
}
