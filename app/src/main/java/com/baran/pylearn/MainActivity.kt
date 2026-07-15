package com.baran.pylearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val BgTop = Color(0xFF0E1118)
private val BgBottom = Color(0xFF1A2133)
private val Accent = Color(0xFF3E8FD6)
private val AccentDark = Color(0xFF2B6CB0)
private val Done = Color(0xFF3BB273)
private val Locked = Color(0xFF2E3646)
private val LockedRing = Color(0xFF3A4456)
private val CardBg = Color(0xFF222B3D)
private val CodeBg = Color(0xFF10131C)
private val TextMain = Color(0xFFEDF1F8)
private val TextDim = Color(0xFF93A0B8)
private val Yellow = Color(0xFFFFD43B)
private val ErrRed = Color(0xFFE06C75)

private val screenBg = Brush.verticalGradient(listOf(BgTop, BgBottom))

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
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(screenBg)) {
                val idx = openIndex
                if (idx == null) {
                    MapScreen(lessons, progress, doneVersion) { openIndex = it }
                } else {
                    LessonScreen(
                        lesson = lessons[idx],
                        onExit = { openIndex = null },
                        onComplete = {
                            progress.markDone(lessons[idx].id)
                            doneVersion++
                            openIndex = null
                        },
                    )
                }
            }
        }
    }
}

/* ---------------- MAP ---------------- */

private fun fracX(i: Int): Float = (0.5f + 0.32f * sin(i * 1.15f)).coerceIn(0.16f, 0.84f)

@Composable
fun MapScreen(lessons: List<Lesson>, progress: Progress, doneVersion: Int, onOpen: (Int) -> Unit) {
    val ids = lessons.map { it.id }
    val completed = remember(doneVersion) { progress.completedCount(ids) }
    val firstOpen = remember(doneVersion) {
        lessons.indexOfFirst { !progress.isDone(it.id) }.let { if (it == -1) lessons.size else it }
    }
    val pct = if (lessons.isEmpty()) 0 else (completed * 100 / lessons.size)

    val itemH = 150.dp
    val node = 74.dp
    val density = LocalDensity.current
    val itemPx = with(density) { itemH.toPx() }
    val topPadPx = with(density) { 96.dp.toPx() }

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 22.dp, 20.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Accent, AccentDark))),
                contentAlignment = Alignment.Center,
            ) { Text("Py", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PyLearn", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Python'u adım adım öğren", color = TextDim, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%$pct", color = Yellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("$completed/${lessons.size}", color = TextDim, fontSize = 12.sp)
            }
        }
        LinearProgressIndicator(
            progress = { if (lessons.isEmpty()) 0f else completed / lessons.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Done,
            trackColor = Locked,
        )
        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        ) {
            val w = maxWidth
            val totalH = itemH * lessons.size + 140.dp
            Box(Modifier.fillMaxWidth().height(totalH)) {
                // winding path
                Canvas(Modifier.matchParentSize()) {
                    val path = Path()
                    lessons.indices.forEach { i ->
                        val x = fracX(i) * size.width
                        val y = topPadPx + i * itemPx
                        if (i == 0) path.moveTo(x, y) else {
                            val px = fracX(i - 1) * size.width
                            val py = topPadPx + (i - 1) * itemPx
                            val my = (py + y) / 2
                            path.cubicTo(px, my, x, my, x, y)
                        }
                    }
                    drawPath(
                        path,
                        color = LockedRing,
                        style = Stroke(
                            width = 14f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 34f)),
                        ),
                    )
                }

                // start marker
                Box(
                    Modifier.offset(x = w * fracX(0) - 30.dp, y = 34.dp).width(60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("BAŞLA", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                var lastUnit = ""
                lessons.forEachIndexed { i, lesson ->
                    val isDone = progress.isDone(lesson.id)
                    val isCurrent = i == firstOpen
                    val unlocked = isDone || isCurrent
                    val cx = w * fracX(i)
                    val nodeTop = with(density) { topPadPx.toDp() } + itemH * i - node / 2

                    if (lesson.unit != lastUnit) {
                        lastUnit = lesson.unit
                        UnitBanner(
                            lesson.unit,
                            Modifier.offset(y = nodeTop - 48.dp).fillMaxWidth().padding(horizontal = 20.dp),
                        )
                    }

                    NodeItem(
                        index = i,
                        lesson = lesson,
                        isDone = isDone,
                        isCurrent = isCurrent,
                        unlocked = unlocked,
                        nodeSize = node,
                        modifier = Modifier.width(node + 90.dp)
                            .offset(x = cx - (node + 90.dp) / 2, y = nodeTop),
                        onClick = { if (unlocked) onOpen(i) },
                    )
                }

                // trophy
                Box(
                    Modifier.offset(
                        x = w * fracX(lessons.size - 1) - 34.dp,
                        y = with(density) { topPadPx.toDp() } + itemH * (lessons.size - 1) + 44.dp,
                    ).size(68.dp).shadow(10.dp, CircleShape)
                        .clip(CircleShape).background(if (completed == lessons.size) Yellow else Locked),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents, null,
                        tint = if (completed == lessons.size) Color(0xFF7A5A00) else TextDim,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitBanner(name: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(LockedRing))
        Text(
            name.uppercase(),
            color = TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(LockedRing))
    }
}

@Composable
private fun NodeItem(
    index: Int,
    lesson: Lesson,
    isDone: Boolean,
    isCurrent: Boolean,
    unlocked: Boolean,
    nodeSize: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val fill = when {
        isDone -> Brush.linearGradient(listOf(Done, Color(0xFF2E9C63)))
        isCurrent -> Brush.linearGradient(listOf(Accent, AccentDark))
        else -> Brush.linearGradient(listOf(Locked, Locked))
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            if (isCurrent) {
                val t = rememberInfiniteTransition(label = "pulse")
                val s by t.animateFloat(
                    1f, 1.35f,
                    infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "s",
                )
                val a by t.animateFloat(
                    0.5f, 0f,
                    infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "a",
                )
                Box(
                    Modifier.size(nodeSize).scale(s).clip(CircleShape)
                        .background(Accent.copy(alpha = a)),
                )
            }
            Box(
                Modifier.size(nodeSize)
                    .shadow(if (unlocked) 8.dp else 0.dp, CircleShape)
                    .clip(CircleShape).background(fill)
                    .then(if (isCurrent) Modifier.border(3.dp, TextMain, CircleShape) else Modifier)
                    .clickable(enabled = unlocked, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isDone -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    !unlocked -> Icon(Icons.Filled.Lock, null, tint = TextDim, modifier = Modifier.size(28.dp))
                    else -> Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            lesson.title,
            color = if (unlocked) TextMain else TextDim,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${lesson.steps.size} adım · ${lesson.subtitle}",
            color = TextDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/* ---------------- LESSON STEPPER ---------------- */

@Composable
fun LessonScreen(lesson: Lesson, onExit: () -> Unit, onComplete: () -> Unit) {
    var stepIndex by remember { mutableStateOf(0) }
    val step = lesson.steps[stepIndex]

    Column(Modifier.fillMaxSize()) {
        // top bar: exit + segmented progress
        Row(
            Modifier.fillMaxWidth().padding(16.dp, 18.dp, 16.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) { Icon(Icons.Filled.Close, "Kapat", tint = TextDim) }
            Spacer(Modifier.width(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                lesson.steps.indices.forEach { i ->
                    Box(
                        Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (i <= stepIndex) Accent else Locked),
                    )
                }
            }
        }

        Column(Modifier.weight(1f)) {
            key(stepIndex) {
                if (step.isTask) {
                    TaskStep(
                        lesson = lesson,
                        step = step,
                        onNext = {
                            if (stepIndex == lesson.steps.lastIndex) onComplete() else stepIndex++
                        },
                    )
                } else {
                    LearnStep(
                        lesson = lesson,
                        step = step,
                        isLast = stepIndex == lesson.steps.lastIndex,
                        onNext = {
                            if (stepIndex == lesson.steps.lastIndex) onComplete() else stepIndex++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LearnStep(lesson: Lesson, step: Step, isLast: Boolean, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 4.dp, 20.dp, 20.dp),
        ) {
            Chip("ÖĞREN", Accent)
            Spacer(Modifier.height(14.dp))
            Text(lesson.title, color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text(step.text, color = TextMain, fontSize = 17.sp, lineHeight = 26.sp)
            if (step.code.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text("Örnek", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                CodeBlock(step.code)
            }
        }
        BottomButton(if (isLast) "Bitir" else "Devam", Accent, onNext)
    }
}

@Composable
private fun TaskStep(lesson: Lesson, step: Step, onNext: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var showHint by remember { mutableStateOf(false) }
    val passed = result == true

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 4.dp, 20.dp, 20.dp),
        ) {
            Chip("ALIŞTIRMA", Yellow, Color(0xFF3A2E00))
            Spacer(Modifier.height(14.dp))
            Text(step.text, color = TextMain, fontSize = 18.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            Text("Kodunu yaz:", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            CodeInput(code, enabled = !passed) { code = it; result = null }
            Spacer(Modifier.height(8.dp))
            if (!passed) {
                TextButton(onClick = { showHint = !showHint }) {
                    Icon(Icons.Filled.Lightbulb, null, tint = Yellow, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (showHint) "İpucunu gizle" else "İpucu", color = Yellow)
                }
                AnimatedVisibility(showHint) {
                    Text(
                        step.hint, color = TextDim, fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(CardBg).padding(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (result) {
                true -> Feedback("Doğru! Harikasın.", Done, Icons.Filled.CheckCircle)
                false -> Feedback("Tam olmadı, tekrar dene.", ErrRed, Icons.Filled.ErrorOutline)
                null -> {}
            }
        }
        if (passed) {
            BottomButton("Devam", Done, onNext)
        } else {
            BottomButton("Kontrol Et", Accent) { result = checkAnswer(code, step.expected) }
        }
    }
}

@Composable
private fun Feedback(msg: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(msg, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color = Color.White) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun BottomButton(label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(20.dp)) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            shape = RoundedCornerShape(15.dp),
        ) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White) }
    }
}

@Composable
private fun CodeBlock(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CodeBg).padding(16.dp),
        color = Color(0xFF9CDCFE),
        fontFamily = FontFamily.Monospace,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
}

@Composable
private fun CodeInput(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        textStyle = TextStyle(color = TextMain, fontFamily = FontFamily.Monospace, fontSize = 15.sp),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
            .clip(RoundedCornerShape(12.dp)).background(CodeBg)
            .border(1.dp, LockedRing, RoundedCornerShape(12.dp)).padding(16.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text("buraya yaz...", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
            }
            inner()
        },
    )
}
