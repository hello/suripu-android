package is.hello.sense.ui.dialogs;

import android.animation.LayoutTransition;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.UnreadStatePresenter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SafeOnClickListener;

import static android.widget.LinearLayout.LayoutParams;

public class QuestionsDialogFragment extends InjectionDialogFragment
        implements CompoundButton.OnCheckedChangeListener {
    public static final String TAG = QuestionsDialogFragment.class.getSimpleName();

    private static final long DELAY_INCREMENT = 20;
    private static final long THANK_YOU_DURATION_MS = 2 * 1000;

    @Inject QuestionsPresenter questionsPresenter;
    @Inject UnreadStatePresenter unreadStatePresenter;

    private final AnimatorContext animatorContext = new AnimatorContext(TAG);

    private ViewGroup rootContainer;
    private TextView titleText;
    private ViewGroup choicesContainer;
    private Button skipButton;
    private Button nextButton;

    private boolean hasClearedAllViews = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.hasClearedAllViews = savedInstanceState.getBoolean("hasClearedAllViews", false);
        } else {
            Analytics.trackEvent(Analytics.TopView.EVENT_QUESTION, null);
            questionsPresenter.userEnteredFlow();
        }

        addPresenter(questionsPresenter);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_questions);

        this.rootContainer = (ViewGroup) dialog.findViewById(R.id.fragment_questions_container);
        this.titleText = (TextView) dialog.findViewById(R.id.fragment_questions_title);
        this.choicesContainer = (ViewGroup) dialog.findViewById(R.id.fragment_questions_choices);

        LayoutTransition transition = choicesContainer.getLayoutTransition();
        AnimatorTemplate.DEFAULT.apply(transition);
        FastOutSlowInInterpolator entranceInterpolator = new FastOutSlowInInterpolator();
        transition.setInterpolator(LayoutTransition.APPEARING, entranceInterpolator);
        transition.setInterpolator(LayoutTransition.DISAPPEARING, entranceInterpolator);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);

        this.skipButton = (Button) dialog.findViewById(R.id.fragment_questions_skip);
        Views.setSafeOnClickListener(skipButton, this::skipQuestion);

        this.nextButton = (Button) dialog.findViewById(R.id.fragment_questions_next);
        Views.setSafeOnClickListener(nextButton, this::nextQuestion);

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasSubscriptions()) {
            bindAndSubscribe(questionsPresenter.question,
                             this::bindQuestion,
                             this::questionUnavailable);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasClearedAllViews", hasClearedAllViews);
    }


    //region Animations

    public void showNextButton(boolean animate) {
        if (nextButton.getVisibility() == View.VISIBLE) {
            return;
        }

        if (animate) {
            animatorContext.transaction(t -> {
                t.animatorFor(skipButton)
                 .fadeOut(View.INVISIBLE);

                t.animatorFor(nextButton)
                 .fadeIn();
            }, finished -> {
            });
        } else {
            Anime.cancelAll(nextButton, skipButton);

            skipButton.setVisibility(View.INVISIBLE);
            nextButton.setVisibility(View.VISIBLE);
        }
    }

    public void showSkipButton(boolean animate) {
        if (skipButton.getVisibility() == View.VISIBLE) {
            return;
        }

        if (animate) {
            animatorContext.transaction(t -> {
                t.animatorFor(nextButton)
                 .fadeOut(View.INVISIBLE);

                t.animatorFor(skipButton)
                 .fadeIn();
            }, finished -> {
            });
        } else {
            Anime.cancelAll(nextButton, skipButton);

            nextButton.setVisibility(View.INVISIBLE);
            skipButton.setVisibility(View.VISIBLE);
        }
    }

    public void updateCallToAction() {
        if (questionsPresenter.hasSelectedChoices()) {
            showNextButton(true);
        } else {
            showSkipButton(true);
        }
    }

    public void animateOutAllViews(@NonNull Runnable onCompletion) {
        if (hasClearedAllViews) {
            rootContainer.removeAllViews();
            onCompletion.run();
        } else {
            animatorContext.transaction(t -> {
                for (int i = 0, count = rootContainer.getChildCount(); i < count; i++) {
                    t.animatorFor(rootContainer.getChildAt(i))
                     .fadeOut(View.INVISIBLE);
                }
            }, finished -> {
                if (finished) {
                    this.hasClearedAllViews = true;

                    rootContainer.removeAllViews();
                    stateSafeExecutor.execute(onCompletion);
                }
            });
        }
    }

    public void showThankYou(@NonNull Runnable onCompletion) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View thankYou = inflater.inflate(R.layout.item_question_thank_you, rootContainer, false);
        ImageView thankYouImage = (ImageView) thankYou.findViewById(R.id.item_thank_you_image);
        thankYouImage.setScaleX(0f);
        thankYouImage.setScaleY(0f);
        thankYouImage.setAlpha(0f);

        TextView thankYouText = (TextView) thankYou.findViewById(R.id.item_thank_you_text);
        thankYouText.setAlpha(0f);

        rootContainer.addView(thankYou);

        animatorContext.transaction(t -> {
            t.animatorFor(thankYouText)
             .fadeIn();

            t.animatorFor(thankYouImage)
             .scale(1f)
             .fadeIn();
        }, finished -> {
            if (finished) {
                rootContainer.postDelayed(stateSafeExecutor.bind(onCompletion),
                                          THANK_YOU_DURATION_MS);
            }
        });

        unreadStatePresenter.updateInsightsLastViewed();
    }

    public void clearQuestions(boolean animate, @Nullable Runnable onCompletion) {
        showSkipButton(animate);

        nextButton.setEnabled(false);
        skipButton.setEnabled(false);

        if (animate) {
            AnimatorTemplate template = AnimatorTemplate.DEFAULT
                    .withInterpolator(new FastOutLinearInInterpolator());
            animatorContext.transaction(template, AnimatorContext.OPTIONS_DEFAULT, t -> {
                for (int i = 0, count = choicesContainer.getChildCount(); i < count; i++) {
                    t.animatorFor(choicesContainer.getChildAt(i))
                     .withStartDelay(DELAY_INCREMENT * i)
                     .scale(0f);
                }
            }, finished -> {
                if (finished && onCompletion != null) {
                    onCompletion.run();
                }
            });
        } else {
            choicesContainer.removeAllViews();

            if (onCompletion != null) {
                onCompletion.run();
            }
        }
    }

    //endregion


    //region Binding Questions

    public void bindQuestion(@Nullable Question question) {
        if (question == null) {
            animateOutAllViews(() -> showThankYou(this::dismissSafely));
        } else {
            clearQuestions(false, null);

            titleText.setText(question.getText());
            skipButton.setEnabled(true);
            nextButton.setEnabled(true);

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
        LayoutParams dividerLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.divider_size));
        dividerLayoutParams.setMargins(margin, 0, margin, 0);
        return dividerLayoutParams;
    }

    public void bindSingleChoiceQuestion(@NonNull Question question) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        LayoutParams choiceLayoutParams = createChoiceLayoutParams();
        LayoutParams dividerLayoutParams = createDividerLayoutParams();
        View.OnClickListener onClickListener = new SafeOnClickListener(stateSafeExecutor, this::singleChoiceSelected);

        List<Question.Choice> choices = question.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            Question.Choice choice = choices.get(i);

            Button choiceButton = (Button) inflater.inflate(R.layout.item_question_single_choice, choicesContainer, false);
            choiceButton.setText(choice.getText());
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
            choiceButton.setChecked(false);
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
        ErrorDialogFragment.presentError(getActivity(), e);
    }

    //endregion


    //region Button Callbacks

    public void nextQuestion(@NonNull View sender) {
        Analytics.trackEvent(Analytics.TopView.EVENT_ANSWER_QUESTION, null);

        clearQuestions(true, questionsPresenter::answerQuestion);
    }

    public void skipQuestion(@NonNull View sender) {
        Analytics.trackEvent(Analytics.TopView.EVENT_SKIP_QUESTION, null);
        questionsPresenter.skipQuestion(true).subscribe();
    }

    public void singleChoiceSelected(@NonNull View sender) {
        Analytics.trackEvent(Analytics.TopView.EVENT_ANSWER_QUESTION, null);

        Question.Choice choice = (Question.Choice) sender.getTag(R.id.fragment_questions_tag_choice);
        questionsPresenter.addChoice(choice);
        clearQuestions(true, questionsPresenter::answerQuestion);
    }

    @Override
    public void onCheckedChanged(CompoundButton choiceButton, boolean checked) {
        Question.Choice choice = (Question.Choice) choiceButton.getTag(R.id.fragment_questions_tag_choice);
        if (checked) {
            questionsPresenter.addChoice(choice);
        } else {
            questionsPresenter.removeChoice(choice);
        }

        updateCallToAction();
    }

    //endregion
}
