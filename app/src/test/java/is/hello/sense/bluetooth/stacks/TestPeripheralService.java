package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.UUID;

public class TestPeripheralService implements PeripheralService {
    private final UUID uuid;
    private final int type;

    public TestPeripheralService(@NonNull UUID uuid, int type) {
        this.uuid = uuid;
        this.type = type;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int getType() {
        return type;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestPeripheralService that = (TestPeripheralService) o;

        return type == that.type && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + type;
        return result;
    }

    @Override
    public String toString() {
        return "TestPeripheralService{" +
                "uuid=" + uuid +
                ", type=" + type +
                '}';
    }
}
