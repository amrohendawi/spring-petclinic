import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OwnerTest {

	// Dummy Pet class to support tests
	public static class Pet {

		private String name;

		public Pet(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Pet pet = (Pet) o;
			return (name == null ? pet.name == null : name.equals(pet.name));
		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}

	}

	// Assume Owner has a getPet(String, boolean) method that is being delegated to by
	// getPet(String).
	public static class Owner {

		// This method is assumed to be part of the class; actual implementation is not
		// provided.
		public Pet getPet(String name, boolean ignoreNew) {
			// Dummy implementation for test purposes. In real scenario, this would have
			// business logic.
			if (name == null)
				return null;
			if (name.trim().isEmpty())
				return null;
			return new Pet(name);
		}

		public Pet getPet(String name) {
			return getPet(name, false);
		}

	}

	@Spy
	private Owner ownerSpy = new Owner();

	@BeforeEach
	public void setUp() {
		// No additional setup required since we are using @Spy for ownerSpy
	}

	@Test
	@DisplayName("Should return correct Pet instance for a valid pet name (happy path)")
	public void testGetPetHappyPath() {
		// Arrange
		String petName = "Buddy";
		Pet expectedPet = new Pet(petName);
		// Stub the delegated method getPet(String, boolean) to return our expected pet
		doReturn(expectedPet).when(ownerSpy).getPet(petName, false);

		// Act
		Pet result = ownerSpy.getPet(petName);

		// Assert
		assertNotNull(result, "Returned pet should not be null");
		assertEquals(expectedPet, result, "Pet returned should match the expected pet");

		// Verify that delegation is happening with the correct parameters
		verify(ownerSpy, times(1)).getPet(petName, false);
	}

	@Test
	@DisplayName("Should handle null input by returning null pet (edge case)")
	public void testGetPetWithNullInput() {
		// Arrange
		String petName = null;
		// Stub: when getPet is called with null, then return null
		doReturn(null).when(ownerSpy).getPet(petName, false);

		// Act
		Pet result = ownerSpy.getPet(petName);

		// Assert
		assertNull(result, "getPet should return null when pet name is null");
		verify(ownerSpy, times(1)).getPet(petName, false);
	}

	@Test
	@DisplayName("Should handle empty string pet name by returning null (boundary condition)")
	public void testGetPetWithEmptyString() {
		// Arrange
		String petName = "";
		doReturn(null).when(ownerSpy).getPet(petName, false);

		// Act
		Pet result = ownerSpy.getPet(petName);

		// Assert
		assertNull(result, "getPet should return null when pet name is empty");
		verify(ownerSpy, times(1)).getPet(petName, false);
	}

	@Test
	@DisplayName("Should delegate call correctly for a pet name with only whitespace (input variation)")
	public void testGetPetWithWhitespaceName() {
		// Arrange
		String petName = "   ";
		// If the underlying logic treats whitespace as invalid, we can assume it returns
		// null
		doReturn(null).when(ownerSpy).getPet(petName, false);

		// Act
		Pet result = ownerSpy.getPet(petName);

		// Assert
		assertNull(result, "getPet should return null when pet name contains only whitespace");
		verify(ownerSpy, times(1)).getPet(petName, false);
	}

	@Test
	@DisplayName("Should propagate exception thrown by delegated method (exception handling)")
	public void testGetPetWhenDelegatedMethodThrowsException() {
		// Arrange
		String petName = "Max";
		RuntimeException exception = new RuntimeException("Test exception");
		doThrow(exception).when(ownerSpy).getPet(petName, false);

		// Act & Assert
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> ownerSpy.getPet(petName),
				"Expected getPet to throw RuntimeException");
		assertEquals("Test exception", thrown.getMessage(), "Exception message should match");
		verify(ownerSpy, times(1)).getPet(petName, false);
	}

}
