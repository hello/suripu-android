package com.hello.ble;

import android.util.Log;

import com.google.common.io.LittleEndianDataInputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 7/7/14.
 */
public class PillMotionData {

    public final static int STRUCT_HEADER_SIZE = 1 + 1 + 2 + 8 + 2;

    public final DateTime timestamp;
    public final Long maxAmplitude;

    public PillMotionData(final DateTime timestamp, final Long maxAmplitude) {
        this.timestamp = timestamp;
        this.maxAmplitude = maxAmplitude;
    }


    public static List<PillMotionData> fromBytes(final byte[] payload, final int unitLength) {
        final List<PillMotionData> list = new ArrayList<>();
        final ByteArrayInputStream pillByteArrayInputStream = new ByteArrayInputStream(payload);
        final LittleEndianDataInputStream pillInputStream = new LittleEndianDataInputStream(pillByteArrayInputStream);


        try {

            byte version = pillInputStream.readByte();
            byte reserved1 = pillInputStream.readByte();


            int structLength = pillInputStream.readUnsignedShort();


            long timestamp = pillInputStream.readLong();

            int validIndex = pillInputStream.readUnsignedShort();
            int currentIndex = validIndex == 0xFFFF ? 0 : validIndex + 1;
            final long[] valueList = new long[(structLength - STRUCT_HEADER_SIZE) / (unitLength / 8)];
            for (int i = 0; i < valueList.length; i++) {
                if (unitLength == 16) {
                    valueList[i] = pillInputStream.readUnsignedShort();
                } else if (unitLength == 32) {
                    valueList[i] = pillInputStream.readInt();
                    if (valueList[i] < 0) {
                        valueList[i] += 0xFFFFFFFF;
                    }
                }

            }

            pillInputStream.close();

            if (validIndex != 0xFFFF) {

                final DateTime startTime = new DateTime(timestamp, DateTimeZone.UTC);

                if (currentIndex > valueList.length - 1) {
                    currentIndex = 0;
                }

                DateTime currentDataTime = startTime;

                if (validIndex > valueList.length - 1 || payload.length != structLength) {
                    throw new IllegalArgumentException("Corrupted data");
                }

                int index = validIndex;
                while (list.size() < valueList.length - 1) {
                    if (index != currentIndex) {
                        long value = valueList[index] - 1;
                        final PillMotionData pillMotionData = new PillMotionData(currentDataTime, value);
                        list.add(0, pillMotionData);
                        currentDataTime = currentDataTime.minusMinutes(1);
                        Log.i("IMU DATA", pillMotionData.timestamp + ", " + pillMotionData.maxAmplitude);
                    }

                    index--;

                    if (index == -1) {
                        index = valueList.length - 1;
                    }
                }

            }

            Log.i("Current max at index " + currentIndex, String.valueOf(valueList[currentIndex]));

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (IllegalFieldValueException ifvEx) {
            ifvEx.printStackTrace();
        }


        return list;
    }
}
