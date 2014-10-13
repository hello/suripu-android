package is.hello.sense.api.sessions;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

import is.hello.sense.api.ApiEnvironment;

import static is.hello.sense.AssertExtensions.assertNoThrow;
import static is.hello.sense.AssertExtensions.assertThrows;

public class OAuthCredentialsTests extends TestCase {
    private final ApiEnvironment API_ENVIRONMENT = ApiEnvironment.STAGING;

    @SuppressWarnings("ConstantConditions")
    public void testConstraints() {
        assertThrows(() -> new OAuthCredentials(API_ENVIRONMENT, "", "password"));
        assertThrows(() -> new OAuthCredentials(API_ENVIRONMENT, "username", ""));
        assertThrows(() -> new OAuthCredentials(API_ENVIRONMENT, null, "password"));
        assertThrows(() -> new OAuthCredentials(API_ENVIRONMENT, "username", null));
    }

    public void testOutput() {
        OAuthCredentials credentials = new OAuthCredentials(API_ENVIRONMENT, "test123", "321tset");
        assertNull(credentials.fileName());
        assertEquals("application/x-www-form-urlencoded", credentials.mimeType());
        assertEquals(101, credentials.length());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertNoThrow(() -> credentials.writeTo(outputStream));
        String output = new String(outputStream.toByteArray());
        assertEquals("grant_type=password&client_id=android_dev&client_secret=99999secret&username=test123&password=321tset", output);
    }
}
