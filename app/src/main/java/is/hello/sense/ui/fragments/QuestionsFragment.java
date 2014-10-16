package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    @Inject QuestionsPresenter questionsPresenter;

    private TextView titleText;
    private ViewGroup questionsContainer;

    private ProgressBar loadingIndicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(questionsPresenter);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questions, container, false);

        this.titleText = (TextView) view.findViewById(R.id.fragment_questions_title);
        this.questionsContainer = (ViewGroup) view.findViewById(R.id.fragment_questions_container);

        Button skipButton = (Button) view.findViewById(R.id.fragment_questions_skip);
        skipButton.setOnClickListener(this::skipQuestion);

        this.loadingIndicator = new ProgressBar(getActivity(), null, android.R.style.Widget_Holo_Light_ProgressBar_Small_Inverse);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasSubscriptions()) {
            bindAndSubscribe(questionsPresenter.currentQuestion, this::bindQuestion, this::presentError);
        }
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

    
    public void clearQuestions(boolean animate, @Nullable Runnable onCompletion) {
        if (animate) {
            long delay = 0;
            for (int index = 0, count = questionsContainer.getChildCount(); index < count; index++) {
                View child = questionsContainer.getChildAt(index);
                boolean isLast = (index == count - 1);

                PropertyAnimatorProxy animator = animate(child);
                animator.scale(0.5f);
                animator.alpha(0f);
                if (isLast) {
                    animator.setOnAnimationCompleted(finished -> {
                        if (finished && onCompletion != null)
                            onCompletion.run();
                    });
                }
                animator.setStartDelay(delay);
                animator.start();

                delay += DELAY_INCREMENT;
            }
        } else {
            questionsContainer.removeAllViews();

            if (onCompletion != null)
                onCompletion.run();
        }
    }

    public void bindQuestion(@Nullable Question question) {
        if (question == null) {
            questionsPresenter.questionsAcknowledged();
            getActivity().finish();
        } else {
            titleText.setText(question.getText());

            clearQuestions(false, null);

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View.OnClickListener onClickListener = this::choiceSelected;
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.bottomMargin = (int) getResources().getDimension(R.dimen.gap_medium);
            long delay = 0;
            for (Question.Choice choice : question.getChoices()) {
                Button choiceButton = (Button) inflater.inflate(R.layout.sub_fragment_button_choice, questionsContainer, false);
                choiceButton.setText(choice.getText());
                choiceButton.setTag(choice);
                choiceButton.setOnClickListener(onClickListener);
                questionsContainer.addView(choiceButton, layoutParams);

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

    public void presentError(@NonNull Throwable e) {
        questionsContainer.removeAllViews();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
