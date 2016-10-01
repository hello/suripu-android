package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.mvp.view.onboarding.RoomCheckView;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.RoomCheckPresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;

public class OnboardingRoomCheckFragment extends BasePresenterFragment
implements RoomCheckPresenter.Output{

    @Inject
    RoomCheckPresenter presenter;

    private RoomCheckView presenterView;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        this.presenterView = new RoomCheckView(getActivity(), getAnimatorContext());
        return presenterView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(presenterView != null) {
            presenterView.stopAnimations();
            presenterView.destroyView();
            presenterView = null;
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        presenter = null;
    }

    //region RoomCheck Output

    @Override
    public void initialize(){
        presenterView.initSensorContainerXOffset();
    }

    @Override
    public void createSensorConditionViews(final @DrawableRes int[] icons) {
        presenterView.removeSensorContainerViews();

        final LinearLayout.LayoutParams layoutParams
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int padding = getResources().getDimensionPixelOffset(R.dimen.item_room_sensor_condition_view_width) / 2;
        for(@DrawableRes final int sensorIconRes : icons) {
            presenterView.createSensorConditionView(sensorIconRes,
                                                    padding,
                                                    layoutParams);
        }
    }

    @Override
    public void unavailableConditions(final Throwable e) {
        presenterView.removeSensorContainerViews();
    }

    @Override
    public void showConditionAt(final int position,
                                @NonNull final String statusMessage,
                                @DrawableRes final int conditionDrawable,
                                @StringRes final int checkStatusMessage,
                                @DrawableRes final int conditionIcon,
                                @ColorRes final int conditionColorRes,
                                final int value,
                                @NonNull final String unitSuffix,
                                @NonNull  final Runnable onComplete) {
        presenterView.animateSenseToGray();
        presenterView.showConditionAt(position,
                                      checkStatusMessage,
                                      statusMessage,
                                      conditionIcon,
                                      ResourcesCompat.getDrawable(getResources(), conditionDrawable, null),
                                      conditionColorRes,
                                      value,
                                      unitSuffix,
                                      onComplete);
    }

    @Override
    public void showCompletion(final boolean animate, @NonNull final Runnable onContinue) {
        presenterView.stopAnimations();
        presenterView.setSenseCondition(animate, presenterView.getDefaultSenseCondition());
        presenterView.showCompletion(animate,
                                     ignored -> onContinue.run());
    }

    //endregion
}
