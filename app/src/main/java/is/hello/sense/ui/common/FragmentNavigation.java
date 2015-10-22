package is.hello.sense.ui.common;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.util.StateSafeExecutor;

public interface FragmentNavigation {
    void pushFragment(@NonNull Fragment fragment,
                      @Nullable String title,
                      boolean wantsBackStackEntry);

    void pushFragmentAllowingStateLoss(@NonNull Fragment fragment,
                                       @Nullable String title,
                                       boolean wantsBackStackEntry);

    void popFragment(@NonNull Fragment fragment,
                     boolean immediate);

    void flowFinished(@NonNull Fragment fragment,
                      int responseCode,
                      @Nullable Intent result);

    @Nullable Fragment getTopFragment();

    interface BackInterceptingFragment {
        boolean onInterceptBack(@NonNull Runnable back);
    }

    final class Delegate {
        private final @NonNull FragmentManager fragmentManager;
        private final @IdRes int containerId;
        private final @Nullable StateSafeExecutor stateSafeExecutor;

        public Delegate(@NonNull SenseActivity activity,
                        @IdRes int containerId,
                        @Nullable StateSafeExecutor stateSafeExecutor) {
            this.fragmentManager = activity.getFragmentManager();
            this.containerId = containerId;
            this.stateSafeExecutor = stateSafeExecutor;
        }

        protected FragmentTransaction createTransaction(@NonNull Fragment fragment,
                                                        @Nullable String title,
                                                        boolean wantsBackStackEntry) {
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            final String tag = fragment.getClass().getSimpleName();
            if (getTopFragment() == null) {
                transaction.add(containerId, fragment, tag);
            } else {
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
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
            if (stateSafeExecutor != null) {
                stateSafeExecutor.execute(transaction::commit);
            } else {
                transaction.commit();
            }
        }

        public void pushFragmentAllowingStateLoss(@NonNull Fragment fragment,
                                                  @Nullable String title,
                                                  boolean wantsBackStackEntry) {
            final FragmentTransaction transaction = createTransaction(fragment, title,
                                                                      wantsBackStackEntry);
            transaction.commitAllowingStateLoss();
        }

        public void popFragment(@NonNull Fragment fragment,
                                boolean immediate) {
            final String tag = fragment.getClass().getSimpleName();
            if (immediate) {
                fragmentManager.popBackStackImmediate(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                fragmentManager.popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }

        @Nullable
        public Fragment getTopFragment() {
            return fragmentManager.findFragmentById(containerId);
        }
    }
}
