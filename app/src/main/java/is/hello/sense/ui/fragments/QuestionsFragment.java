package is.hello.sense.ui.fragments;

import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

import static android.widget.LinearLayout.LayoutParams;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class QuestionsFragment extends InjectionFragment implements CompoundButton.OnCheckedChangeListener {
    public static final String BACK_STACK_NAME = QuestionsFragment.class.getSimpleName();

    private static final long DELAY_INCREMENT = 20;
    private static final long DISMISS_DELAY = 1000;

    @Inject QuestionsPresenter questionsPresenter;

    private ViewGroup superContainer;
    private TextView titleText;
    private ViewGroup choicesContainer;
    private Button skipButton;
    private Button nextButton;

    private final Handler dismissHandler = new Handler(Looper.getMainLooper());
    private final List<Question.Choice> selectedAnswers = new ArrayList<>();

    private boolean hasClearedAllViews = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.hasClearedAllViews = savedInstanceState.getBoolean("hasClearedAllViews", false);
        }

        addPresenter(questionsPresenter);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questions, container, false);

        this.superContainer = (ViewGroup) view.findViewById(R.id.fragment_questions_container);
        this.titleText = (TextView) view.findViewById(R.id.fragment_questions_title);
        this.choicesContainer = (ViewGroup) view.findViewById(R.id.fragment_questions_choices);

        this.skipButton = (Button) view.findViewById(R.id.fragment_questions_skip);
        skipButton.setOnClickListener(this::skipQuestion);

        this.nextButton = (Button) view.findViewById(R.id.fragment_questions_next);
        nextButton.setOnClickListener(this::nextQuestion);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasSubscriptions()) {
            bindAndSubscribe(questionsPresenter.currentQuestion, this::bindQuestion, this::questionUnavailable);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasClearedAllViews", hasClearedAllViews);
    }


    //region Animations

    public void showNextButton(boolean animate) {
        skipButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
    }

    public void showSkipButton(boolean animate) {
        skipButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.GONE);
    }

    public void updateCallToAction(boolean animate) {
        if (selectedAnswers.isEmpty()) {
            showSkipButton(animate);
        } else {
            showNextButton(animate);
        }
    }

    public void animateOutAllViews(@NonNull Runnable onCompletion) {
        if (hasClearedAllViews) {
            superContainer.removeAllViews();
            onCompletion.run();
        } else {
            for (int index = 0, count = superContainer.getChildCount(); index < count; index++) {
                View child = superContainer.getChildAt(index);
                boolean isLast = (index == count - 1);

                PropertyAnimatorProxy animator = animate(child);
                animator.alpha(0f);
                if (isLast) {
                    animator.addOnAnimationCompleted(finished -> {
                        if (finished) {
                            this.hasClearedAllViews = true;

                            superContainer.removeAllViews();
                            onCompletion.run();
                        }
                    });
                }
                animator.start();
            }
        }
    }

    public void displayThankYou() {
        titleText.setText(R.string.title_thank_you);
        superContainer.addView(titleText);

        titleText.setAlpha(0f);
        animate(titleText)
                .alpha(1f)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        dismissHandler.postDelayed(() -> {
                            questionsPresenter.questionsAcknowledged();
                            FragmentManager fragmentManager = getFragmentManager();
                            if (fragmentManager != null) {
                                fragmentManager.popBackStack(BACK_STACK_NAME, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            }
                        }, DISMISS_DELAY);
                    }
                })
                .start();
    }

    public void clearQuestions(boolean animate, @Nullable Runnable onCompletion) {
        showSkipButton(animate);

        if (animate) {
            long delay = 0;
            for (int index = 0, count = choicesContainer.getChildCount(); index < count; index++) {
                View child = choicesContainer.getChildAt(index);
                boolean isLast = (index == count - 1);

                PropertyAnimatorProxy animator = animate(child);
                animator.scale(0.5f);
                animator.alpha(0f);
                if (isLast) {
                    animator.addOnAnimationCompleted(finished -> {
                        if (finished && onCompletion != null) {
                            onCompletion.run();
                        }
                    });
                }
                animator.setStartDelay(delay);
                animator.start();

                delay += DELAY_INCREMENT;
            }
        } else {
            choicesContainer.removeAllViews();

            if (onCompletion != null)
                onCompletion.run();
        }
    }

    //endregion


    //region Binding Questions

    public void bindQuestion(@Nullable Question question) {
        if (question == null) {
            animateOutAllViews(this::displayThankYou);
        } else {
            titleText.setText(question.getText());

            clearQuestions(false, null);

            switch (question.getType()) {
                case CHOICE: {
                    bindSingleChoiceQuestion(question);
                    break;
                }

                case CHECKBOX: {
                    bindMultipleChoiceQuestion(question);
                    break;
                }

                default: {
                    break;
                }
            }
        }
    }

    public LayoutParams createChoiceLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    public LayoutParams createDividerLayoutParams() {
        int margin = getResources().getDimensionPixelSize(R.dimen.gap_outer);
        LayoutParams dividerLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.divider_height));
        dividerLayoutParams.setMargins(margin, 0, margin, 0);
        return dividerLayoutParams;
    }

    public void bindSingleChoiceQuestion(@NonNull Question question) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        LayoutParams choiceLayoutParams = createChoiceLayoutParams();
        LayoutParams dividerLayoutParams = createDividerLayoutParams();
        View.OnClickListener onClickListener = this::singleChoiceSelected;

        List<Question.Choice> choices = question.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            Question.Choice choice = choices.get(i);

            Button choiceButton = (Button) inflater.inflate(R.layout.item_question_single_choice, choicesContainer, false);
            choiceButton.setText(choice.getText());
            choiceButton.setTag(R.id.fragment_questions_tag_question, question);
            choiceButton.setTag(R.id.fragment_questions_tag_choice, choice);
            choiceButton.setOnClickListener(onClickListener);
            choicesContainer.addView(choiceButton, choiceLayoutParams);

            if (i < choices.size() - 1) {
                View divider = new View(getActivity());
                divider.setBackgroundResource(R.color.light_accent);
                choicesContainer.addView(divider, dividerLayoutParams);
            }
        }
    }

    public void bindMultipleChoiceQuestion(@NonNull Question question) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        LayoutParams choiceLayoutParams = createChoiceLayoutParams();
        LayoutParams dividerLayoutParams = createDividerLayoutParams();

        List<Question.Choice> choices = question.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            Question.Choice choice = choices.get(i);

            ToggleButton choiceButton = (ToggleButton) inflater.inflate(R.layout.item_question_checkbox, choicesContainer, false);
            choiceButton.setTextOn(choice.getText());
            choiceButton.setTextOff(choice.getText());
            choiceButton.setTag(R.id.fragment_questions_tag_question, question);
            choiceButton.setTag(R.id.fragment_questions_tag_choice, choice);
            choiceButton.setOnCheckedChangeListener(this);
            choicesContainer.addView(choiceButton, choiceLayoutParams);

            if (i < choices.size() - 1) {
                View divider = new View(getActivity());
                divider.setBackgroundResource(R.color.light_accent);
                choicesContainer.addView(divider, dividerLayoutParams);
            }
        }
    }

    public void questionUnavailable(@NonNull Throwable e) {
        choicesContainer.removeAllViews();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }

    //endregion


    //region Button Callbacks

    public void nextQuestion(@NonNull View sender) {
        clearQuestions(true, () -> {
            Question question = (Question) sender.getTag(R.id.fragment_questions_tag_question);
            bindAndSubscribe(questionsPresenter.answerQuestion(question, selectedAnswers),
                             unused -> {
                                 selectedAnswers.clear();
                                 questionsPresenter.nextQuestion();
                             },
                             error -> ErrorDialogFragment.presentError(getFragmentManager(), error));
        });
    }

    public void skipQuestion(@NonNull View sender) {
        questionsPresenter.skipQuestion();
    }


    public void singleChoiceSelected(@NonNull View sender) {
        clearQuestions(true, () -> {
            Question question = (Question) sender.getTag(R.id.fragment_questions_tag_question);
            Question.Choice choice = (Question.Choice) sender.getTag(R.id.fragment_questions_tag_choice);
            bindAndSubscribe(questionsPresenter.answerQuestion(question, Lists.newArrayList(choice)),
                    unused -> questionsPresenter.nextQuestion(),
                    error -> ErrorDialogFragment.presentError(getFragmentManager(), error));
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        Question.Choice choice = (Question.Choice) compoundButton.getTag(R.id.fragment_questions_tag_choice);
        if (checked) {
            compoundButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.questions_check, 0);
            selectedAnswers.add(choice);
        } else {
            compoundButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.questions_circle, 0, 0, 0);
            selectedAnswers.remove(choice);
        }

        updateCallToAction(true);
    }

    //endregion
}
