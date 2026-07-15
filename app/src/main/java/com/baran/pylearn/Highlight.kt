package com.baran.pylearn

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val keywords = setOf(
    "def", "if", "else", "elif", "for", "while", "in", "return",
    "import", "from", "and", "or", "not", "True", "False", "None", "class",
)
private val builtins = setOf("print", "input", "range", "len", "int", "str", "float", "list")

private val kwColor = Color(0xFF9D7CFF)
private val builtinColor = Color(0xFF4B8BBE)
private val strColor = Color(0xFF58CC02)
private val numColor = Color(0xFFFF9F45)
private val commentColor = Color(0xFF6B7385)
private val baseColor = Color(0xFFD8DEEA)

private val token = Regex(
    "(#[^\\n]*)" +
        "|(\"[^\"]*\"|'[^']*')" +
        "|(\\b\\d+\\.?\\d*\\b)" +
        "|([A-Za-z_][A-Za-z0-9_]*)"
)

fun highlightPython(code: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in token.findAll(code)) {
        if (m.range.first > last) {
            withStyle(SpanStyle(color = baseColor)) { append(code.substring(last, m.range.first)) }
        }
        val text = m.value
        val color = when {
            m.groups[1] != null -> commentColor
            m.groups[2] != null -> strColor
            m.groups[3] != null -> numColor
            text in keywords -> kwColor
            text in builtins -> builtinColor
            else -> baseColor
        }
        withStyle(SpanStyle(color = color)) { append(text) }
        last = m.range.last + 1
    }
    if (last < code.length) {
        withStyle(SpanStyle(color = baseColor)) { append(code.substring(last)) }
    }
}
