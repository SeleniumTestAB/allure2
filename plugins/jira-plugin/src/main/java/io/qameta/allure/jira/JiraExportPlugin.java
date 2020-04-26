/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.jira;

import io.qameta.allure.Aggregator;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.ExecutorInfo;
import io.qameta.allure.entity.Link;
import io.qameta.allure.entity.Statistic;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.PropertyUtils.getProperty;

/**
 * @author eroshenkoam (Artem Eroshenko).
 */
public class JiraExportPlugin implements Aggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraExportPlugin.class);

    private static final String EXECUTORS_BLOCK_NAME = "executor";

    private static final String ALLURE_JIRA_ENABLED = "ALLURE_JIRA_ENABLED";
    private static final String ALLURE_JIRA_LAUNCH_ISSUES = "ALLURE_JIRA_LAUNCH_ISSUES";

    private final Supplier<JiraService> jiraServiceSupplier;
    private final boolean enabled;
    private final String issues;

    public JiraExportPlugin() {
        this(
                getProperty(ALLURE_JIRA_ENABLED).map(Boolean::parseBoolean).orElse(false),
                getProperty(ALLURE_JIRA_LAUNCH_ISSUES).orElse(""),
                () -> new JiraServiceBuilder().defaults().build()
        );
    }

    public JiraExportPlugin(final boolean enabled,
                            final String issues,
                            final Supplier<JiraService> jiraServiceSupplier) {
        this.jiraServiceSupplier = jiraServiceSupplier;
        this.enabled = enabled;
        this.issues = issues;
    }

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final Path outputDirectory) {
        if (enabled) {
            final JiraService jiraService = jiraServiceSupplier.get();

            final List<String> issues = splitByComma(this.issues);
            final ExecutorInfo executor = getExecutor(launchesResults);
            final Statistic statisticToConvert = getStatistic(launchesResults);
            final List<LaunchStatisticExport> statistic = convertStatistics(statisticToConvert);
            final JiraLaunch launch = getJiraLaunch(executor, statistic);
            final List<JiraExportResult> created = exportLaunchToJira(jiraService, launch, issues);

            getTestResults(launchesResults).stream()
                    .map(testResult -> getJiraTestResult(executor, testResult))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(testResult -> {
                        getTestResults(launchesResults).stream()
                                .forEach(testResult1 -> exportTestResultToJira(jiraService, testResult, testResult1));
                    });
        }
    }

    private JiraLaunch getJiraLaunch(final ExecutorInfo executor,
                                     final List<LaunchStatisticExport> statistic) {
        return new JiraLaunch()
                .setStatistic(statistic)
                .setName(executor.getBuildName())
                .setUrl(executor.getReportUrl())
                .setDate(System.currentTimeMillis());
    }

    private Optional<JiraTestResult> getJiraTestResult(final ExecutorInfo executor,
                                                       final TestResult testResult) {
        final List<String> issues = testResult.getLinks().stream()
                .filter(this::isIssueLink)
                .map(Link::getName)
                .collect(Collectors.toList());

        if (issues.isEmpty()) {
            return Optional.empty();
        } else {
            final JiraTestResult jiraTestResult = new JiraTestResult()
                    .setTestCaseId(testResult.getTestId())
                    .setHistoryKey(testResult.getHistoryId())
                    .setName(testResult.getName())
                    .setUrl(getJiraTestResultUrl(executor.getReportUrl(), testResult.getUid()))
                    .setStatus(testResult.getStatus().toString())
                    .setColor(findColorForStatus(testResult.getStatus()))
                    .setDate(testResult.getTime().getStop())
                    .setLaunchUrl(executor.getReportUrl())
                    .setLaunchName(executor.getBuildName());
            return Optional.of(jiraTestResult);
        }
    }


    private List<TestResult> getTestResults(final List<LaunchResults> launchesResults) {
        return launchesResults.stream()
                .map(LaunchResults::getAllResults)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private ExecutorInfo getExecutor(final List<LaunchResults> launchesResults) {
        return launchesResults.stream()
                .map(launchResults -> launchResults.getExtra(EXECUTORS_BLOCK_NAME))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(ExecutorInfo.class::isInstance)
                .map(ExecutorInfo.class::cast)
                .findFirst()
                .orElse(new ExecutorInfo());
    }

    private Statistic getStatistic(final List<LaunchResults> launchesResults) {
        final Statistic statistic = new Statistic();
        launchesResults.stream()
                .map(LaunchResults::getAllResults)
                .flatMap(Collection::stream)
                .forEach(statistic::update);
        return statistic;
    }

    private List<LaunchStatisticExport> convertStatistics(final Statistic statistic) {
        return Stream.of(Status.values()).map(status ->
                new LaunchStatisticExport(status.value(), findColorForStatus(status), statistic.get(status)))
                .collect(Collectors.toList());

    }

    private String findColorForStatus(final Status status) {
        switch (status) {
            case FAILED:
                return StatusColor.RED.value();
            case PASSED:
                return StatusColor.GREEN.value();
            case SKIPPED:
                return StatusColor.GRAY.value();
            default:
                return StatusColor.YELLOW.value();
        }
    }

    private List<JiraExportResult> exportLaunchToJira(final JiraService jiraService,
                                                      final JiraLaunch launch,
                                                      final List<String> issues) {
        try {
            final List<JiraExportResult> created = jiraService.createJiraLaunch(launch, issues);
            LOGGER.info(String.format("Allure launch '%s' synced with issues  successfully",
                    issues));
            return created;
        } catch (Throwable e) {
            LOGGER.error(String.format("Allure launch sync with issue '%s' error", issues), e);
            throw e;
        }
    }

    private void exportTestResultToJira(final JiraService jiraService, final JiraTestResult jiraTestResult, final TestResult testResult) {
        try {
            final List<String> issues = testResult.getLinks().stream()
                    .filter(this::isIssueLink)
                    .map(Link::getName)
                    .collect(Collectors.toList());

            final List<JiraExportResult> created = jiraService.createTestResult(jiraTestResult, issues);
            created.forEach(creation -> {
                        LOGGER.info(String.format("Allure test result '%s' synced with issue '%s' successfully",
                                creation.getExternalId(),
                                creation.getIssueKey()));
                    }

            );
        } catch (Throwable e) {
            LOGGER.error(String.format("Allure test result sync with issue '%s' failed", jiraTestResult.getExternalId()), e);
            throw e;
        }
    }


    private String getJiraTestResultUrl(final String reportUrl, final String uuid) {
        return Optional.ofNullable(reportUrl)
                .map(url -> url.endsWith("index.html") ? "%s#testresult/%s" : "%s/#testresult/%s")
                .map(pattern -> String.format(pattern, reportUrl, uuid))
                .orElse(null);
    }

    private boolean isIssueLink(final Link link) {
        return "issue".equals(link.getType());
    }

    private static List<String> splitByComma(final String value) {
        return Arrays.asList(value.split(","));
    }

}
