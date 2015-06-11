package is.hello.sense.api.sessions;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

import is.hello.sense.BuildConfig;
import is.hello.sense.api.ApiEndpoint;

import static is.hello.sense.AssertExtensions.assertNoThrow;
import static is.hello.sense.AssertExtensions.assertThrows;

public class OAuthCredentialsTests extends TestCase {
    private final ApiEndpoint defaultApiEndpoint = new ApiEndpoint();

    @SuppressWarnings("ConstantConditions")
    public void testConstraints() {
        assertThrows(() -> new OAuthCredentials(defaultApiEndpoint, "", "password"));
        assertThrows(() -> new OAuthCredentials(defaultApiEndpoint, "username", ""));
        assertThrows(() -> new OAuthCredentials(defaultApiEndpoint, null, "password"));
        assertThrows(() -> new OAuthCredentials(defaultApiEndpoint, "username", null));
    }

    public void testOutput() {
        String expected = ("grant_type=password&client_id=" + BuildConfig.CLIENT_ID +
                "&client_secret=" + BuildConfig.CLIENT_SECRET +
                "&username=test123&password=321tset");

        OAuthCredentials credentials = new OAuthCredentials(defaultApiEndpoint, "test123", "321tset");
        assertNull(credentials.fileName());
        assertEquals("application/x-www-form-urlencoded", credentials.mimeType());
        assertEquals(expected.length(), credentials.length());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertNoThrow(() -> credentials.writeTo(outputStream));
        String output = new String(outputStream.toByteArray());
        assertEquals(expected, output);
    }
}
