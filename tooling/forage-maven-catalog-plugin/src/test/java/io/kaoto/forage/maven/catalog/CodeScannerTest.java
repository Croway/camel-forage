package io.kaoto.forage.maven.catalog;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CodeScanner path filtering.
 */
public class CodeScannerTest {

    @Test
    public void testScannablePathAcceptsRegularSources() {
        Path scanRoot = Path.of("/home/user/forage/library/module/src/main/java");
        Path file = scanRoot.resolve("io/kaoto/forage/Example.java");

        assertThat(CodeScanner.isScannablePath(scanRoot, file)).isTrue();
    }

    @Test
    public void testScannablePathSkipsTestDirectoriesBelowScanRoot() {
        Path scanRoot = Path.of("/home/user/forage/library/module");
        Path testFile = scanRoot.resolve("src/test/java/io/kaoto/forage/ExampleTest.java");

        assertThat(CodeScanner.isScannablePath(scanRoot, testFile)).isFalse();
    }

    @Test
    public void testScannablePathSkipsTargetDirectoriesBelowScanRoot() {
        Path scanRoot = Path.of("/home/user/forage/library/module");
        Path generatedFile = scanRoot.resolve("target/generated-sources/io/kaoto/forage/Generated.java");

        assertThat(CodeScanner.isScannablePath(scanRoot, generatedFile)).isFalse();
    }

    @Test
    public void testScannablePathIgnoresTestSegmentAboveScanRoot() {
        // A checkout under a directory literally named "test" must not cause files to be skipped
        Path scanRoot = Path.of("/home/ci/test/forage/library/module/src/main/java");
        Path file = scanRoot.resolve("io/kaoto/forage/Example.java");

        assertThat(CodeScanner.isScannablePath(scanRoot, file)).isTrue();
    }

    @Test
    public void testScannablePathIgnoresTargetSegmentAboveScanRoot() {
        Path scanRoot = Path.of("/home/ci/target/forage/library/module/src/main/java");
        Path file = scanRoot.resolve("io/kaoto/forage/Example.java");

        assertThat(CodeScanner.isScannablePath(scanRoot, file)).isTrue();
    }

    @Test
    public void testScannablePathSkipsTestSegmentBelowScanRootEvenWhenRootContainsTest() {
        // Segments below the scan root are still filtered when the root itself lives under "test"
        Path scanRoot = Path.of("/home/ci/test/forage/library/module");
        Path testFile = scanRoot.resolve("src/test/java/io/kaoto/forage/ExampleTest.java");

        assertThat(CodeScanner.isScannablePath(scanRoot, testFile)).isFalse();
    }
}
