import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WelcomeControllerTest {

	private WelcomeController welcomeController;

	@BeforeEach
	public void setUp() {
		welcomeController = new WelcomeController();
	}

	@Test
	@DisplayName("Should return 'welcome' when welcome() is called")
	public void testWelcomeReturnsCorrectView() {
		String expected = "welcome";
		String actual = welcomeController.welcome();
		assertEquals(expected, actual, "The welcome method should return 'welcome'");
	}

	@Test
	@DisplayName("Should not return a null value")
	public void testWelcomeIsNotNull() {
		String result = welcomeController.welcome();
		assertNotNull(result, "The welcome method should not return null");
	}

	@Test
	@DisplayName("Should return a non-empty string")
	public void testWelcomeReturnsNonEmptyString() {
		String result = welcomeController.welcome();
		assertFalse(result.trim().isEmpty(), "The returned string should not be empty");
	}

	@Test
	@DisplayName("Should return a consistent value on multiple calls")
	public void testWelcomeConsistentAcrossMultipleCalls() {
		String firstCall = welcomeController.welcome();
		String secondCall = welcomeController.welcome();
		String thirdCall = welcomeController.welcome();
		assertEquals(firstCall, secondCall, "Multiple calls should return the same result");
		assertEquals(secondCall, thirdCall, "Multiple calls should return the same result");
	}

	@Test
	@DisplayName("Should not throw any exceptions when calling welcome()")
	public void testWelcomeDoesNotThrowException() {
		assertDoesNotThrow(() -> welcomeController.welcome(), "The welcome method should not throw any exceptions");
	}

}

// For completeness, here is the WelcomeController class definition. In a real project,
// this would be in its own file.
class WelcomeController {

	public String welcome() {
		return "welcome";
	}

}
