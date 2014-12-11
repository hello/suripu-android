package is.hello.sense.ui.fragments.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.Views;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Markdown;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class OnboardingRoomCheckFragment extends InjectionFragment {
    private static final int CONDITION_VISIBLE_DURATION = 2000;
    private static final long END_CONTAINER_VIEW_DELAY = 50;

    @Inject CurrentConditionsPresenter currentConditionsPresenter;
    @Inject Markdown markdown;

    private final Handler deferrer = new Handler(Looper.getMainLooper());
    private final int[] conditionTitles = {
            R.string.room_condition_checking_temperature,
            R.string.room_condition_checking_humidity,
            R.string.room_condition_checking_particulates,
            R.string.room_condition_checking_sound,
            R.string.room_condition_checking_light,
    };
    private final int[] endStateImages = {
            R.drawable.room_check_temperature_blue,
            R.drawable.room_check_humidity_blue,
            R.drawable.room_check_particulates_blue,
            R.drawable.room_check_sound_blue,
            R.drawable.room_check_light_blue,
    };
    private final List<SensorState> conditions = new ArrayList<>();
    private final List<UnitFormatter.Formatter> unitFormatters = new ArrayList<>();

    private LinearLayout conditionsContainer;

    private LinearLayout conditionItemContainer;
    private TextView conditionItemTitle;
    private TextView conditionItemMessage;
    private TextView conditionItemValue;

    private LinearLayout endContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentConditionsPresenter.update();
        addPresenter(currentConditionsPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_room_check, container, false);

        this.conditionsContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_room_check_container);
        Animation.Properties.DEFAULT.apply(conditionsContainer.getLayoutTransition(), false);

        this.conditionItemContainer = (LinearLayout) inflater.inflate(R.layout.item_room_check_condition, container, false);
        this.conditionItemTitle = (TextView) conditionItemContainer.findViewById(R.id.item_room_check_condition_title);
        this.conditionItemMessage = (TextView) conditionItemContainer.findViewById(R.id.item_room_check_condition_message);
        this.conditionItemValue = (TextView) conditionItemContainer.findViewById(R.id.item_room_check_condition_value);

        this.endContainer = (LinearLayout) inflater.inflate(R.layout.sub_fragment_room_check_end, container, false);
        Button continueButton = (Button) endContainer.findViewById(R.id.sub_fragment_room_check_end_continue);
        continueButton.setOnClickListener(this::continueOnboarding);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(currentConditionsPresenter.currentConditions, this::bindConditions, this::conditionsUnavailable);
    }


    //region Displaying Conditions

    public void showConditionsAt(int position, @NonNull Runnable onBefore, @NonNull Runnable onFinish) {
        if (conditionItemContainer.getParent() != null) {
            SimpleTransitionListener listener = new SimpleTransitionListener(LayoutTransition.DISAPPEARING, conditionItemContainer) {
                @Override
                protected void onEnd() {
                    showConditionsAt(position, onBefore, onFinish);
                }
            };
            withTransitionListener(listener).removeView(conditionItemContainer);
        } else {
            onBefore.run();

            SimpleTransitionListener listener = new SimpleTransitionListener(LayoutTransition.APPEARING, conditionItemContainer) {
                @Override
                protected void onEnd() {
                    onFinish.run();
                }
            };
            withTransitionListener(listener).addView(conditionItemContainer, position);
        }
    }

    public void showConditionAt(int position) {
        if (position < conditions.size()) {
            conditionsContainer.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);

            SensorState condition = conditions.get(position);
            UnitFormatter.Formatter formatter = unitFormatters.get(position);

            showConditionsAt(position + 1, () -> {
                int sensorConditionColor = getResources().getColor(condition.getCondition().colorRes);

                conditionItemTitle.setText(conditionTitles[position]);
                bindAndSubscribe(markdown.renderWithEmphasisColor(sensorConditionColor, condition.getMessage()),
                                 conditionItemMessage::setText,
                                 ignored -> conditionItemMessage.setText(condition.getMessage()));

                conditionItemValue.setTextColor(sensorConditionColor);
                conditionItemValue.setText("0");

                ImageView conditionImage = (ImageView) conditionsContainer.getChildAt(position);
                conditionImage.setImageResource(endStateImages[position]);
            }, () -> {
                if (condition.getValue() == 0f) {
                    deferrer.postDelayed(() -> showConditionAt(position + 1), CONDITION_VISIBLE_DURATION);
                } else {
                    ValueAnimator animator = Animation.Properties.DEFAULT.apply(ValueAnimator.ofFloat(0f, condition.getValue()));
                    animator.addUpdateListener(a -> {
                        float value = (Float) a.getAnimatedValue();
                        if (formatter != null) {
                            conditionItemValue.setText(formatter.format(value));
                        } else {
                            conditionItemContainer.setTag(value + condition.getValue());
                        }
                    });
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            deferrer.postDelayed(() -> showConditionAt(position + 1), CONDITION_VISIBLE_DURATION);
                        }
                    });
                    animator.start();
                }
            });
        } else {
            showComplete();
        }
    }

    public void showComplete() {
        conditionsContainer.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        conditionsContainer.removeView(conditionItemContainer);

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
                long delay = (END_CONTAINER_VIEW_DELAY * endContainer.getChildCount());
                for (View child : Views.children(endContainer)) {
                    animate(child)
                            .setStartDelay(delay)
                            .slideY(slideAmount, 0f)
                            .start();

                    delay -= END_CONTAINER_VIEW_DELAY;
                }
            }
        };
        withTransitionListener(listener).addView(endContainer);
    }

    //endregion


    public void bindConditions(@NonNull CurrentConditionsPresenter.Result result) {
        UnitSystem unitSystem = result.units;
        RoomConditions roomConditions = result.conditions;

        this.conditions.add(roomConditions.getTemperature());
        this.unitFormatters.add(unitSystem::formatTemperature);

        this.conditions.add(roomConditions.getHumidity());
        this.unitFormatters.add(null);

        this.conditions.add(roomConditions.getParticulates());
        this.unitFormatters.add(unitSystem::formatParticulates);

        this.conditions.add(new SensorState(70, "[placeholder] It's a bit loud.", Condition.ALERT, "db", DateTime.now()));
        this.unitFormatters.add(unitSystem::formatDecibels);

        this.conditions.add(new SensorState(200, "[placeholder] It’s really bright in your room right now, but that’s ok. It’s only midday!", Condition.WARNING, "lux", DateTime.now()));
        this.unitFormatters.add(unitSystem::formatDecibels);

        showConditionAt(0);
    }

    public void conditionsUnavailable(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
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
