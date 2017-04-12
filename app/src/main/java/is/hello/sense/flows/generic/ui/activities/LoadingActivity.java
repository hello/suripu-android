package is.hello.sense.flows.generic.ui.activities;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.flows.generic.interactors.LoadingInteractor;
import is.hello.sense.ui.activities.appcompat.InjectionActivity;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

public class LoadingActivity extends InjectionActivity {
    private static final String EXTRA_MESSAGE = LoadingActivity.class.getSimpleName() + ".EXTRA_MESSAGE";
    private static final int MIN_DURATION_MS = 1000;

    public static void startActivity(@NonNull final Context context,
                                     @NonNull final String message) {
        final Intent intent = new Intent(context, LoadingActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.startActivity(intent);
    }

    @Inject
    LoadingInteractor loadingInteractor;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || !getIntent().hasExtra(EXTRA_MESSAGE)) {
            finish();
            return;
        }
        final String message = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (message == null) {
            finish();
            return;
        }
        LoadingDialogFragment.show(getFragmentManager(),
                                   message,
                                   LoadingDialogFragment.DEFAULTS);
        bindAndSubscribe(loadingInteractor.sub,
                         this::closeWithDone,
                         (e) -> this.closeWithDone(null));
    }

    public void closeWithDone(final VoidResponse ignored) {
        new Handler().postDelayed(() -> LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), this::finish), MIN_DURATION_MS);
    }
}
