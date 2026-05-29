package computer.brads.flowchat.core;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.List;

/**
 * JUnit 4 tests wrapping FlowChatTestRunner + additional edge cases.
 */
public class FlowChatTestRunnerTest {

    private List<FlowChatTestRunner.TestResult> results;

    @Before
    public void setUp() {
        results = FlowChatTestRunner.runCommonTests();
    }

    @Test
    public void allCommonTestsPass() {
        StringBuilder failures = new StringBuilder();
        int passed = 0;
        for (FlowChatTestRunner.TestResult r : results) {
            if (r.passed) {
                passed++;
            } else {
                failures.append(String.format("  FAIL #%d %s: %s\n", r.number, r.name, r.error));
            }
        }
        if (failures.length() > 0) {
            fail(String.format("%d/%d tests failed:\n%s", results.size() - passed, results.size(), failures));
        }
        assertTrue("Expected at least 20 tests, got " + results.size(), results.size() >= 20);
    }

    @Test
    public void testRunnerReturnsResults() {
        assertNotNull("Results should not be null", results);
        assertFalse("Results should not be empty", results.isEmpty());
    }
}
