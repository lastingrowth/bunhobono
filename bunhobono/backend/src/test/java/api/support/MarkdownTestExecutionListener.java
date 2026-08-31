package api.support;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MarkdownTestExecutionListener
        implements TestExecutionListener {

    private final ConcurrentMap<String, Long> startTimes =
            new ConcurrentHashMap<>();
    private final List<TestResult> results =
            java.util.Collections.synchronizedList(new ArrayList<>());

    private TestPlan testPlan;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    @Override
    public void executionStarted(TestIdentifier identifier) {
        if (identifier.isTest()) {
            startTimes.put(
                    identifier.getUniqueId(),
                    System.nanoTime()
            );
        }
    }

    @Override
    public void executionSkipped(
            TestIdentifier identifier,
            String reason
    ) {
        if (!identifier.isTest()) {
            return;
        }

        resolveReportName(identifier).ifPresent(name ->
                results.add(new TestResult(
                        name,
                        "SKIPPED",
                        0,
                        reason
                ))
        );
    }

    @Override
    public void executionFinished(
            TestIdentifier identifier,
            TestExecutionResult executionResult
    ) {
        if (!identifier.isTest()) {
            return;
        }

        resolveReportName(identifier).ifPresent(name -> {
            long startedAt = startTimes.getOrDefault(
                    identifier.getUniqueId(),
                    System.nanoTime()
            );
            long elapsedMs =
                    (System.nanoTime() - startedAt) / 1_000_000;

            String status = switch (executionResult.getStatus()) {
                case SUCCESSFUL -> "PASS";
                case ABORTED -> "SKIPPED";
                case FAILED -> "FAIL";
            };

            String error = executionResult.getThrowable()
                    .map(Throwable::toString)
                    .orElse("");

            results.add(new TestResult(
                    name,
                    status,
                    elapsedMs,
                    error
            ));
        });
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        List<TestResult> snapshot;

        synchronized (results) {
            snapshot = results.stream()
                    .sorted(Comparator.comparing(TestResult::name))
                    .toList();
        }

        writeReport(
                snapshot.stream()
                        .filter(result -> result.name().startsWith("UT-"))
                        .toList(),
                "Spring 단위 테스트 실행 결과",
                "spring-unit-test-result.md"
        );

        writeReport(
                snapshot.stream()
                        .filter(result -> result.name().startsWith("IT-"))
                        .toList(),
                "Spring 통합 테스트 실행 결과",
                "spring-integration-test-result.md"
        );
    }

    private Optional<String> resolveReportName(
            TestIdentifier identifier
    ) {
        String invocationName = identifier.getDisplayName();

        if (isTestId(invocationName)) {
            return Optional.of(invocationName);
        }

        if (testPlan == null) {
            return Optional.empty();
        }

        TestIdentifier current = identifier;

        while (testPlan.getParent(current).isPresent()) {
            current = testPlan.getParent(current).orElseThrow();
            String parentName = current.getDisplayName();

            if (isTestId(parentName)) {
                return Optional.of(
                        parentName + " / " + invocationName
                );
            }
        }

        return Optional.empty();
    }

    private static boolean isTestId(String displayName) {
        return displayName.startsWith("UT-")
                || displayName.startsWith("IT-");
    }

    private void writeReport(
            List<TestResult> testResults,
            String title,
            String fileName
    ) {
        if (testResults.isEmpty()) {
            return;
        }

        long passed = countStatus(testResults, "PASS");
        long failed = countStatus(testResults, "FAIL");
        long skipped = countStatus(testResults, "SKIPPED");

        StringBuilder markdown = new StringBuilder()
                .append("# ").append(title).append("\n\n")
                .append("| 항목 | 내용 |\n")
                .append("|---|---|\n")
                .append("| 실행 일시 | ")
                .append(LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd HH:mm:ss"
                        )
                ))
                .append(" |\n")
                .append("| 전체 | ").append(testResults.size())
                .append(" |\n")
                .append("| 통과 | ").append(passed).append(" |\n")
                .append("| 실패 | ").append(failed).append(" |\n")
                .append("| 건너뜀 | ").append(skipped).append(" |\n")
                .append("| 판정 | **")
                .append(failed == 0 ? "PASS" : "FAIL")
                .append("** |\n\n")
                .append("## 상세 결과\n\n")
                .append("| 테스트 | 결과 | 시간(ms) | 오류 |\n")
                .append("|---|---|---:|---|\n");

        for (TestResult result : testResults) {
            markdown.append("| ")
                    .append(escape(result.name()))
                    .append(" | **")
                    .append(result.status())
                    .append("** | ")
                    .append(result.elapsedMs())
                    .append(" | ")
                    .append(escape(result.error()))
                    .append(" |\n");
        }

        Path output = reportDirectory().resolve(fileName);

        try {
            Files.createDirectories(output.getParent());
            Files.writeString(
                    output,
                    markdown.toString(),
                    StandardCharsets.UTF_8
            );
            System.out.println(
                    "Markdown test report: "
                            + output.toAbsolutePath()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "테스트 결과 Markdown 생성 실패",
                    exception
            );
        }
    }

    private static long countStatus(
            List<TestResult> testResults,
            String status
    ) {
        return testResults.stream()
                .filter(result -> status.equals(result.status()))
                .count();
    }

    private static Path reportDirectory() {
        Path workingDirectory = Path.of(
                System.getProperty("user.dir")
        ).toAbsolutePath();

        if (Files.isDirectory(
                workingDirectory.resolve("src/test/java")
        )) {
            return workingDirectory.getParent()
                    .resolve("docs/testing/results");
        }

        return workingDirectory.resolve("docs/testing/results");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private record TestResult(
            String name,
            String status,
            long elapsedMs,
            String error
    ) {
    }
}
