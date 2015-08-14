package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collection;

import javax.inject.Inject;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.adapter.QuestionChoiceAdapter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.recycler.ExtendedItemAnimator;
import is.hello.sense.ui.recycler.StaggeredSlideItemAnimator;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class QuestionsDialogFragment extends InjectionDialogFragment
        implements QuestionChoiceAdapter.OnSelectionChangedListener, AnimatorContext.Scene {
    public static final String TAG = QuestionsDialogFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ERROR = 0x30;
    private static final long THANK_YOU_DURATION_MS = 2 * 1000;

    @Inject QuestionsPresenter questionsPresenter;

    private final AnimatorContext animatorContext = new AnimatorContext(TAG);

    private ViewGroup superContainer;
    private TextView titleText;
    private StaggeredSlideItemAnimator choiceAnimator;
    private QuestionChoiceAdapter choiceAdapter;
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
        }

        addPresenter(questionsPresenter);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_questions);

        this.superContainer = (ViewGroup) dialog.findViewById(R.id.fragment_questions_container);
        this.titleText = (TextView) dialog.findViewById(R.id.fragment_questions_title);

        RecyclerView choicesRecycler = (RecyclerView) dialog.findViewById(R.id.fragment_questions_recycler);
        choicesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        choicesRecycler.addItemDecoration(new CenteringDecoration(getResources()));
        choicesRecycler.setHasFixedSize(true);

        this.choiceAnimator = new StaggeredSlideItemAnimator(getAnimatorContext());
        choicesRecycler.setItemAnimator(choiceAnimator);

        this.choiceAdapter = new QuestionChoiceAdapter(getActivity(), this);
        choicesRecycler.setAdapter(choiceAdapter);

        this.skipButton = (Button) dialog.findViewById(R.id.fragment_questions_skip);
        Views.setSafeOnClickListener(skipButton, this::skipQuestion);

        this.nextButton = (Button) dialog.findViewById(R.id.fragment_questions_next);
        Views.setSafeOnClickListener(nextButton, ignored -> nextQuestion());

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasSubscriptions()) {
            bindAndSubscribe(questionsPresenter.currentQuestion,
                             this::bindQuestion,
                             this::questionUnavailable);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasClearedAllViews", hasClearedAllViews);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ERROR) {
            dismissSafely();
        }
    }

    //region Animations

    public void showNextButton(boolean animate) {
        if (nextButton.getVisibility() == View.VISIBLE) {
            return;
        }

        if (animate) {
            animatorFor(skipButton).fadeOut(View.INVISIBLE).start();
            animatorFor(nextButton).fadeIn().start();
        } else {
            skipButton.setVisibility(View.INVISIBLE);
            nextButton.setVisibility(View.VISIBLE);
        }
    }

    public void showSkipButton(boolean animate) {
        if (skipButton.getVisibility() == View.VISIBLE) {
            return;
        }

        if (animate) {
            animatorFor(nextButton).fadeOut(View.INVISIBLE).start();
            animatorFor(skipButton).fadeIn().start();
        } else {
            nextButton.setVisibility(View.INVISIBLE);
            skipButton.setVisibility(View.VISIBLE);
        }
    }

    public void animateOutAllViews(@NonNull Runnable onCompletion) {
        if (hasClearedAllViews) {
            superContainer.removeAllViews();
            onCompletion.run();
        } else {
            getAnimatorContext().transaction(t -> {
                for (int i = 0, count = superContainer.getChildCount(); i < count; i++) {
                    t.animatorFor(superContainer.getChildAt(i))
                     .fadeOut(View.GONE);
                }
            }, finished -> {
                this.hasClearedAllViews = true;

                superContainer.removeAllViews();
                onCompletion.run();
            });
        }
    }

    public void showThankYou(@NonNull Runnable onCompletion) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View thankYou = inflater.inflate(R.layout.item_question_thank_you, superContainer, false);
        ImageView thankYouImage = (ImageView) thankYou.findViewById(R.id.item_thank_you_image);
        thankYouImage.setScaleX(0f);
        thankYouImage.setScaleY(0f);
        thankYouImage.setAlpha(0f);

        TextView thankYouText = (TextView) thankYou.findViewById(R.id.item_thank_you_text);
        thankYouText.setAlpha(0f);

        superContainer.addView(thankYou);

        getAnimatorContext().transaction(t -> {
            t.animatorFor(thankYouText)
             .fadeIn();

            t.animatorFor(thankYouImage)
             .scale(1f)
             .fadeIn();
        }, finished -> {
            if (finished) {
                superContainer.postDelayed(() -> stateSafeExecutor.execute(onCompletion),
                                           THANK_YOU_DURATION_MS);
            }
        });
    }

    public void clearQuestions(@Nullable Runnable onCompletion) {
        showSkipButton(true);

        nextButton.setEnabled(false);
        skipButton.setEnabled(false);

        choiceAnimator.addListener(new ExtendedItemAnimator.Listener() {
            @Override
            public void onItemAnimatorWillStart(@NonNull AnimatorContext.Transaction transaction) {
            }

            @Override
            public void onItemAnimatorDidStop(boolean finished) {
                choiceAnimator.removeListener(this);

                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        });

        choiceAdapter.clear();
    }

    //endregion


    //region Bindings

    public void bindQuestion(@Nullable Question question) {
        if (question != null) {
            titleText.setText(question.getText());
            choiceAdapter.bindQuestion(question);
        } else {
            animateOutAllViews(() -> showThankYou(this::dismissSafely));
        }
    }

    public void questionUnavailable(Throwable e) {
        choiceAdapter.clear();

        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e)
                .withOperation("Binding question")
                .build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    //endregion


    //region Interactions

    public void nextQuestion() {
        Analytics.trackEvent(Analytics.TopView.EVENT_ANSWER_QUESTION, null);

        clearQuestions(() -> {
            questionsPresenter.nextQuestion();

            bindAndSubscribe(questionsPresenter.answerQuestion(choiceAdapter.getQuestion(),
                                                               choiceAdapter.getSelectedChoices()),
                             unused -> Logger.info(getClass().getSimpleName(), "Answered question"),
                             e -> Logger.error(getClass().getSimpleName(), "Could not answer question", e));
        });
    }

    public void skipQuestion(@NonNull View sender) {
        Analytics.trackEvent(Analytics.TopView.EVENT_SKIP_QUESTION, null);
        clearQuestions(questionsPresenter::skipQuestion);
    }

    @Override
    public void onSelectedChoicesChanged(@NonNull Question.Type questionType,
                                         @NonNull Collection<Integer> selectedItems) {
        if (questionType == Question.Type.CHECKBOX) {
            if (selectedItems.isEmpty()) {
                showSkipButton(true);
            } else {
                showNextButton(true);
            }
        } else if (questionType == Question.Type.CHOICE) {
            nextQuestion();
        }
    }

    //endregion


    @NonNull
    @Override
    public AnimatorContext getAnimatorContext() {
        return animatorContext;
    }


    /**
     * Centers the questions in the recycler view. Currently requires that choice items
     * be of a fixed height to function correctly. Unfortunately, not all layout info
     * needed to do the centering dynamically is available to the decoration.
     */
    static class CenteringDecoration extends RecyclerView.ItemDecoration {
        private final int itemHeight;

        CenteringDecoration(@NonNull Resources resources) {
            this.itemHeight = resources.getDimensionPixelSize(R.dimen.item_question_height);
        }

        @Override
        public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
            if (parent.getChildLayoutPosition(child) == 0) {
                int height = parent.getMeasuredHeight();
                int contentHeight = state.getItemCount() * itemHeight;
                if (contentHeight < height) {
                    outRect.top += (height - contentHeight) / 2;
                }
            }
        }
    }
}
