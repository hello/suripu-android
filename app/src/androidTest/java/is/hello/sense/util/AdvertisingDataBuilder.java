package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.bluetooth.stacks.util.BluetoothUtils;

public class AdvertisingDataBuilder {
    private static final int HEADER_LENGTH = 2;

    private final List<Pair<Integer, byte[]>> entries = new ArrayList<>();
    private int totalSize = 0;

    public void add(int type, @NonNull String payload) {
        byte[] payloadAsBytes = BluetoothUtils.convertStringToBytes(payload);
        entries.add(Pair.create(type, payloadAsBytes));
        totalSize += HEADER_LENGTH + payloadAsBytes.length;
    }

    public byte[] buildRaw() {
        byte[] buffer = new byte[totalSize];

        int pointer = 0;
        for (Pair<Integer, byte[]> entry : entries) {
            //noinspection UnnecessaryLocalVariable
            int lengthOffset = pointer;
            int typeOffset = pointer + 1;
            int dataOffset = pointer + 2;
            int dataLength = entry.second.length;
            int entryLength = dataLength + 1;

            buffer[lengthOffset] = (byte) entryLength;
            buffer[typeOffset] = entry.first.byteValue();

            System.arraycopy(
                /* src */ entry.second,
                /* srcStart */ 0,
                /* dest */ buffer,
                /* destStart */ dataOffset,
                /* length */ dataLength
            );

            pointer += dataLength + HEADER_LENGTH;
        }

        return buffer;
    }

    public AdvertisingData build() {
        return AdvertisingData.parse(buildRaw());
    }
}
