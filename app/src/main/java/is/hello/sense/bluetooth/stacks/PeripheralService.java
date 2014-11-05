package is.hello.sense.bluetooth.stacks;

import java.util.UUID;

public interface PeripheralService {
    UUID getUuid();
    int getType();
}
