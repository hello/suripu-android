package is.hello.sense.debug;

import android.support.annotation.NonNull;

import is.hello.sense.api.ApiEndpoint;

public class NamedApiEndpoint extends ApiEndpoint {
    private final String name;

    public NamedApiEndpoint(@NonNull String clientId,
                            @NonNull String clientSecret,
                            @NonNull String url,
                            @NonNull String name) {
        super(clientId, clientSecret, url);

        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final NamedApiEndpoint that = (NamedApiEndpoint) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
