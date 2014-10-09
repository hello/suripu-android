package is.hello.sense.ui.fragments;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class QuestionsFragment extends InjectionFragment {
    @Inject QuestionsPresenter questionsPresenter;

    private TextView titleText;
    private ViewGroup questionsContainer;

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

        LayoutTransition transition = Animation.Properties.DEFAULT.apply(new LayoutTransition(), false);
        transition.enableTransitionType(LayoutTransition.CHANGING);
        questionsContainer.setLayoutTransition(transition);

        Button skipButton = (Button) view.findViewById(R.id.fragment_questions_skip);
        skipButton.setOnClickListener(this::skipQuestion);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasSubscriptions()) {
            Observable<Question> question = bindFragment(this, questionsPresenter.currentQuestion);
            track(question.subscribe(this::bindQuestion, this::presentError));
        }
    }

    public void skipQuestion(@NonNull View sender) {
        questionsPresenter.skipQuestion();
    }

    public void choiceSelected(@NonNull View sender) {
        Question.Choice choice = (Question.Choice) sender.getTag();

        LoadingDialogFragment.show(getFragmentManager());
        Observable<ApiResponse> result = bindFragment(this, questionsPresenter.answerQuestion(choice));
        result.subscribe(unused -> {
            LoadingDialogFragment.close(getFragmentManager());
            questionsPresenter.nextQuestion();
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }


    public void bindQuestion(@Nullable Question question) {
        if (question == null) {
            questionsPresenter.update();
            getActivity().finish();
        } else {
            titleText.setText(question.getText());

            questionsContainer.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View.OnClickListener onClickListener = this::choiceSelected;
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.bottomMargin = (int) getResources().getDimension(R.dimen.gap_medium);
            for (Question.Choice choice : question.getChoices()) {
                Button choiceButton = (Button) inflater.inflate(R.layout.sub_fragment_button_choice, questionsContainer, false);
                choiceButton.setText(choice.getText());
                choiceButton.setTag(choice);
                choiceButton.setOnClickListener(onClickListener);
                questionsContainer.addView(choiceButton, layoutParams);
            }
        }
    }

    public void presentError(@NonNull Throwable e) {
        questionsContainer.removeAllViews();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
