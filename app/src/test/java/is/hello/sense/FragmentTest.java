package is.hello.sense;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.LinearLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.mvp.presenters.PresenterFragment;

import static junit.framework.Assert.assertNotNull;
import static org.robolectric.util.FragmentTestUtil.startFragment;


public abstract class FragmentTest<T extends PresenterFragment>
        extends SenseTestCase {

    protected T fragment;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    private T getInstanceOfT() {
        final ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
        final Class<T> type = (Class<T>) superClass.getActualTypeArguments()[0];
        try {
            return type.newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() throws Exception {
        startFragmentAndSpy(startWithArgs());
    }

    @Test
    public void fragmentIsntNull() {
        assertNotNull(fragment);
    }

    private void startFragmentAndSpy(@Nullable final Bundle args) {
        fragment = getInstanceOfT();

        if (args != null) {
            fragment.setArguments(args);
        }

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        startFragment(fragment, activityCreatingFragment());
        fragment = Mockito.spy(fragment);
    }

    protected final void spyOnPresenterView() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
    }

    /**
     * @return arguments for a fragment.
     */
    @Nullable
    protected Bundle startWithArgs() {
        return null;
    }

    /**
     * Allows fragments to provide a custom activity, useful for when they expect the activity
     * to provide an interface.
     *
     * @return null to use default {@link org.robolectric.util.FragmentTestUtil.org.robolectric.util.FragmentTestUtil.FragmentUtilActivity}
     */
    @NonNull
    protected Class<? extends FragmentTestActivity> activityCreatingFragment() {
        return FragmentTestActivity.class;
    }

    //region lifecycle helpers

    protected final void callOnCreate() {
        fragment.onCreate(null);
    }

    protected final void callOnViewCreated() {
        fragment.onViewCreated(Mockito.mock(View.class), null);
    }

    protected final void callOnResume() {
        fragment.onResume();
    }

    protected final void callOnPause() {
        fragment.onPause();
    }

    protected final void callOnDestroyView() {
        fragment.onDestroyView();
    }

    protected final void callOnDetach() {
        fragment.onDetach();
    }

    //endregion


    /**
     * Uses reflection to update a final field value so it can be spied on.
     *
     * @param objectWithField object containing the field.
     * @param field           field to remove final from.
     * @param newValue        value to change field with.
     * @throws Exception
     */
    protected void changeFinalFieldValue(@NonNull final Object objectWithField,
                                         @NonNull final Field field,
                                         @Nullable final Object newValue) throws Exception {
        field.setAccessible(true);
        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(objectWithField, newValue);
    }

    /**
     * Extendable version of {@link org.robolectric.util.FragmentTestUtil.org.robolectric.util.FragmentTestUtil.FragmentUtilActivity}
     * for providing our own activities.
     */
    // Remember if the extending class is an inner class it must be static
    public static class FragmentTestActivity extends Activity {
        public FragmentTestActivity() {
            // Remember to leave an empty constructor
        }

        @Override
        protected void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final LinearLayout view = new LinearLayout(this, null, 0);
            @IdRes final int id = 1;
            view.setId(id);
            setContentView(view);
        }

    }

}
