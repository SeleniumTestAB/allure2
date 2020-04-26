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

import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public final class TestData {

    private TestData(){
    }

    public static JiraService mockJiraService() {
        final JiraService service = mock(JiraService.class);
        when(service.createJiraLaunch(any(JiraLaunch.class), anyList())).thenAnswer(i -> {
            List<JiraLaunchResult> jiraLaunchResults = spy(new ArrayList<>());
            jiraLaunchResults.add(new JiraLaunchResult().setExternalId("ALLURE-1")
                    .setIssueKey("ALLURE-1")
                    .setStatus("ok"));
            return jiraLaunchResults;
        });
        when(service.createTestResult(any(JiraTestResult.class))).thenAnswer(i -> {
            final JiraTestResult testResult = i.getArgument(0);
            testResult.setExternalId(String.valueOf(RandomUtils.nextInt()));
            return testResult;
        });
        return service;
    }

    public static TestResult createTestResult(final Status status) {
        return new TestResult()
                .setUid(RandomStringUtils.random(10))
                .setName(RandomStringUtils.random(10))
                .setStatus(status);
    }

}
