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

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.RecyclerAdapterTesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmartAlarmAdapterTests extends SenseTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private final FakeAlarmEnabledChangedListener alarmEnabledChangedListener = new FakeAlarmEnabledChangedListener();
    private SmartAlarmAdapter adapter;


    //region Lifecycle

    @Before
    public void setUp() {
        this.adapter = new SmartAlarmAdapter(getContext(), alarmEnabledChangedListener);
    }

    @After
    public void tearDown() {
        alarmEnabledChangedListener.reset();
    }

    //endregion


    //region Rendering

    @Test
    public void messageRendering() throws Exception {
        SmartAlarmAdapter.Message message = new SmartAlarmAdapter.Message(R.string.app_name, StringRef.from("Blah blah blah"));
        message.actionRes = android.R.string.ok;

        LambdaVar<Boolean> clickListenerCalled = LambdaVar.of(false);
        message.onClickListener = view -> {
           clickListenerCalled.set(true);
        };

        adapter.bindMessage(message);

        assertEquals(1, adapter.getItemCount());

        SmartAlarmAdapter.MessageViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_MESSAGE, 0);
        assertEquals("Sense", holder.titleText.getText().toString());
        assertEquals("Blah blah blah", holder.messageText.getText().toString());
        assertEquals("OK", holder.actionButton.getText().toString());

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertTrue(clickListenerCalled.get());
    }

    @Test
    public void smartAlarmRendering() throws Exception {
        Alarm alarm1 = new Alarm();
        alarm1.setEnabled(true);
        alarm1.setRepeated(true);
        alarm1.getDaysOfWeek().add(DateTimeConstants.SATURDAY);
        alarm1.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        alarm1.setSmart(true);
        alarm1.setTime(new LocalTime(8, 30));

        Alarm alarm2 = new Alarm();
        alarm2.setEnabled(false);
        alarm2.setRepeated(false);
        alarm2.setRingOnce();
        alarm2.setSmart(true);
        alarm2.setTime(new LocalTime(5, 45));

        adapter.bindAlarms(Lists.newArrayList(alarm1, alarm2));


        SmartAlarmAdapter.AlarmViewHolder holder1 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 0);
        assertTrue(holder1.enabled.isChecked());
        assertEquals("Smart Alarm  ―  Sun, Sat", holder1.repeat.getText().toString());
        assertEquals("8:30", holder1.time.getText().toString());
        assertEquals("AM", holder1.timePeriod.getText().toString());

        SmartAlarmAdapter.AlarmViewHolder holder2 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 1);
        assertFalse(holder2.enabled.isChecked());
        assertEquals("Smart Alarm", holder2.repeat.getText().toString());
        assertEquals("5:45", holder2.time.getText().toString());
        assertEquals("AM", holder2.timePeriod.getText().toString());
    }

    @Test
    public void alarmRendering() throws Exception {
        Alarm alarm1 = new Alarm();
        alarm1.setEnabled(true);
        alarm1.setRepeated(true);
        alarm1.getDaysOfWeek().add(DateTimeConstants.SATURDAY);
        alarm1.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        alarm1.setSmart(false);
        alarm1.setTime(new LocalTime(8, 30));

        Alarm alarm2 = new Alarm();
        alarm2.setEnabled(false);
        alarm2.setRepeated(false);
        alarm2.setRingOnce();
        alarm2.setSmart(false);
        alarm2.setTime(new LocalTime(5, 45));

        adapter.bindAlarms(Lists.newArrayList(alarm1, alarm2));


        SmartAlarmAdapter.AlarmViewHolder holder1 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 0);
        assertTrue(holder1.enabled.isChecked());
        assertEquals("Alarm  ―  Sun, Sat", holder1.repeat.getText().toString());
        assertEquals("8:30", holder1.time.getText().toString());
        assertEquals("AM", holder1.timePeriod.getText().toString());

        SmartAlarmAdapter.AlarmViewHolder holder2 = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 1);
        assertFalse(holder2.enabled.isChecked());
        assertEquals("Alarm", holder2.repeat.getText().toString());
        assertEquals("5:45", holder2.time.getText().toString());
        assertEquals("AM", holder2.timePeriod.getText().toString());
    }

    @Test
    public void enabledListener() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setEnabled(true);
        alarm.setRepeated(true);
        alarm.getDaysOfWeek().add(DateTimeConstants.SATURDAY);
        alarm.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        alarm.setSmart(false);
        alarm.setTime(new LocalTime(8, 30));

        adapter.bindAlarms(Lists.newArrayList(alarm));


        SmartAlarmAdapter.AlarmViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, SmartAlarmAdapter.VIEW_ID_ALARM, 0);

        holder.enabled.performClick();
        assertTrue(alarmEnabledChangedListener.called);
        assertEquals(0, alarmEnabledChangedListener.position);
        assertFalse(alarmEnabledChangedListener.enabled);
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
