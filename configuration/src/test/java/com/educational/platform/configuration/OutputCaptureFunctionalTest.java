package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functionally validates the {@code OutputCaptureExtension} provided by
 * {@code spring-boot-starter-test}. TestSliceAnnotationFunctionalTest
 * verifies it is on the classpath; this test verifies it actually captures
 * {@code System.out} and {@code System.err} output, which is essential for
 * validating bootRun startup log assertions in integration tests.
 */
@ExtendWith(OutputCaptureExtension.class)
class OutputCaptureFunctionalTest {

    @Test
    void outputCapture_shouldCaptureSystemOut(CapturedOutput output) {
        System.out.println("bootRun-stdout-verification");
        assertThat(output.getOut())
                .as("CapturedOutput must capture System.out writes for bootRun log verification")
                .contains("bootRun-stdout-verification");
    }

    @Test
    void outputCapture_shouldCaptureSystemErr(CapturedOutput output) {
        System.err.println("bootRun-stderr-verification");
        assertThat(output.getErr())
                .as("CapturedOutput must capture System.err writes for bootRun error log verification")
                .contains("bootRun-stderr-verification");
    }

    @Test
    void outputCapture_shouldCaptureAllOutput(CapturedOutput output) {
        System.out.println("stdout-line");
        System.err.println("stderr-line");
        assertThat(output.getAll())
                .as("CapturedOutput.getAll() must include both stdout and stderr")
                .contains("stdout-line")
                .contains("stderr-line");
    }

    @Test
    void outputCapture_shouldBeIsolatedPerTest(CapturedOutput output) {
        System.out.println("isolated-test-output");
        assertThat(output.getOut())
                .as("Each test method must receive a fresh CapturedOutput (no bleed from other tests)")
                .contains("isolated-test-output")
                .doesNotContain("bootRun-stdout-verification");
    }

    @Test
    void outputCapture_toString_shouldContainAllOutput(CapturedOutput output) {
        System.out.println("toString-verification");
        assertThat(output.toString())
                .as("CapturedOutput.toString() must include captured output for assertion convenience")
                .contains("toString-verification");
    }
}
