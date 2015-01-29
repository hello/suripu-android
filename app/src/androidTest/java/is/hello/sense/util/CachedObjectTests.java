package is.hello.sense.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;

public class CachedObjectTests extends InjectionTestCase {
    private static final String FILENAME = "Suripu-Unit-Test";

    @Inject ObjectMapper objectMapper;

    private CachedObject<Name> cachedObject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        File cacheDirectory = getInstrumentation().getTargetContext().getCacheDir();
        assertNotNull(cacheDirectory);
        this.cachedObject = new CachedObject<>(CachedObject.getFile(cacheDirectory, FILENAME),
                                               new TypeReference<Name>() {},
                                               objectMapper);
    }


    public void testGet() throws Exception {
        Name name = new Name("John", "Smith");
        assertNotNull(Sync.last(cachedObject.set(name)));

        Name retrievedName = Sync.last(cachedObject.get());
        assertEquals(retrievedName, name);
    }

    public void testSet() throws Exception {
        Name name = new Name("John", "Smith");
        assertEquals(name, Sync.last(cachedObject.set(name)));
    }


    public static class Name {
        @JsonProperty("first_name")
        public final String firstName;

        @JsonProperty("last_name")
        public final String lastName;

        public Name(@JsonProperty("first_name") String firstName,
                    @JsonProperty("last_name") String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Name name = (Name) o;

            if (firstName != null ? !firstName.equals(name.firstName) : name.firstName != null)
                return false;
            if (lastName != null ? !lastName.equals(name.lastName) : name.lastName != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = firstName != null ? firstName.hashCode() : 0;
            result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
            return result;
        }


        @Override
        public String toString() {
            return "Name{" +
                    "firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    '}';
        }
    }
}
