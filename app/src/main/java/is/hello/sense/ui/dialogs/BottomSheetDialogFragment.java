package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.ArrayList;

import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.StringRef;

import static is.hello.sense.ui.widget.SenseBottomSheet.Option;

public class BottomSheetDialogFragment extends SenseDialogFragment implements SenseBottomSheet.OnOptionSelectedListener {
    public static final String TAG = BottomSheetDialogFragment.class.getSimpleName();

    public static final String RESULT_POSITION = BottomSheetDialogFragment.class.getName() + ".RESULT_POSITION";
    public static final String RESULT_OPTION = BottomSheetDialogFragment.class.getName() + ".RESULT_OPTION";

    private static final String ARG_TITLE = BottomSheetDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_OPTIONS = BottomSheetDialogFragment.class.getName() + ".ARG_OPTIONS";

    //region Lifecycle

    public static BottomSheetDialogFragment newInstance(@NonNull StringRef title,
                                                        @NonNull ArrayList<Option> options) {
        BottomSheetDialogFragment dialogFragment = new BottomSheetDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_TITLE, title);
        arguments.putSerializable(ARG_OPTIONS, options);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
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
            bottomSheet.setTitle(title.resolve(getActivity()));

            //noinspection unchecked
            ArrayList<Option> options = (ArrayList<Option>) getArguments().getSerializable(ARG_OPTIONS);
            bottomSheet.addOptions(options);
        }

        return bottomSheet;
    }

    //endregion


    @Override
    public void onOptionSelected(int position, @NonNull Option option) {
        if (getTargetFragment() != null) {
            Intent result = new Intent();
            result.putExtra(RESULT_POSITION, position);
            result.putExtra(RESULT_OPTION, option);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, result);
        }
    }
}
