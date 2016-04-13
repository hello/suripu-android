package is.hello.sense.ui.activities;

;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;

import static is.hello.sense.ui.recycler.FadingEdgesItemDecoration.*;

public class ListActivity extends InjectionActivity {

    private TextView selectedTextView;

    private String selectedName;
    private ArrayList<String> list;
    private int itemVerticalPadding;
    private int itemHorizontalPadding;

    private static final String ARG_LIST = ListActivity.class.getName() + ".ARG_LIST";
    private static final String ARG_SELECTED_NAME = ListActivity.class.getName() + ".ARG_SELECTED_NAME";
    private static final String ARG_TITLE = ListActivity.class.getName() + ".ARG_TITLE";

    public static final String VALUE_NAME = ListActivity.class.getName() + ".VALUE_NAME";


    public static void startActivityForResult(
            final @NonNull InjectionFragment fragment,
            final int requestCode,
            final @StringRes int titleRes,
            final @NonNull String selectedName,
            final @NonNull List<?> list) {

        final ArrayList<String> newList = new ArrayList<>();
        for (Object object : list) {
            newList.add(object.toString());
        }
        final Intent intent = new Intent(fragment.getActivity(), ListActivity.class);
        intent.putStringArrayListExtra(ARG_LIST, newList);
        intent.putExtra(ARG_SELECTED_NAME, selectedName);
        intent.putExtra(ARG_TITLE, titleRes);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        final Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        list = intent.getStringArrayListExtra(ARG_LIST);
        selectedName = intent.getStringExtra(ARG_SELECTED_NAME);
        final int titleRes = intent.getIntExtra(ARG_TITLE, R.string.empty);

        this.itemHorizontalPadding = getResources().getDimensionPixelSize(R.dimen.gap_outer);
        this.itemVerticalPadding = getResources().getDimensionPixelSize(R.dimen.gap_outer_half);
        final TextView title = (TextView) findViewById(R.id.item_section_title_text);
        title.setText(titleRes);
        findViewById(R.id.item_section_title_divider).setVisibility(View.GONE);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_list_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, getResources(),
                                                                     EnumSet.of(ScrollEdge.TOP), Style.STRAIGHT));

        if (list == null) {
            list = new ArrayList<>();
        }
        recyclerView.setAdapter(new ActivityListAdapter());
    }

    private void onClick(final @NonNull String item) {
        final Intent intent = new Intent();
        intent.putExtra(VALUE_NAME, item);
        setResult(RESULT_OK, intent);
        finish();
    }


    private class ActivityListAdapter extends RecyclerView.Adapter<BaseViewHolder> {
        private final int drawablePadding;
        private final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        private ActivityListAdapter() {
            final Resources resources = getResources();
            this.drawablePadding = resources.getDimensionPixelSize(R.dimen.gap_large);

        }

        @Override
        public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final TextView textView = new TextView(ListActivity.this, null, R.style.AppTheme_Text_Body);
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_off, 0, 0, 0);
            textView.setCompoundDrawablePadding(drawablePadding);
            textView.setBackgroundResource(R.drawable.selectable_dark_bounded);
            textView.setLayoutParams(layoutParams);
            return new BaseViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(final BaseViewHolder holder, final int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

    }

    private class BaseViewHolder extends RecyclerView.ViewHolder {
        final TextView title;

        BaseViewHolder(final @NonNull TextView view) {
            super(view);

            this.title = view;
            title.setPadding(itemHorizontalPadding, itemVerticalPadding,
                             itemHorizontalPadding, itemVerticalPadding);
        }

        void bind(final int position) {
            final String item = list.get(position);
            title.setText(item);
            if (item.equals(selectedName)) {
                selectedTextView = title;
                title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_on, 0, 0, 0);
            }
            title.setOnClickListener((view) -> {
                if (selectedTextView != null) {
                    selectedTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_off, 0, 0, 0);
                }
                title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_on, 0, 0, 0);
                onClick(item);
            });
        }
    }
}
