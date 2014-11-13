package is.hello.sense;

import com.google.protobuf.ByteString;

import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;

public final class Fixtures {
    public static final MorpheusBle.wifi_endpoint[] SCAN_RESULTS_1 = {
            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-10)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WPA2)
                    .setSsid("Hello")
                    .setBssid(ByteString.copyFrom(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06}))
                    .build(),

            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-16)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WPA2)
                    .setSsid("Hello")
                    .setBssid(ByteString.copyFrom(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x07}))
                    .build(),

            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-5)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                    .setSsid("Hello Guest")
                    .setBssid(ByteString.copyFrom(new byte[] {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16}))
                    .build(),
    };

    public static final MorpheusBle.wifi_endpoint[] SCAN_RESULTS_2 = {
            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-11)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WPA2)
                    .setSsid("Hello")
                    .setBssid(ByteString.copyFrom(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06}))
                    .build(),

            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-13)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WPA2)
                    .setSsid("Hello")
                    .setBssid(ByteString.copyFrom(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x07}))
                    .build(),

            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-3)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                    .setSsid("Hello Guest")
                    .setBssid(ByteString.copyFrom(new byte[] {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16}))
                    .build(),

            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-7)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                    .setSsid("Go Away")
                    .setBssid(ByteString.copyFrom(new byte[] {0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26}))
                    .build(),

            MorpheusBle.wifi_endpoint.newBuilder()
                    .setRssi(-1)
                    .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WEP)
                    .setSsid("Mostly Radiation")
                    .setBssid(ByteString.copyFrom(new byte[] {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36}))
                    .build(),
    };
}
