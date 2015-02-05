package is.hello.sense.api.sessions;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import is.hello.sense.BuildConfig;
import retrofit.mime.TypedOutput;

public class OAuthCredentials implements TypedOutput {
    public final String username;
    public final String password;
    private final ByteArrayOutputStream outputStream;

    public OAuthCredentials(@NonNull String username, @NonNull String password) {
        if (TextUtils.isEmpty(username))
            throw new IllegalArgumentException("username cannot be omitted");

        if (TextUtils.isEmpty(password))
            throw new IllegalArgumentException("password cannot be omitted");

        this.username = username;
        this.password = password;
        this.outputStream = new ByteArrayOutputStream();

        generate();
    }


    private void addField(@NonNull String name, @NonNull String value) {
        try {
            if (outputStream.size() > 0)
                outputStream.write('&');

            String pair = Uri.encode(name) + "=" + Uri.encode(value);
            outputStream.write(pair.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generate() {
        addField("grant_type", "password");
        addField("client_id", BuildConfig.CLIENT_ID);
        addField("client_secret", BuildConfig.CLIENT_SECRET);
        addField("username", username);
        addField("password", password);
    }

    @Override
    public String fileName() {
        return null;
    }

    @Override
    public String mimeType() {
        return "application/x-www-form-urlencoded";
    }

    @Override
    public long length() {
        return outputStream.size();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(outputStream.toByteArray());
    }
}
