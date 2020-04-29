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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
/**
 * Color of given Status.
 */
@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public enum StatusColor {
    //f90602, 78b63c, 888888
    RED("f90602"),
    GREEN("78b63c"),
    YELLOW("febe0d"),
    GRAY("888888");

    private final String value;
}
