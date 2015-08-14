package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.Question.Choice;
import is.hello.sense.functional.Lists;

public class QuestionChoiceAdapter extends ArrayRecyclerAdapter<Choice, ArrayRecyclerAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final OnSelectionChangedListener onSelectionChangedListener;
    private final Set<Integer> selectedChoices = new HashSet<>();
    private Question question;
    private @NonNull Question.Type type = Question.Type.CHOICE;

    public QuestionChoiceAdapter(@NonNull Context context,
                                 @NonNull OnSelectionChangedListener onSelectionChangedListener) {
        super(new ArrayList<>());

        this.inflater = LayoutInflater.from(context);
        this.onSelectionChangedListener = onSelectionChangedListener;
    }


    //region Binding

    public void bindQuestion(@NonNull Question question) {
        selectedChoices.clear();

        this.question = question;
        this.type = question.getType();

        replaceAll(question.getChoices());
    }

    @Override
    public void clear() {
        selectedChoices.clear();
        super.clear();
    }

    public Question getQuestion() {
        return question;
    }

    public List<Choice> getSelectedChoices() {
        return Lists.map(selectedChoices, this::getItem);
    }

    protected void notifySelectedChoicesChanged() {
        onSelectionChangedListener.onSelectedChoicesChanged(type, selectedChoices);
    }

    //endregion


    //region Providing Data

    @Override
    public int getItemViewType(int position) {
        return type.ordinal();
    }

    @Override
    public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Question.Type type = Question.Type.values()[viewType];
        switch (type) {
            case CHOICE: {
                View view = inflater.inflate(R.layout.item_question_single_choice, parent, false);
                return new ChoiceViewHolder(view);
            }
            case CHECKBOX: {
                View view = inflater.inflate(R.layout.item_question_checkbox, parent, false);
                return new CheckBoxViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException("Unsupported type " + type);
            }
        }
    }

    @Override
    public void onBindViewHolder(ArrayRecyclerAdapter.ViewHolder holder, int position) {
        holder.bind(position);
    }

    class ChoiceViewHolder extends ViewHolder {
        final Button button;

        ChoiceViewHolder(@NonNull View itemView) {
            super(itemView);

            this.button = (Button) itemView.findViewById(R.id.item_question_single_choice);
            button.setOnClickListener(this);
        }

        @Override
        public void bind(int position) {
            Choice choice = getItem(position);
            button.setText(choice.getText());
        }

        @Override
        public void onClick(View ignored) {
            selectedChoices.add(getAdapterPosition());
            notifySelectedChoicesChanged();
        }
    }

    class CheckBoxViewHolder extends ViewHolder implements CompoundButton.OnCheckedChangeListener {
        final ToggleButton button;

        CheckBoxViewHolder(@NonNull View itemView) {
            super(itemView);

            this.button = (ToggleButton) itemView.findViewById(R.id.item_question_checkbox);
            button.setOnCheckedChangeListener(this);
        }

        @Override
        public void bind(int position) {
            Choice choice = getItem(position);
            button.setChecked(selectedChoices.contains(position));
            button.setText(choice.getText());
            button.setTextOn(choice.getText());
            button.setTextOff(choice.getText());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                selectedChoices.add(getAdapterPosition());
            } else {
                selectedChoices.remove(getAdapterPosition());
            }
            notifySelectedChoicesChanged();
        }
    }

    //endregion


    public interface OnSelectionChangedListener {
        void onSelectedChoicesChanged(@NonNull Question.Type type,
                                      @NonNull Collection<Integer> selectedItems);
    }
}
