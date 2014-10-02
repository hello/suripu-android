package com.hello.ble.util;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by pangwu on 7/2/14.
 */
public class BleDateTimeConverter {

    public static byte[] dateTimeToBLETime(final DateTime dateTime) {

        final DateTime utcDateTime = new DateTime(dateTime.getMillis(), DateTimeZone.UTC).withMillisOfSecond(0);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final LittleEndianDataOutputStream outputStream = new LittleEndianDataOutputStream(byteArrayOutputStream);
        byte[] bleDateTime = null;

        try {
            /*outputStream.writeShort(utcDateTime.getYear());
            outputStream.writeByte(utcDateTime.getMonthOfYear());
            outputStream.writeByte(utcDateTime.getDayOfMonth());
            outputStream.writeByte(utcDateTime.getHourOfDay());
            outputStream.writeByte(utcDateTime.getMinuteOfHour());
            outputStream.writeByte(utcDateTime.getSecondOfMinute());
            outputStream.writeByte(utcDateTime.getDayOfWeek());*/


            outputStream.writeLong(utcDateTime.getMillis());
            outputStream.flush();

            bleDateTime = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            outputStream.close();


        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return bleDateTime;

    }

    public static DateTime bleTimeToDateTime(final byte[] bleTime) {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bleTime);
        final LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(byteArrayInputStream);
        byte[] bleDateTime = null;

        try {
            /*int year = inputStream.readShort();
            byte month = inputStream.readByte();
            byte day = inputStream.readByte();
            byte hour = inputStream.readByte();
            byte minute = inputStream.readByte();
            byte second = inputStream.readByte();

            return new DateTime(year, month, day, hour, minute, second, DateTimeZone.UTC);
            */

            long timestamp = inputStream.readLong();
            return new DateTime(timestamp, DateTimeZone.UTC);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (IllegalFieldValueException ifvex) {
            ifvex.printStackTrace();
        }

        return null;

    }
}
