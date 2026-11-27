package com.clausify.contract;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #110: a 0.9 MB PDF that draws ~300 MB of text, read in a separate JVM capped at 160 MB of heap, with a watchdog.
 * Before the text cap this ran out of memory; now it is rejected and the next ordinary PDF still works.
 */
class PdfTextExtractorIsolationTest {

    @Test
    void textBombIsRejectedInsideASmallHeap() throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        Process probe = new ProcessBuilder(java, "-Xmx160m", "-XX:+ExitOnOutOfMemoryError",
                "-cp", System.getProperty("java.class.path"), HostilePdfProbe.class.getName(), "300")
                .redirectErrorStream(true)
                .start();

        boolean finished = probe.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            probe.destroyForcibly();
        }
        String output = new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(finished).as("probe finished within 120 s").isTrue();
        assertThat(probe.exitValue()).as(output).isZero();
        assertThat(output)
                .contains("REJECTED " + PdfTextExtractor.TOO_MUCH_TEXT)
                .contains("NEXT_PAGES 2");
    }
}
