package is.hello.sense.bluetooth.stacks;

import java.util.List;
import java.util.UUID;

public interface Service {
    UUID getUuid();
    int getType();
    List<Service> getIncludedServices();
}
