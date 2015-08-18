/*
 * Based on FragmentPagerAdapter from support-v13
 *
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.hello.sense.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

/**
 * A re-implementation of {@link android.support.v13.app.FragmentPagerAdapter} that does not
 * keep fragments in memory off-screen, exposes the currently visible fragment, and exposes
 * the tags used to add fragments to the wrapped fragment manager.
 */
public abstract class FragmentPagerAdapter extends PagerAdapter {
    private final FragmentManager fragmentManager;

    private @Nullable FragmentTransaction currentTransaction;
    private @Nullable Fragment currentFragment;

    //region Contract

    /**
     * Should be overriden by subclasses and made public.
     *
     * @param fragmentManager   The fragment manager to add fragments to.
     */
    protected FragmentPagerAdapter(@NonNull FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    /**
     * Called when the fragment manager needs to
     * populate a certain position with a fragment.
     * <p>
     * Subclasses should return a new instance each time this method is called.
     * @param position  The position a fragment is required for.
     * @return  A new fragment instance. Must not be null.
     */
    public abstract @NonNull Fragment createFragment(int position);

    /**
     * Provides an item id for a given position.
     * <p>
     * The default implementation returns the position.
     * @param position  The position an id is needed for.
     * @return  A stable item id. Used in tag generation.
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Retrieve the currently visible fragment from the adapter.
     * @return  The currently visible fragment if applicable.
     */
    public @Nullable Fragment getCurrentFragment() {
        return currentFragment;
    }

    //endregion


    //region Providing Fragments

    /**
     * Generates a fragment tag from a given container view iid and item id.
     * @param viewContainerId   The id of the container of the fragment.
     * @param itemId            The id of the item.
     * @return A stable tag for displaying the fragment.
     */
    public static String makeFragmentTag(int viewContainerId, long itemId) {
        return "is:hello:sense:pager:" + viewContainerId + ":" + itemId;
    }

    @Override
    public void startUpdate(ViewGroup container) {
        // Do nothing.
    }

    @SuppressLint("CommitTransaction")
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (currentTransaction == null) {
            this.currentTransaction = fragmentManager.beginTransaction();
        }

        String tag = makeFragmentTag(container.getId(), getItemId(position));
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            if (fragment.isDetached()) {
                currentTransaction.attach(fragment);
            }
        } else {
            fragment = createFragment(position);
            currentTransaction.add(container.getId(), fragment, tag);
        }

        if (fragment != currentFragment) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }

        return fragment;
    }

    @SuppressLint("CommitTransaction")
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (currentTransaction == null) {
            this.currentTransaction = fragmentManager.beginTransaction();
        }

        Fragment fragment = (Fragment) object;
        currentTransaction.remove(fragment);

        if (currentFragment == fragment) {
            this.currentFragment = null;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (currentTransaction != null) {
            currentTransaction.commitAllowingStateLoss();
            this.currentTransaction = null;

            fragmentManager.executePendingTransactions();
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != currentFragment) {
            if (currentFragment != null) {
                currentFragment.setMenuVisibility(false);
                currentFragment.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            this.currentFragment = fragment;
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == ((Fragment) object).getView());
    }

    //endregion
}
