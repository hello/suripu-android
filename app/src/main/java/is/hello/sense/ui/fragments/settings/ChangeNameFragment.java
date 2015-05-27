package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class ChangeNameFragment extends AccountEditingFragment {
    private TextView nameText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_name, container, false);

        this.nameText = (TextView) view.findViewById(R.id.fragment_change_name_value);
        nameText.setOnEditorActionListener(new EditorActionHandler(() -> submit(nameText)));
        nameText.setText(getContainer().getAccount().getName());

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
        if (TextUtils.isEmpty(nameText.getText())) {
            animate(nameText).simplePop(1.4f).start();
            return;
        }

        getContainer().getAccount().setName(nameText.getText().toString());
        getContainer().onAccountUpdated(this);
    }
}
