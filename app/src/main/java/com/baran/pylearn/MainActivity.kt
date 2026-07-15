package com.baran.pylearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val BgTop = Color(0xFF10131C)
private val BgBottom = Color(0xFF1B2030)
private val Accent = Color(0xFF3776AB)
private val Done = Color(0xFF3BB273)
private val Locked = Color(0xFF39414F)
private val CardBg = Color(0xFF232A3B)
private val CodeBg = Color(0xFF12141C)
private val TextMain = Color(0xFFEAEEF5)
private val TextDim = Color(0xFF9AA4B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val lessons = remember { loadLessons(ctx) }
    val progress = remember { Progress(ctx) }
    var doneVersion by remember { mutableStateOf(0) }
    var openIndex by remember { mutableStateOf<Int?>(null) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = BgTop)) {
        Surface(Modifier.fillMaxSize(), color = BgTop) {
            val idx = openIndex
            if (idx == null) {
                MapScreen(lessons, progress, doneVersion) { openIndex = it }
            } else {
                LessonScreen(
                    lesson = lessons[idx],
                    onBack = { openIndex = null },
                    onDone = {
                        progress.markDone(lessons[idx].id)
                        doneVersion++
                        openIndex = null
                    },
                )
            }
        }
    }
}

private fun fracX(i: Int): Float = (0.5f + 0.30f * sin(i * 1.15f)).coerceIn(0.18f, 0.82f)

@Composable
fun MapScreen(lessons: List<Lesson>, progress: Progress, doneVersion: Int, onOpen: (Int) -> Unit) {
    val ids = lessons.map { it.id }
    val completed = remember(doneVersion) { progress.completedCount(ids) }
    val firstLocked = remember(doneVersion) { lessons.indexOfFirst { !progress.isDone(it.id) } }
    val itemH = 132.dp
    val node = 66.dp

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 24.dp, 20.dp, 8.dp)) {
            Text("PyLearn", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Python'u adım adım öğren", color = TextDim, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (lessons.isEmpty()) 0f else completed / lessons.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Done,
                trackColor = Locked,
            )
            Spacer(Modifier.height(4.dp))
            Text("$completed / ${lessons.size} tamamlandı", color = TextDim, fontSize = 12.sp)
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        ) {
            val w = maxWidth
            Box(Modifier.fillMaxWidth().height(itemH * lessons.size + 24.dp)) {
                val itemPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemH.toPx() }
                Canvas(Modifier.matchParentSize()) {
                    val topPad = itemPx / 2
                    val path = Path()
                    lessons.indices.forEach { i ->
                        val x = fracX(i) * size.width
                        val y = topPad + i * itemPx
                        if (i == 0) path.moveTo(x, y) else {
                            val px = fracX(i - 1) * size.width
                            val py = topPad + (i - 1) * itemPx
                            val my = (py + y) / 2
                            path.cubicTo(px, my, x, my, x, y)
                        }
                    }
                    drawPath(path, color = Locked, style = Stroke(width = 10f))
                }

                lessons.forEachIndexed { i, lesson ->
                    val isDone = progress.isDone(lesson.id)
                    val isCurrent = i == firstLocked
                    val unlocked = isDone || isCurrent
                    val cx = w * fracX(i)
                    NodeItem(
                        index = i,
                        lesson = lesson,
                        isDone = isDone,
                        isCurrent = isCurrent,
                        unlocked = unlocked,
                        modifier = Modifier
                            .width(node + 60.dp)
                            .offset(x = cx - (node + 60.dp) / 2, y = itemH * i + (itemH - node) / 2 - 10.dp),
                        nodeSize = node,
                        onClick = { if (unlocked) onOpen(i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeItem(
    index: Int,
    lesson: Lesson,
    isDone: Boolean,
    isCurrent: Boolean,
    unlocked: Boolean,
    modifier: Modifier,
    nodeSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val color = when {
        isDone -> Done
        isCurrent -> Accent
        else -> Locked
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(color)
                .then(if (isCurrent) Modifier.border(3.dp, TextMain, CircleShape) else Modifier)
                .clickable(enabled = unlocked, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isDone -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(30.dp))
                !unlocked -> Icon(Icons.Filled.Lock, null, tint = TextDim, modifier = Modifier.size(26.dp))
                else -> Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            lesson.title,
            color = if (unlocked) TextMain else TextDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun LessonScreen(lesson: Lesson, onBack: () -> Unit, onDone: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var showHint by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Geri", tint = TextMain)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(lesson.title, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(lesson.subtitle, color = Accent, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionCard {
            Text(lesson.explanation, color = TextMain, fontSize = 16.sp, lineHeight = 24.sp)
        }
        Spacer(Modifier.height(14.dp))

        Text("Örnek", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        CodeBlock(lesson.example)
        Spacer(Modifier.height(18.dp))

        SectionCard {
            Text("Alıştırma", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(lesson.exerciseText, color = TextMain, fontSize = 16.sp, lineHeight = 24.sp)
        }
        Spacer(Modifier.height(12.dp))

        Text("Kodunu yaz:", color = TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        CodeInput(code) { code = it; result = null }
        Spacer(Modifier.height(10.dp))

        TextButton(onClick = { showHint = !showHint }) {
            Text(if (showHint) "İpucunu gizle" else "İpucu göster", color = Accent)
        }
        if (showHint) {
            Text(lesson.hint, color = TextDim, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(6.dp))
        }

        when (result) {
            true -> Text("Doğru! 🎉", color = Done, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            false -> Text("Tam olmadı, tekrar dene.", color = Color(0xFFE06C75), fontSize = 15.sp)
            null -> {}
        }
        Spacer(Modifier.height(12.dp))

        if (result == true) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Done),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Devam", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        } else {
            Button(
                onClick = { result = checkAnswer(code, lesson.expected) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Kontrol Et", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).padding(16.dp),
        content = content,
    )
}

@Composable
private fun CodeBlock(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CodeBg).padding(16.dp),
        color = Color(0xFF9CDCFE),
        fontFamily = FontFamily.Monospace,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
}

@Composable
private fun CodeInput(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = TextMain, fontFamily = FontFamily.Monospace, fontSize = 15.sp),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CodeBg)
            .border(1.dp, Locked, RoundedCornerShape(12.dp))
            .padding(16.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text("buraya yaz...", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
            }
            inner()
        },
    )
}
