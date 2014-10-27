package is.hello.sense.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;

public class CachedObjectTests extends InjectionTestCase {
    private static final String FILENAME = "Suripu-Unit-Test";

    @Inject ObjectMapper objectMapper;

    private CachedObject<Name> cachedObject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.cachedObject = new CachedObject<>(getInstrumentation().getContext(),
                                               FILENAME,
                                               new TypeReference<Name>() {},
                                               objectMapper);
    }


    public void testGet() throws Exception {
        fail();
    }

    public void testSet() throws Exception {
        fail();
    }


    private static class Name {
        public String firstName;
        public String lastName;


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
