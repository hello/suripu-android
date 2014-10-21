package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class QuestionsFragment extends InjectionFragment {
    private static final long DELAY_INCREMENT = 20;
    private static final long DISMISS_DELAY = 1000;

    @Inject QuestionsPresenter questionsPresenter;

    private ViewGroup superContainer;
    private TextView titleText;
    private ViewGroup choicesContainer;

    private final Handler dismissHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            questionsPresenter.questionsAcknowledged();

            Activity activity = getActivity();
            if (activity != null)
                activity.finish();
        }
    };

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
    public void onDestroyView() {
        super.onDestroyView();

        dismissHandler.removeMessages(0);
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
            Question.Choice choice = (Question.Choice) sender.getTag();
            Observable<ApiResponse> result = bind(questionsPresenter.answerQuestion(choice));
            result.subscribe(unused -> questionsPresenter.nextQuestion(),
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
        titleText.setGravity(Gravity.CENTER);
        titleText.setTextSize(getResources().getDimensionPixelOffset(R.dimen.text_size_large));
        titleText.setText(R.string.title_thank_you);
        superContainer.addView(titleText);

        titleText.setAlpha(0f);
        animate(titleText)
                .alpha(1f)
                .setApplyChangesToView(true)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        dismissHandler.sendEmptyMessageDelayed(0, DISMISS_DELAY);
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
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.bottomMargin = (int) getResources().getDimension(R.dimen.gap_small);
            long delay = 0;
            for (Question.Choice choice : question.getChoices()) {
                Button choiceButton = (Button) inflater.inflate(R.layout.sub_fragment_button_choice, choicesContainer, false);
                choiceButton.setText(choice.getText());
                choiceButton.setTag(choice);
                choiceButton.setOnClickListener(onClickListener);
                choicesContainer.addView(choiceButton, layoutParams);

                choiceButton.setScaleX(0.5f);
                choiceButton.setScaleY(0.5f);
                choiceButton.setAlpha(0f);
                animate(choiceButton)
                        .scale(1f)
                        .alpha(1f)
                        .setStartDelay(delay)
                        .setApplyChangesToView(true)
                        .start();

                delay += DELAY_INCREMENT;
            }
        }
    }

    public void questionUnavailable(@NonNull Throwable e) {
        choicesContainer.removeAllViews();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
