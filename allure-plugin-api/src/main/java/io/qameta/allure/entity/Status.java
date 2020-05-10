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
package io.qameta.allure.entity;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * @author charlie (Dmitry Baev).
 */
public enum Status implements Serializable {

    FAILED("failed", "f90602"),
    BROKEN("broken", "febe0d"),
    PASSED("passed", "78b63c"),
    SKIPPED("skipped", "888888"),
    UNKNOWN("unknown", "bf34a6");

    private static final long serialVersionUID = 1L;

    private final String value;
    private final String color;

    Status(final String v, String color) {
        value = v;
        this.color = color;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String color() { return this.color;}

}
