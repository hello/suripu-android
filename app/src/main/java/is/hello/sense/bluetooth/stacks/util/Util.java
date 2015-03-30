package is.hello.sense.bluetooth.stacks.util;

import is.hello.sense.bluetooth.stacks.Peripheral;

public class Util {
    public static String peripheralConfigToString(@Peripheral.Config int peripheralConfig) {
        if (peripheralConfig == Peripheral.CONFIG_EMPTY) {
            return "CONFIG_EMPTY";
        }

        String options = "";
        if ((peripheralConfig & Peripheral.CONFIG_FRAGILE_BONDS) == Peripheral.CONFIG_FRAGILE_BONDS) {
            options += "CONFIG_FRAGILE_BONDS | ";
        }
        if ((peripheralConfig & Peripheral.CONFIG_AUTO_ACTIVATE_COMPATIBILITY_SHIMS) == Peripheral.CONFIG_AUTO_ACTIVATE_COMPATIBILITY_SHIMS) {
            options += "CONFIG_AUTO_ACTIVATE_COMPATIBILITY_SHIMS | ";
        }
        if ((peripheralConfig & Peripheral.CONFIG_WAIT_AFTER_SERVICE_DISCOVERY) == Peripheral.CONFIG_WAIT_AFTER_SERVICE_DISCOVERY) {
            options += "CONFIG_WAIT_AFTER_SERVICE_DISCOVERY | ";
        }

        if (options.endsWith(" | ")) {
            options = options.substring(0, options.length() - 3);
        }

        return options;
    }
}
