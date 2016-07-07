package is.hello.sense.ui.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Window;

import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.Logger;
import is.hello.sense.util.StateSafeExecutor;

public final class FragmentNavigationDelegate implements FragmentManager.OnBackStackChangedListener {
    public static final String SAVED_STATUS_BAR_COLOR = FragmentNavigationDelegate.class.getName() + "#SAVED_STATUS_BAR_COLOR";

    private final @NonNull SenseActivity activity;
    private final @IdRes int containerId;
    private final @Nullable StateSafeExecutor stateSafeExecutor;

    private final @ColorInt int defaultStatusBarColor;
    private @Nullable ValueAnimator statusBarAnimator;

    //region Lifecycle

    public FragmentNavigationDelegate(@NonNull SenseActivity activity,
                                      @IdRes int containerId,
                                      @Nullable StateSafeExecutor stateSafeExecutor) {
        this.activity = activity;
        this.containerId = containerId;
        this.stateSafeExecutor = stateSafeExecutor;

        this.defaultStatusBarColor = Windows.getStatusBarColor(activity.getWindow());
        if (Windows.isStatusBarColorAvailable()) {
            getFragmentManager().addOnBackStackChangedListener(this);
        }
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(SAVED_STATUS_BAR_COLOR, Windows.getStatusBarColor(activity.getWindow()));
    }

    public void onRestoreInstanceState(@NonNull Bundle inState) {
        final @ColorInt int statusBarColor = inState.getInt(SAVED_STATUS_BAR_COLOR);
        Windows.setStatusBarColor(activity.getWindow(), statusBarColor);
    }

    public void onDestroy() {
        if (Windows.isStatusBarColorAvailable()) {
            getFragmentManager().removeOnBackStackChangedListener(this);
            if (statusBarAnimator != null) {
                statusBarAnimator.cancel();
            }
        }
    }

    //endregion


    //region Default Implementations

    public FragmentManager getFragmentManager() {
        return activity.getFragmentManager();
    }

    public FragmentTransaction createTransaction(@NonNull Fragment fragment,
                                                 @Nullable String title,
                                                 boolean wantsBackStackEntry) {
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        final String tag = fragment.getClass().getSimpleName();
        if (getTopFragment() == null) {
            transaction.add(containerId, fragment, tag);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
            transaction.setCustomAnimations(R.animator.fragment_fade_in,
                                            R.animator.fragment_fade_out,
                                            R.animator.fragment_fade_in,
                                            R.animator.fragment_fade_out);
            transaction.replace(containerId, fragment, tag);
        }

        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(tag);
        }

        return transaction;
    }

    public void pushFragment(@NonNull Fragment fragment,
                             @Nullable String title,
                             boolean wantsBackStackEntry) {
        final FragmentTransaction transaction = createTransaction(fragment, title,
                                                                  wantsBackStackEntry);
        final FragmentManager fragmentManager = getFragmentManager();
        if (stateSafeExecutor != null) {
            stateSafeExecutor.execute(() -> {
                if (!wantsBackStackEntry) {
                    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                transaction.commit();
                fragmentManager.executePendingTransactions();
            });
        } else {
            if (!wantsBackStackEntry) {
                fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            transaction.commit();
            fragmentManager.executePendingTransactions();
        }
    }

    public void pushFragmentAllowingStateLoss(@NonNull Fragment fragment,
                                              @Nullable String title,
                                              boolean wantsBackStackEntry) {
        final FragmentTransaction transaction = createTransaction(fragment, title,
                                                                  wantsBackStackEntry);
        if (!wantsBackStackEntry) {
            // There's no state safe way to pop the back stack to the beginning.
            // Since state loss is fine inside of this method, we just swallow
            // any IllegalStateExceptions thrown.
            try {
                getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } catch (IllegalStateException e) {
                Logger.info(getClass().getSimpleName(),
                            "Popping back stack is currently impossible. State loss happening.",
                            e);
            }
        }
        transaction.commitAllowingStateLoss();
    }

    public void popFragment(@NonNull Fragment fragment,
                            boolean immediate) {
        final String tag = fragment.getClass().getSimpleName();
        if (immediate) {
            getFragmentManager().popBackStackImmediate(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            getFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Nullable
    public Fragment getTopFragment() {
        return getFragmentManager().findFragmentById(containerId);
    }

    //endregion


    //region Status bar tinting

    @Override
    public void onBackStackChanged() {
        if (statusBarAnimator != null) {
            statusBarAnimator.cancel();
        }

        final @ColorInt int targetColor;
        final Fragment topFragment = getTopFragment();
        final StatusBarColorProvider provider;
        if (topFragment instanceof StatusBarColorProvider) {
            provider = (StatusBarColorProvider) topFragment;
            targetColor = provider.getStatusBarColor(activity.getResources());
        } else {
            provider = StatusBarColorProvider.EmptyProvider.newInstance();
            targetColor = defaultStatusBarColor;
        }

        final Window window = activity.getWindow();
        final @ColorInt int currentColor = Windows.getStatusBarColor(window);
        if (currentColor != targetColor) {
            this.statusBarAnimator = AnimatorTemplate.DEFAULT.createColorAnimator(currentColor,
                                                                                  targetColor);
            statusBarAnimator.addUpdateListener(Windows.createStatusBarUpdateListener(window));
            statusBarAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    provider.onStatusBarTransitionBegan(targetColor);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    Windows.setStatusBarColor(window, targetColor);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    provider.onStatusBarTransitionEnded(Windows.getStatusBarColor(window));
                }
            });
            statusBarAnimator.start();
        }
    }

    //endregion
}
