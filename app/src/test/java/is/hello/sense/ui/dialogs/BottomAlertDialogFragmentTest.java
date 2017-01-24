package is.hello.sense.ui.dialogs;


import android.os.Bundle;
import android.support.annotation.NonNull;

import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.util.FragmentTestUtil;

import is.hello.sense.api.model.v2.alerts.DialogViewModel;
import is.hello.sense.graph.SenseTestCase;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BottomAlertDialogFragmentTest extends SenseTestCase{

    @Test
    public void useEmptyDialogViewModelWhenNoArgs() throws Exception {
        final TestDialogFragment testDialogFragment = spy(new TestDialogFragment());
        testDialogFragment.onCreate(null);
        verify(testDialogFragment, Mockito.times(1)).getEmptyDialogViewModelInstance();
    }

    @Test
    public void useEmptyDialogViewModelWhenWrongTypeArg() throws Exception {
        final TestDialogFragment testDialogFragment = spy(new TestDialogFragment());
        final Bundle args = new Bundle();
        args.putSerializable(TestDialogFragment.ARG_ALERT, "wrong type");
        testDialogFragment.setArguments(args);
        testDialogFragment.onCreate(null);
        verify(testDialogFragment, Mockito.times(1)).getEmptyDialogViewModelInstance();
    }

    @Test
    public void useEmptyDialogViewModelWhenWrongTypeSavedInstanceState() throws Exception {
        final TestDialogFragment testDialogFragment = spy(new TestDialogFragment());
        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putSerializable(TestDialogFragment.ARG_ALERT, "wrong type");
        testDialogFragment.onCreate(savedInstanceState);
        verify(testDialogFragment, Mockito.times(1)).getEmptyDialogViewModelInstance();
    }

    @Test
    public void successfulFragmentStart() throws Exception {
        final TestDialogFragment testDialogFragment = spy(TestDialogFragment.class);
        final Bundle args = new Bundle();
        final TestDialogViewModel model = new TestDialogViewModel(TestDialogViewModel.TEST_STRING);
        args.putSerializable(TestDialogFragment.ARG_ALERT, model);
        testDialogFragment.setArguments(args);
        FragmentTestUtil.startFragment(testDialogFragment);
        verify(testDialogFragment, Mockito.never()).getEmptyDialogViewModelInstance();
        assertEquals(testDialogFragment.alert, model);
    }


    static class TestDialogViewModel implements DialogViewModel<String> {

        static final String TEST_STRING = "Increasing code coverage is fun";
        final static String EMPTY_TEST_STRING = "Having no tests is no fun";
        private final String textString;

        TestDialogViewModel(@NonNull final String testString) {
            this.textString = testString;
        }

        @Override
        public String getTitle() {
            return textString;
        }

        @Override
        public String getBody() {
            return textString;
        }

        @Override
        public String getPositiveButtonText() {
            return textString;
        }

        @Override
        public String getNeutralButtonText() {
            return textString;
        }

        @Override
        public String getAnalyticPropertyType() {
            return textString;
        }
    }

    public static class TestDialogFragment extends BottomAlertDialogFragment<TestDialogViewModel> {

        @Override
        TestDialogViewModel getEmptyDialogViewModelInstance() {
            return new TestDialogViewModel(TestDialogViewModel.EMPTY_TEST_STRING);
        }

        @Override
        void onPositiveButtonClicked() {
            //do nothing
        }

        TestDialogViewModel getAlert() {
            return alert;
        }
    }

}