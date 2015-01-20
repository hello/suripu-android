package is.hello.sense.ui.fragments.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Markdown;
import rx.Scheduler;

import static android.widget.LinearLayout.LayoutParams;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class OnboardingRoomCheckFragment extends InjectionFragment {
    private static final long CONDITION_VISIBLE_MS = 2000;
    private static final long END_CONTAINER_DELAY_MS = 50;

    @Inject RoomConditionsPresenter roomConditionsPresenter;
    @Inject Markdown markdown;

    private final Scheduler.Worker deferWorker = observeScheduler.createWorker();

    private final int[] CONDITION_TITLES = {
            R.string.room_condition_checking_temperature,
            R.string.room_condition_checking_humidity,
            R.string.room_condition_checking_particulates,
            R.string.room_condition_checking_sound,
            R.string.room_condition_checking_light,
    };
    private final int[] ACTIVE_STATE_IMAGES = {
            R.drawable.room_check_temperature_blue,
            R.drawable.room_check_humidity_blue,
            R.drawable.room_check_particulates_blue,
            R.drawable.room_check_sound_blue,
            R.drawable.room_check_light_blue,
    };
    private final List<SensorState> conditions = new ArrayList<>();
    private final List<UnitFormatter.Formatter> conditionFormatters = new ArrayList<>();

    private boolean animationCompleted = false;
    private @Nullable ValueAnimator currentValueAnimator = null;

    private LinearLayout conditionsContainer;

    private LinearLayout itemContainer;
    private TextView itemTitle;
    private TextView itemMessage;
    private TextView itemValue;

    private View topDivider;
    private View bottomDivider;

    private LinearLayout endContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.animationCompleted = savedInstanceState.getBoolean("animationCompleted", false);
        }

        roomConditionsPresenter.update();
        addPresenter(roomConditionsPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_room_check, container, false);

        this.conditionsContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_room_check_container);
        Animations.Properties.DEFAULT.apply(conditionsContainer.getLayoutTransition(), false);

        this.itemContainer = (LinearLayout) inflater.inflate(R.layout.item_room_check_condition, container, false);
        this.itemTitle = (TextView) itemContainer.findViewById(R.id.item_room_check_condition_title);
        this.itemMessage = (TextView) itemContainer.findViewById(R.id.item_room_check_condition_message);
        this.itemValue = (TextView) itemContainer.findViewById(R.id.item_room_check_condition_value);

        this.topDivider = createDivider();
        this.bottomDivider = createDivider();

        this.endContainer = (LinearLayout) inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, container, false);
        Button continueButton = (Button) endContainer.findViewById(R.id.sub_fragment_room_check_end_continue);
        Views.setSafeOnClickListener(continueButton, this::continueOnboarding);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (animationCompleted) {
            jumpToEnd();
        } else {
            bindAndSubscribe(roomConditionsPresenter.currentConditions.take(1), this::bindConditions, this::conditionsUnavailable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        jumpToEnd();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (this.currentValueAnimator != null) {
            currentValueAnimator.cancel();
        }

        deferWorker.unsubscribe();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("animationCompleted", animationCompleted);
    }


    private View createDivider() {
        View divider = new View(getActivity());
        divider.setBackgroundResource(R.color.border);

        Resources resources = getResources();
        int dividerMargin = resources.getDimensionPixelSize(R.dimen.gap_medium);
        int dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, dividerHeight);
        layoutParams.setMargins(0, dividerMargin, 0, dividerMargin);
        divider.setLayoutParams(layoutParams);

        return divider;
    }


    //region Displaying Conditions

    public void showConditionsAt(int position, @NonNull Runnable onBefore, @NonNull Runnable onFinish) {
        if (itemContainer.getParent() != null) {
            SimpleTransitionListener listener = new SimpleTransitionListener(LayoutTransition.DISAPPEARING, itemContainer) {
                @Override
                protected void onEnd() {
                    showConditionsAt(position, onBefore, onFinish);
                }
            };
            withTransitionListener(listener).removeView(itemContainer);

            if (topDivider.getParent() != null) {
                conditionsContainer.removeView(topDivider);
            }

            if (bottomDivider.getParent() != null) {
                conditionsContainer.removeView(bottomDivider);
            }
        } else {
            onBefore.run();

            SimpleTransitionListener listener = new SimpleTransitionListener(LayoutTransition.APPEARING, itemContainer) {
                @Override
                protected void onEnd() {
                    onFinish.run();
                }
            };
            withTransitionListener(listener).addView(itemContainer, position);

            int bottomDividerPosition = position + 1;
            if (position > 1) {
                conditionsContainer.addView(topDivider, position - 1);
                bottomDividerPosition++;
            }

            if (position < conditions.size()) {
                conditionsContainer.addView(bottomDivider, bottomDividerPosition);
            }
        }
    }

    public void showConditionAt(int position) {
        if (endContainer.getParent() != null || isDetached()) {
            return;
        }

        if (position < conditions.size()) {
            conditionsContainer.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);

            SensorState condition = conditions.get(position);
            UnitFormatter.Formatter formatter = conditionFormatters.get(position);

            showConditionsAt(position + 1, () -> {
                int sensorConditionColor = getResources().getColor(condition.getCondition().colorRes);

                itemTitle.setText(CONDITION_TITLES[position]);

                String message = condition.getMessage();
                if (TextUtils.isEmpty(message)) {
                    itemMessage.setText(R.string.missing_data_placeholder);
                } else {
                    itemMessage.setText(message);
                    bindAndSubscribe(markdown.renderWithEmphasisColor(sensorConditionColor, message),
                                     itemMessage::setText,
                                     Functions.LOG_ERROR);
                }

                itemValue.setTextColor(sensorConditionColor);
                itemValue.setText("0");

                ImageView conditionImage = (ImageView) conditionsContainer.getChildAt(position);
                conditionImage.setImageResource(ACTIVE_STATE_IMAGES[position]);
            }, () -> {
                if (condition.getValue() == null || condition.getValue() == 0f) {
                    deferWorker.schedule(() -> showConditionAt(position + 1), CONDITION_VISIBLE_MS, TimeUnit.MILLISECONDS);
                } else {
                    this.currentValueAnimator = Animations.Properties.DEFAULT.apply(ValueAnimator.ofFloat(0f, condition.getValue()));
                    currentValueAnimator.addUpdateListener(a -> {
                        float value = (Float) a.getAnimatedValue();
                        if (formatter != null) {
                            itemValue.setText(formatter.format(value));
                        } else {
                            itemContainer.setTag(value + condition.getValue());
                        }
                    });
                    currentValueAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            deferWorker.schedule(() -> showConditionAt(position + 1), CONDITION_VISIBLE_MS, TimeUnit.MILLISECONDS);
                            OnboardingRoomCheckFragment.this.currentValueAnimator = null;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            OnboardingRoomCheckFragment.this.currentValueAnimator = null;
                        }
                    });
                    currentValueAnimator.start();
                }
            });
        } else {
            deferWorker.schedule(() -> showComplete(true));
        }
    }

    public void jumpToEnd() {
        LayoutTransition layoutTransition = conditionsContainer.getLayoutTransition();
        layoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        layoutTransition.disableTransitionType(LayoutTransition.APPEARING);
        layoutTransition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        layoutTransition.disableTransitionType(LayoutTransition.DISAPPEARING);

        if (this.currentValueAnimator != null) {
            currentValueAnimator.cancel();
        }

        if (itemContainer.getParent() != null) {
            conditionsContainer.removeView(itemContainer);
        }

        if (endContainer.getParent() == null) {
            for (int i = 0, size = conditionsContainer.getChildCount(); i < size; i++) {
                ImageView image = (ImageView) conditionsContainer.getChildAt(i);
                image.setImageResource(ACTIVE_STATE_IMAGES[i]);
            }
            showComplete(true);
        }
    }

    public void showComplete(boolean animate) {
        this.animationCompleted = true;

        conditionsContainer.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        conditionsContainer.removeView(itemContainer);
        conditionsContainer.removeView(topDivider);
        conditionsContainer.removeView(bottomDivider);

        if (animate) {
            SimpleTransitionListener listener = new SimpleTransitionListener(LayoutTransition.APPEARING, endContainer) {
                @Override
                protected void onStart() {
                    for (View child : Views.children(endContainer)) {
                        child.setAlpha(0f);
                    }
                }

                @Override
                protected void onEnd() {
                    float slideAmount = getResources().getDimensionPixelSize(R.dimen.gap_outer);
                    long delay = (END_CONTAINER_DELAY_MS * endContainer.getChildCount());
                    for (View child : Views.children(endContainer)) {
                        animate(child)
                                .setStartDelay(delay)
                                .slideYAndFade(slideAmount, 0f, 0f, 1f)
                                .start();

                        delay -= END_CONTAINER_DELAY_MS;
                    }
                }
            };
            withTransitionListener(listener).addView(endContainer);
        } else {
            conditionsContainer.addView(endContainer);
        }
    }

    //endregion


    public void bindConditions(@NonNull RoomConditionsPresenter.Result result) {
        if (endContainer.getParent() != null) {
            return;
        }

        UnitSystem unitSystem = result.units;
        RoomConditions roomConditions = result.conditions;

        this.conditions.add(roomConditions.getTemperature());
        this.conditionFormatters.add(unitSystem::formatTemperature);

        this.conditions.add(roomConditions.getHumidity());
        this.conditionFormatters.add(null);

        this.conditions.add(roomConditions.getParticulates());
        this.conditionFormatters.add(unitSystem::formatParticulates);

        this.conditions.add(new SensorState(70, "[placeholder] It's a bit loud.", Condition.ALERT, "db", DateTime.now()));
        this.conditionFormatters.add(unitSystem::formatDecibels);

        this.conditions.add(roomConditions.getLight());
        this.conditionFormatters.add(unitSystem::formatLight);

        showConditionAt(0);
    }

    public void conditionsUnavailable(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
        showComplete(true);
    }


    public void continueOnboarding(@NonNull View sender) {
        ((OnboardingActivity) getActivity()).showSmartAlarmInfo();
    }


    //region Transitions

    private @NonNull LinearLayout withTransitionListener(@NonNull SimpleTransitionListener listener) {
        conditionsContainer.getLayoutTransition().addTransitionListener(listener);
        return conditionsContainer;
    }

    private static abstract class SimpleTransitionListener implements LayoutTransition.TransitionListener {
        private final int targetTransition;
        private final View targetView;

        private SimpleTransitionListener(int targetTransition, View targetView) {
            this.targetTransition = targetTransition;
            this.targetView = targetView;
        }


        protected void onStart() {

        }

        protected void onEnd() {

        }


        @Override
        public final void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int transitionType) {
            if (view == targetView && transitionType == targetTransition) {
                onStart();
            }
        }

        @Override
        public final void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int transitionType) {
            if (view == targetView && transitionType == targetTransition) {
                onEnd();
                layoutTransition.removeTransitionListener(this);
            }
        }
    }

    //endregion
}
