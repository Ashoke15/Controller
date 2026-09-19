package com.diyproject.controller.devtools

/** One collapsible-free section of an in-app help sheet — a heading plus
 *  plain-language explanations of what each control does. Content only;
 *  no UI here so it can be unit-tested and reused across dev-tool screens. */
data class HelpSection(
    val title: String,
    val items: List<HelpItem>
)

data class HelpItem(
    val label: String,
    val description: String
)