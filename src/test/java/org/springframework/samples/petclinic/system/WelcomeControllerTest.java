import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Assuming WelcomeController is in the same package or imported properly
public class WelcomeControllerTest {

	private WelcomeController welcomeController;

	@BeforeEach
	public void setUp() {
		welcomeController = new WelcomeController();
	}

	@Test
	@DisplayName("Happy Path: welcome() should return 'welcome'")
	public void testWelcomeReturnsCorrectView() {
		String result = welcomeController.welcome();
		assertEquals("welcome", result, "The welcome view should be 'welcome'");
	}

	@Test
	@DisplayName("Edge Case: welcome() should not return null")
	public void testWelcomeDoesNotReturnNull() {
		String result = welcomeController.welcome();
		assertNotNull(result, "The result should not be null");
	}

	@Test
	@DisplayName("Boundary Condition: welcome() should always return the same constant value on repeated calls")
	public void testMultipleCallsReturnSameValue() {
		String firstCall = welcomeController.welcome();
		String secondCall = welcomeController.welcome();
		assertEquals(firstCall, secondCall, "Repeated calls to welcome() should return the same result");
	}

	@Test
	@DisplayName("Different Invocation: welcome() should not return an empty string")
	public void testWelcomeIsNotEmpty() {
		String result = welcomeController.welcome();
		assertFalse(result.isEmpty(), "The welcome message should not be empty");
	}

	@Test
	@DisplayName("Exception Handling: welcome() should not throw any exceptions")
	public void testWelcomeDoesNotThrowException() {
		assertDoesNotThrow(() -> {
			welcomeController.welcome();
		}, "Calling welcome() should not throw an exception");
	}

}

// Assuming the class under test is defined as follows:
class WelcomeController {

	public String welcome() {
		return "welcome";
	}

}