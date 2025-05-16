import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Assuming this is the target class
class WelcomeController {
    public String welcome() {
        return "welcome";
    }
}

public class WelcomeControllerTest {

    private WelcomeController welcomeController;

    @BeforeEach
    public void setUp() {
        welcomeController = new WelcomeController();
    }

    @Test
    @DisplayName("Happy Path: welcome method returns the correct string")
    public void testWelcomeReturnsCorrectView() {
        String result = welcomeController.welcome();
        assertEquals("welcome", result, "The welcome method should return 'welcome'");
    }

    @Test
    @DisplayName("Edge Case: welcome method should never return null")
    public void testWelcomeNotNull() {
        String result = welcomeController.welcome();
        assertNotNull(result, "The welcome method should not return null");
    }

    @Test
    @DisplayName("Idempotence: Multiple calls to welcome should always return the same result")
    public void testWelcomeIsIdempotent() {
        String firstCall = welcomeController.welcome();
        String secondCall = welcomeController.welcome();
        String thirdCall = welcomeController.welcome();
        assertEquals(firstCall, secondCall, "Multiple calls to welcome should return the same result");
        assertEquals(secondCall, thirdCall, "Multiple calls to welcome should return the same result");
    }
    
    @Test
    @DisplayName("Boundary Condition: Verify the length of the returned string")
    public void testWelcomeStringLength() {
        String result = welcomeController.welcome();
        // The expected string "welcome" has 7 characters
        assertEquals(7, result.length(), "The length of the return string should be 7");
    }

    @Test
    @DisplayName("Exception Handling: Calling welcome should not throw any exception")
    public void testWelcomeDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            welcomeController.welcome();
        }, "The welcome method should not throw an exception");
    }
}
