package com.diyproject.controller.devtools.common

enum class LineEnding(val label: String, val suffix: String) {
    NONE("None", ""),
    NL("\\n", "\n"),
    CR("\\r", "\r"),
    CRLF("\\r\\n", "\r\n")
}