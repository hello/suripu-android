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
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

import static android.widget.LinearLayout.LayoutParams;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class QuestionsFragment extends InjectionFragment {
    public static final String BACK_STACK_NAME = QuestionsFragment.class.getSimpleName();

    private static final int TAG_QUESTION = 1;
    private static final int TAG_CHOICE = 2;

    private static final long DELAY_INCREMENT = 20;
    private static final long DISMISS_DELAY = 1000;

    @Inject QuestionsPresenter questionsPresenter;

    private ViewGroup superContainer;
    private TextView titleText;
    private ViewGroup choicesContainer;

    private final Handler dismissHandler = new Handler(Looper.getMainLooper());

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

        Button skipButton = (Button) view.findViewById(R.id.fragment_questions_skip);
        skipButton.setOnClickListener(this::skipQuestion);

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

    public void skipQuestion(@NonNull View sender) {
        questionsPresenter.skipQuestion();
    }

    public void choiceSelected(@NonNull View sender) {
        clearQuestions(true, () -> {
            Question question = (Question) sender.getTag(TAG_QUESTION);
            Question.Choice choice = (Question.Choice) sender.getTag(TAG_CHOICE);
            bindAndSubscribe(questionsPresenter.answerQuestion(question, choice),
                    unused -> questionsPresenter.nextQuestion(),
                    error -> ErrorDialogFragment.presentError(getFragmentManager(), error));
        });
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
                animator.setApplyChangesToView(true);
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
                .setApplyChangesToView(true)
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

    public void bindQuestion(@Nullable Question question) {
        if (question == null) {
            animateOutAllViews(this::displayThankYou);
        } else {
            titleText.setText(question.getText());

            clearQuestions(false, null);

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View.OnClickListener onClickListener = this::choiceSelected;
            LayoutParams choiceLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            LayoutParams dividerLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.divider_height));
            dividerLayoutParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.gap_outer);
            dividerLayoutParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.gap_outer);

            List<Question.Choice> choices = question.getChoices();
            for (int i = 0; i < choices.size(); i++) {
                Question.Choice choice = choices.get(i);
                Button choiceButton = (Button) inflater.inflate(R.layout.item_question_choice, choicesContainer, false);
                choiceButton.setText(choice.getText());
                choiceButton.setTag(TAG_QUESTION, question);
                choiceButton.setTag(TAG_CHOICE, choice);
                choiceButton.setOnClickListener(onClickListener);
                choicesContainer.addView(choiceButton, choiceLayoutParams);

                if (i < choices.size() - 1) {
                    View divider = new View(getActivity());
                    divider.setBackgroundResource(R.color.light_accent);
                    choicesContainer.addView(divider, dividerLayoutParams);
                }
            }
        }
    }

    public void questionUnavailable(@NonNull Throwable e) {
        choicesContainer.removeAllViews();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
