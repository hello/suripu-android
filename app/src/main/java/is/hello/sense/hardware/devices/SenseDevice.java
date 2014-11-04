package is.hello.sense.hardware.devices;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.hardware.Device;
import is.hello.sense.hardware.DeviceCenter;
import rx.Observable;

public class SenseDevice {
    private static final byte[] MORPHEUS_SERVICE_UUID_BYTES = new byte[]{
            0x23, (byte) 0xD1, (byte) 0xBC, (byte) 0xEA, 0x5F, 0x78,  //785FEABCD123
            0x23, 0x15,   // 1523
            (byte) 0xDE, (byte) 0xEF,   // EFDE
            0x12, 0x12,   // 1212
            (byte) 0xE1, (byte) 0xFE, 0x00, 0x00  // 0000FEE1
    };

    private Device device;

    public static Observable<List<SenseDevice>> scan(@NonNull DeviceCenter deviceCenter) {
        DeviceCenter.ScanCriteria criteria = new DeviceCenter.ScanCriteria();
        criteria.setScanRecord(MORPHEUS_SERVICE_UUID_BYTES);
        return deviceCenter.scanForDevice(criteria, 10 * 1000).map(SenseDevice::fromDevices);
    }

    public static List<SenseDevice> fromDevices(@NonNull List<Device> devices) {
        List<SenseDevice> mapped = new ArrayList<>();
        for (Device device : devices) {
            mapped.add(new SenseDevice(device));
        }
        return mapped;
    }

    public SenseDevice(@NonNull Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }
}
