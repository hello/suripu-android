package is.hello.sense.api.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import is.hello.sense.api.ApiService;
import is.hello.sense.graph.SenseTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GsonJodaTimeTests extends SenseTestCase {
    private final Gson gson;

    public GsonJodaTimeTests() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(new TypeToken<DateTime>(){}.getType(),
                        new GsonJodaTime.DateTimeSerialization(ISODateTimeFormat.dateTime().withZoneUTC(),
                                GsonJodaTime.SerializeAs.NUMBER))
                .registerTypeAdapter(new TypeToken<LocalDate>(){}.getType(),
                        new GsonJodaTime.LocalDateSerialization(DateTimeFormat.forPattern(ApiService.DATE_FORMAT)))
                .registerTypeAdapter(new TypeToken<LocalTime>(){}.getType(),
                        new GsonJodaTime.LocalTimeSerialization(DateTimeFormat.forPattern(ApiService.TIME_FORMAT)))
                .create();
    }

    @Test
    public void deserializeDateTime() throws Exception {
        String asStringSource = "{\"date_time\": \"2015-07-20T18:45:13.000Z\"}";
        DateTimeTest asString = gson.fromJson(asStringSource, DateTimeTest.class);
        assertEquals(new DateTime(2015, 7, 20, 18, 45, 13, DateTimeZone.UTC), asString.dateTime);

        String asNumberSource = "{\"date_time\": 1437417935790}";
        DateTimeTest asNumber = gson.fromJson(asNumberSource, DateTimeTest.class);
        assertEquals(new DateTime(1437417935790L, DateTimeZone.UTC), asNumber.dateTime);

        String asNullSource = "{\"date_time\": null}";
        DateTimeTest asNull = gson.fromJson(asNullSource, DateTimeTest.class);
        assertNull(asNull.dateTime);

        String asEmptySource = "{\"date_time\": \"\"}";
        DateTimeTest asEmpty = gson.fromJson(asEmptySource, DateTimeTest.class);
        assertNull(asEmpty.dateTime);
    }

    @Test
    public void serializeDateTime() throws Exception {
        DateTimeTest toSerialize = new DateTimeTest();
        toSerialize.dateTime = new DateTime(1437417935790L, DateTimeZone.UTC);
        String json = gson.toJson(toSerialize);
        assertEquals("{\"date_time\":1437417935790}", json);
    }

    @Test
    public void deserializeLocalDate() throws Exception {
        String asStringSource = "{\"local_date\": \"2015-07-20\"}";
        LocalDateTest asString = gson.fromJson(asStringSource, LocalDateTest.class);
        assertEquals(new LocalDate(2015, 7, 20), asString.localDate);

        String asNumberSource = "{\"local_date\": 1437417935790}";
        LocalDateTest asNumber = gson.fromJson(asNumberSource, LocalDateTest.class);
        assertEquals(new LocalDate(1437417935790L), asNumber.localDate);

        String asNullSource = "{\"local_date\": null}";
        LocalDateTest asNull = gson.fromJson(asNullSource, LocalDateTest.class);
        assertNull(asNull.localDate);

        String asEmptySource = "{\"local_date\": \"\"}";
        LocalDateTest asEmpty = gson.fromJson(asEmptySource, LocalDateTest.class);
        assertNull(asEmpty.localDate);
    }

    @Test
    public void serializeLocalDate() throws Exception {
        LocalDateTest toSerialize = new LocalDateTest();
        toSerialize.localDate = new LocalDate(2015, 7, 20);
        String json = gson.toJson(toSerialize);
        assertEquals("{\"local_date\":\"2015-07-20\"}", json);
    }

    @Test
    public void deserializeLocalTime() throws Exception {
        String asStringSource = "{\"local_time\": \"18:45\"}";
        LocalTimeTest asString = gson.fromJson(asStringSource, LocalTimeTest.class);
        assertEquals(new LocalTime(18, 45), asString.localTime);

        String asNumberSource = "{\"local_time\": 1437417935790}";
        LocalTimeTest asNumber = gson.fromJson(asNumberSource, LocalTimeTest.class);
        assertEquals(new LocalTime(1437417935790L), asNumber.localTime);

        String asNullSource = "{\"local_time\": null}";
        LocalTimeTest asNull = gson.fromJson(asNullSource, LocalTimeTest.class);
        assertNull(asNull.localTime);

        String asEmptySource = "{\"local_time\": \"\"}";
        LocalTimeTest asEmpty = gson.fromJson(asEmptySource, LocalTimeTest.class);
        assertNull(asEmpty.localTime);
    }

    @Test
    public void serializeLocalTime() throws Exception {
        LocalTimeTest toSerialize = new LocalTimeTest();
        toSerialize.localTime = new LocalTime(18, 45);
        String json = gson.toJson(toSerialize);
        assertEquals("{\"local_time\":\"18:45\"}", json);
    }


    static class DateTimeTest {
        @SerializedName("date_time")
        public DateTime dateTime;
    }

    static class LocalDateTest {
        @SerializedName("local_date")
        public LocalDate localDate;
    }

    static class LocalTimeTest {
        @SerializedName("local_time")
        public LocalTime localTime;
    }
}
