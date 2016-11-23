package is.hello.sense.api.model.v2.alarms;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.model.Alarm;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


public class AlarmGroupsTest {
    @Test
    public void getTotalSize() throws Exception {
        final AlarmGroups alarmGroups = new AlarmGroups();
        assertEquals(0, alarmGroups.getTotalSize());
        alarmGroups.getClassic().add(new Alarm());
        alarmGroups.getExpansions().add(new Alarm());
        alarmGroups.getVoice().add(new Alarm());
        assertEquals(3, alarmGroups.getTotalSize());
    }

    @Test
    public void getAll() throws Exception {
        final AlarmGroups alarmGroups = new AlarmGroups();
        final Alarm classicAlarm = new Alarm();
        final Alarm expansionAlarm = Alarm.generateExpansionAlarmTestCase();
        final Alarm voiceAlarm = Alarm.generateVoiceAlarmTestCase();
        alarmGroups.getClassic().add(classicAlarm);
        alarmGroups.getExpansions().add(expansionAlarm);
        alarmGroups.getVoice().add(voiceAlarm);

        final List<Alarm> returnedAlarms = AlarmGroups.getAll(alarmGroups);
        assertNotNull(returnedAlarms);
        assertTrue(returnedAlarms.contains(classicAlarm));
        assertTrue(returnedAlarms.contains(expansionAlarm));
        assertTrue(returnedAlarms.contains(voiceAlarm));
    }

    @Test
    public void from() throws Exception {
        final List<Alarm> alarms = new ArrayList<>(3);
        alarms.add(Alarm.generateVoiceAlarmTestCase());
        alarms.add(Alarm.generateExpansionAlarmTestCase());
        alarms.add(new Alarm());

        final AlarmGroups resultAlarmGroups = AlarmGroups.from(alarms);

        assertNotNull(resultAlarmGroups);
        assertEquals(resultAlarmGroups.getClassic().size(), 2);
        assertEquals(resultAlarmGroups.getVoice().size(), 1);
        assertEquals(resultAlarmGroups.getExpansions().size(), 0);
    }

}