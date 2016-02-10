package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.widget.FrameLayout;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowSystemClock;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.RecyclerAdapterTesting;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class SmartAlarmAdapterTests extends InjectionTestCase {
    @Inject DateFormatter dateFormatter;

    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private final FakeAlarmEnabledChangedListener alarmEnabledChangedListener = new FakeAlarmEnabledChangedListener();
    private SmartAlarmAdapter adapter;


    //region Lifecycle

    @Before
    public void setUp() {
        this.adapter = new SmartAlarmAdapter(getContext(), alarmEnabledChangedListener, dateFormatter);
    }

    @After
    public void tearDown() {
        alarmEnabledChangedListener.reset();
    }

    //endregion


    //region Rendering

    @Test
    public void messageRendering() throws Exception {
        final SmartAlarmAdapter.Message message = new SmartAlarmAdapter.Message(R.string.app_name, StringRef.from("Blah blah blah"));
        message.actionRes = android.R.string.ok;

        final LambdaVar<Boolean> clickListenerCalled = LambdaVar.of(false);
        message.onClickListener = view -> {
           clickListenerCalled.set(true);
        };

        adapter.bindMessage(message);

        assertThat(adapter.getItemCount(), is(equalTo(1)));

        final SmartAlarmAdapter.MessageViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_MESSAGE, 0);
        assertThat(holder.titleText.getText().toString(), is(equalTo("Sense")));
        assertThat(holder.messageText.getText().toString(), is(equalTo("Blah blah blah")));
        assertThat(holder.actionButton.getText().toString(), is(equalTo("OK")));

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertThat(clickListenerCalled.get(), is(true));
    }

    @Test
    public void smartAlarmRendering() throws Exception {
        final Alarm alarm1 = new Alarm();
        alarm1.setEnabled(true);
        alarm1.setRepeated(true);
        alarm1.addDayOfWeek(DateTimeConstants.SATURDAY);
        alarm1.addDayOfWeek(DateTimeConstants.SUNDAY);
        alarm1.setSmart(true);
        alarm1.setTime(new LocalTime(8, 30));

        final Alarm alarm2 = new Alarm();
        alarm2.setEnabled(false);
        alarm2.setRepeated(false);
        alarm2.setRingOnce();
        alarm2.setSmart(true);
        alarm2.setTime(new LocalTime(5, 45));

        adapter.bindAlarms(Lists.newArrayList(alarm1, alarm2));


        final SmartAlarmAdapter.AlarmViewHolder holder1 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 0);
        assertThat(holder1.enabled.isChecked(), is(true));
        assertThat(holder1.repeat.getText().toString(), is(equalTo("Smart Alarm  ―  Weekends")));
        assertThat(holder1.time.getText().toString(), is(equalTo("8:30 AM")));

        final SmartAlarmAdapter.AlarmViewHolder holder2 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 1);
        assertFalse(holder2.enabled.isChecked());
        assertThat(holder2.repeat.getText().toString(), is(equalTo("Smart Alarm")));
        assertThat(holder2.time.getText().toString(), is(equalTo("5:45 AM")));
    }

    @Test
    public void alarmRendering() throws Exception {
        final Alarm alarm1 = new Alarm();
        alarm1.setEnabled(true);
        alarm1.setRepeated(true);
        alarm1.addDayOfWeek(DateTimeConstants.SATURDAY);
        alarm1.addDayOfWeek(DateTimeConstants.SUNDAY);
        alarm1.setSmart(false);
        alarm1.setTime(new LocalTime(8, 30));

        final Alarm alarm2 = new Alarm();
        alarm2.setEnabled(false);
        alarm2.setRepeated(false);
        alarm2.setRingOnce();
        alarm2.setSmart(false);
        alarm2.setTime(new LocalTime(5, 45));

        adapter.bindAlarms(Lists.newArrayList(alarm1, alarm2));


        final SmartAlarmAdapter.AlarmViewHolder holder1 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 0);
        assertThat(holder1.enabled.isChecked(), is(true));
        assertThat(holder1.repeat.getText().toString(), is(equalTo("Alarm  ―  Weekends")));
        assertThat(holder1.time.getText().toString(), is(equalTo("8:30 AM")));

        final SmartAlarmAdapter.AlarmViewHolder holder2 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 1);
        assertThat(holder2.enabled.isChecked(), is(false));
        assertThat(holder2.repeat.getText().toString(), is(equalTo("Alarm")));
        assertThat(holder2.time.getText().toString(), is(equalTo("5:45 AM")));
    }

    @Test
    public void enabledListener() throws Exception {
        final Alarm alarm = new Alarm();
        alarm.setEnabled(true);
        alarm.setRepeated(true);
        alarm.addDayOfWeek(DateTimeConstants.SATURDAY);
        alarm.addDayOfWeek(DateTimeConstants.SUNDAY);
        alarm.setSmart(false);
        alarm.setTime(new LocalTime(8, 30));

        adapter.bindAlarms(Lists.newArrayList(alarm));


        final SmartAlarmAdapter.AlarmViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 0);

        holder.enabled.performClick();
        assertThat(alarmEnabledChangedListener.called, is(true));
        assertThat(alarmEnabledChangedListener.position, is(equalTo(0)));
        assertThat(alarmEnabledChangedListener.enabled, is(false));
    }

    //endregion


    static class FakeAlarmEnabledChangedListener implements SmartAlarmAdapter.InteractionListener {
        boolean called = false;
        int position = RecyclerView.NO_POSITION;
        Boolean enabled = null;


        @Override
        public void onAlarmClicked(int position, @NonNull Alarm alarm) {

        }

        @Override
        public boolean onAlarmLongClicked(int position, @NonNull Alarm alarm) {
            return false;
        }

        @Override
        public void onAlarmEnabledChanged(int position, boolean enabled) {
            this.called = true;
            this.position = position;
            this.enabled = enabled;
        }


        void reset() {
            this.called = false;
            this.position = RecyclerView.NO_POSITION;
            this.enabled = null;
        }
    }
}
