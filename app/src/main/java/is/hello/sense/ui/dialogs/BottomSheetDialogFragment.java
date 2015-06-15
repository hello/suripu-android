package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.util.ArrayList;

import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.StringRef;

import static is.hello.sense.ui.widget.SenseBottomSheet.Option;

public class BottomSheetDialogFragment extends SenseDialogFragment implements SenseBottomSheet.OnOptionSelectedListener {
    public static final String TAG = BottomSheetDialogFragment.class.getSimpleName();

    public static final String RESULT_OPTION_ID = BottomSheetDialogFragment.class.getName() + ".RESULT_OPTION_ID";
    public static final String RESULT_AFFECTED_POSITION = BottomSheetDialogFragment.class.getName() + ".RESULT_AFFECTED_POSITION";

    private static final String ARG_TITLE = BottomSheetDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_OPTIONS = BottomSheetDialogFragment.class.getName() + ".ARG_OPTIONS";
    private static final String ARG_WANTS_DIVIDERS = BottomSheetDialogFragment.class.getName() + ".ARG_WANTS_DIVIDERS";
    private static final String ARG_AFFECTED_POSITION = BottomSheetDialogFragment.class.getName() + ".ARG_AFFECTED_POSITION";

    //region Lifecycle

    public static BottomSheetDialogFragment newInstance(@Nullable StringRef title,
                                                        @NonNull ArrayList<Option> options) {
        BottomSheetDialogFragment dialogFragment = new BottomSheetDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_TITLE, title);
        arguments.putSerializable(ARG_OPTIONS, options);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    public static BottomSheetDialogFragment newInstance(@NonNull ArrayList<Option> options) {
        return newInstance((StringRef) null, options);
    }

    public static BottomSheetDialogFragment newInstance(@NonNull String title,
                                                        @NonNull ArrayList<Option> options) {
        return newInstance(StringRef.from(title), options);
    }

    public static BottomSheetDialogFragment newInstance(@StringRes int titleRes,
                                                        @NonNull ArrayList<Option> options) {
        return newInstance(StringRef.from(titleRes), options);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseBottomSheet bottomSheet = new SenseBottomSheet(getActivity());
        bottomSheet.setOnOptionSelectedListener(this);

        if (savedInstanceState == null) {
            StringRef title = (StringRef) getArguments().getSerializable(ARG_TITLE);
            if (title != null) {
                bottomSheet.setTitle(title.resolve(getActivity()));
            } else {
                bottomSheet.setTitle(null);
            }
            bottomSheet.setWantsDividers(getArguments().getBoolean(ARG_WANTS_DIVIDERS, false));

            //noinspection unchecked
            ArrayList<Option> options = (ArrayList<Option>) getArguments().getSerializable(ARG_OPTIONS);
            bottomSheet.addOptions(options);
        }

        return bottomSheet;
    }

    public void setWantsDividers(boolean wantsDividers) {
        getArguments().putBoolean(ARG_WANTS_DIVIDERS, wantsDividers);
    }

    public void setAffectedPosition(int affectedPosition) {
        getArguments().putInt(ARG_AFFECTED_POSITION, affectedPosition);
    }

    //endregion


    @Override
    public void onOptionSelected(int position, @NonNull Option option) {
        if (getTargetFragment() != null) {
            Intent result = new Intent();
            result.putExtra(RESULT_OPTION_ID, option.getOptionId());
            result.putExtra(RESULT_AFFECTED_POSITION, getArguments().getInt(ARG_AFFECTED_POSITION, 0));
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, result);
        }
    }
}
