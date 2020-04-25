package io.qameta.allure.jira;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public enum StatusColor {
    RED("#FF0000"),
    GREEN("#008000"),
    YELLOW("#FFFF00"),
    GRAY("#808080");

    private final String value;
}
