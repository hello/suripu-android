package is.hello.sense;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.mvp.presenters.PresenterFragment;

import static junit.framework.Assert.assertNotNull;
import static org.robolectric.util.FragmentTestUtil.startFragment;

@RunWith(SenseTestCase.WorkaroundTestRunner.class)
@Config(constants = BuildConfig.class,
        application = SenseApplication.class,
        sdk = 21)
public abstract class FragmentTest<T extends PresenterFragment> {

    protected T fragment;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    protected Context getContext() {
        return RuntimeEnvironment.application.getApplicationContext();
    }

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

    public void startFragmentAndSpy(@Nullable final Bundle args) {
        fragment = getInstanceOfT();
        if (args != null) {
            fragment.setArguments(args);
        }
        final Class<? extends Activity> activityClass = activityCreatingFragment();
        if (activityClass == null) {
            startFragment(fragment);
        } else {
            startFragment(fragment, activityClass);
        }
        fragment = Mockito.spy(fragment);

    }

    @Nullable
    protected Bundle startWithArgs() {
        return null;
    }

    @Nullable
    protected Class<? extends Activity> activityCreatingFragment() {
        return null;
    }

    @Test
    public void fragmentIsntNull() {
        assertNotNull(fragment);
    }


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

}
