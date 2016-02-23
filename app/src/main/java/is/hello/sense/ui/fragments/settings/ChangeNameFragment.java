package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ChangeNameFragment extends SenseFragment {
    private TextView nameText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_name, container, false);

        this.nameText = (TextView) view.findViewById(R.id.fragment_change_name_value);
        nameText.setOnEditorActionListener(new EditorActionHandler(() -> submit(nameText)));
        nameText.setText(AccountEditor.getContainer(this).getAccount().getName());

        Button submit = (Button) view.findViewById(R.id.fragment_change_name_submit);
        Views.setSafeOnClickListener(submit, this::submit);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.nameText = null;
    }

    public void submit(@NonNull View sender) {
        String name = AccountPresenter.normalizeInput(nameText.getText());
        nameText.setText(name);
        if (!AccountPresenter.validateName(name)) {
            animatorFor(nameText)
                    .scale(1.4f)
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            animatorFor(nameText)
                                    .scale(1.0f)
                                    .start();
                        }
                    })
                    .start();
            return;
        }

        final AccountEditor.Container container = AccountEditor.getContainer(this);
        container.getAccount().setName(name);
        container.onAccountUpdated(this);
    }
}
