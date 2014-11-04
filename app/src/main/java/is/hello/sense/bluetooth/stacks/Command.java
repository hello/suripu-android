package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.UUID;

public final class Command {
    public final @NonNull UUID identifier;
    public final @NonNull byte[] payload;

    public static @NonNull Command with(@NonNull UUID identifier, @NonNull byte[] payload) {
        return new Command(identifier, payload);
    }

    private Command(@NonNull UUID identifier, @NonNull byte[] payload) {
        this.identifier = identifier;
        this.payload = payload;
    }


    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Command command = (Command) o;

        if (!identifier.equals(command.identifier)) return false;
        if (!Arrays.equals(payload, command.payload)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "Command{" +
                "identifier=" + identifier +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }
}
