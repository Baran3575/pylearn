package com.baran.pylearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

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
    var celebrating by remember { mutableStateOf(false) }

    PyLearnTheme {
        Surface(Modifier.fillMaxSize(), color = Pal.bg) {
            Box(Modifier.fillMaxSize()) {
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
                            celebrating = true
                        },
                    )
                }
                if (celebrating) {
                    CelebrationOverlay(xp = progress.xp()) {
                        celebrating = false
                        openIndex = null
                    }
                }
            }
        }
    }
}

/* ================= MAP ================= */

private fun fracX(i: Int): Float = (0.5f + 0.30f * sin(i * 1.15f)).coerceIn(0.18f, 0.82f)

private fun unitColor(unit: String): Color = when (unit) {
    "Temeller" -> Pal.blue
    "Etkileşim" -> Pal.orange
    "Mantık" -> Pal.purple
    "Veri" -> Pal.green
    else -> Pal.blue
}

@Composable
fun MapScreen(lessons: List<Lesson>, progress: Progress, doneVersion: Int, onOpen: (Int) -> Unit) {
    val ids = lessons.map { it.id }
    val completed = remember(doneVersion) { progress.completedCount(ids) }
    val xp = remember(doneVersion) { progress.xp() }
    val firstOpen = remember(doneVersion) {
        lessons.indexOfFirst { !progress.isDone(it.id) }.let { if (it == -1) lessons.size else it }
    }
    val pct = if (lessons.isEmpty()) 0 else completed * 100 / lessons.size
    val allDone = completed == lessons.size

    val itemH = 156.dp
    val node = 78.dp
    val density = LocalDensity.current
    val itemPx = with(density) { itemH.toPx() }
    val topPad = 104.dp
    val topPadPx = with(density) { topPad.toPx() }

    Column(Modifier.fillMaxSize()) {
        MapHeader(xp, pct, completed, lessons.size)

        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        ) {
            val w = maxWidth
            Box(Modifier.fillMaxWidth().height(topPad + itemH * lessons.size + 100.dp)) {
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
                    drawPath(path, Pal.track, style = Stroke(width = 20f, cap = StrokeCap.Round))
                    drawPath(
                        path, Pal.line,
                        style = Stroke(
                            width = 20f, cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 40f)),
                        ),
                    )
                }

                var lastUnit = ""
                lessons.forEachIndexed { i, lesson ->
                    val isDone = progress.isDone(lesson.id)
                    val isCurrent = i == firstOpen
                    val unlocked = isDone || isCurrent
                    val cx = w * fracX(i)
                    val nodeTop = topPad + itemH * i - node / 2

                    if (lesson.unit != lastUnit) {
                        lastUnit = lesson.unit
                        UnitBanner(
                            lesson.unit, unitColor(lesson.unit),
                            Modifier.offset(y = nodeTop - 64.dp).fillMaxWidth().padding(horizontal = 20.dp),
                        )
                    }

                    MapNode(
                        index = i, lesson = lesson,
                        isDone = isDone, isCurrent = isCurrent, unlocked = unlocked,
                        nodeSize = node,
                        modifier = Modifier.width(node + 96.dp)
                            .offset(x = cx - (node + 96.dp) / 2, y = nodeTop),
                        onClick = { if (unlocked) onOpen(i) },
                    )
                }

                // finish: mascot + trophy
                Column(
                    Modifier.offset(y = topPad + itemH * lessons.size - 30.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape)
                            .background(if (allDone) Pal.yellow else Pal.locked),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents, null,
                            tint = if (allDone) Color(0xFF7A5A00) else Pal.textDim,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (allDone) "Tebrikler, bitirdin!" else "Son durak",
                        color = if (allDone) Pal.yellow else Pal.textDim,
                        fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapHeader(xp: Int, pct: Int, done: Int, total: Int) {
    Column(Modifier.background(Pal.bgSoft)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 22.dp, 20.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Pal.blue),
                contentAlignment = Alignment.Center,
            ) { Text("Py", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PyLearn", color = Pal.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Python'u adım adım öğren", color = Pal.textDim, fontSize = 12.sp)
            }
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Pal.surfaceHi)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, null, tint = Pal.yellow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("$xp XP", color = Pal.yellow, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 0.dp, 20.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Pal.track),
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(if (total == 0) 0f else done / total.toFloat())
                        .clip(RoundedCornerShape(6.dp)).background(Pal.green),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("%$pct", color = Pal.text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun UnitBanner(name: String, color: Color, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(color).padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Bookmark, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "BÖLÜM · ${name.uppercase()}",
            color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp,
        )
    }
}

@Composable
private fun MapNode(
    index: Int, lesson: Lesson,
    isDone: Boolean, isCurrent: Boolean, unlocked: Boolean,
    nodeSize: Dp, modifier: Modifier, onClick: () -> Unit,
) {
    val face = when {
        isDone -> Pal.green
        isCurrent -> Pal.blue
        else -> Pal.locked
    }
    val edge = when {
        isDone -> Pal.greenEdge
        isCurrent -> Pal.blueEdge
        else -> Pal.lockedEdge
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (isCurrent) {
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Pal.blue)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) { Text("BAŞLA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            Spacer(Modifier.height(6.dp))
        }
        Box(contentAlignment = Alignment.Center) {
            if (isCurrent) PulseRing(nodeSize)
            Node3D(nodeSize, face, edge, unlocked, onClick) {
                when {
                    isDone -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    !unlocked -> Icon(Icons.Filled.Lock, null, tint = Pal.textDim, modifier = Modifier.size(28.dp))
                    else -> Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            lesson.title,
            color = if (unlocked) Pal.text else Pal.textDim,
            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
        )
        Text("${lesson.steps.size} adım", color = Pal.textDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PulseRing(size: Dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, 1.4f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "s")
    val a by t.animateFloat(0.45f, 0f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(size).scale(s).clip(CircleShape).background(Pal.blue.copy(alpha = a)))
}

@Composable
private fun Node3D(size: Dp, face: Color, edge: Color, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val bezel = 7.dp
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed) bezel else 0.dp, label = "drop")
    Box(Modifier.size(width = size, height = size + bezel)) {
        Box(Modifier.size(size).align(Alignment.BottomCenter).clip(CircleShape).background(edge))
        Box(
            Modifier.size(size).offset(y = drop).clip(CircleShape).background(face)
                .clickable(interaction, indication = null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/* ================= LESSON STEPPER ================= */

@Composable
fun LessonScreen(lesson: Lesson, onExit: () -> Unit, onComplete: () -> Unit) {
    var stepIndex by remember { mutableStateOf(0) }
    val step = lesson.steps[stepIndex]
    val advance: () -> Unit = { if (stepIndex == lesson.steps.lastIndex) onComplete() else stepIndex++ }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp, 20.dp, 16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) { Icon(Icons.Filled.Close, "Kapat", tint = Pal.textDim) }
            Spacer(Modifier.width(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                lesson.steps.indices.forEach { i ->
                    Box(
                        Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp))
                            .background(if (i <= stepIndex) Pal.green else Pal.track),
                    )
                }
            }
        }

        Box(Modifier.weight(1f)) {
            key(stepIndex) {
                if (step.isTask) TaskStep(step, advance) else LearnStep(lesson, step, stepIndex == lesson.steps.lastIndex, advance)
            }
        }
    }
}

@Composable
private fun LearnStep(lesson: Lesson, step: Step, isLast: Boolean, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 4.dp, 20.dp, 20.dp)) {
            Chip("ÖĞREN", Pal.blue)
            Spacer(Modifier.height(16.dp))
            Text(lesson.title, color = Pal.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(lesson.subtitle, color = Pal.blue, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(step.text, color = Pal.text, fontSize = 18.sp, lineHeight = 28.sp)
            if (step.code.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                CodeBlock(step.code)
            }
        }
        BottomButton(if (isLast) "Bitir" else "Devam", Pal.blue, Pal.blueEdge, onNext)
    }
}

@Composable
private fun TaskStep(step: Step, onNext: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var showHint by remember { mutableStateOf(false) }
    val passed = result == true

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 4.dp, 20.dp, 20.dp)) {
            Chip("ALIŞTIRMA", Pal.yellow, Color(0xFF3A2E00))
            Spacer(Modifier.height(16.dp))
            Text(step.text, color = Pal.text, fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text("KODUNU YAZ", color = Pal.textDim, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            CodeInput(code, enabled = !passed) { code = it; result = null }
            Spacer(Modifier.height(8.dp))
            if (!passed) {
                TextButton(onClick = { showHint = !showHint }) {
                    Icon(Icons.Filled.Lightbulb, null, tint = Pal.yellow, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (showHint) "İpucunu gizle" else "İpucu", color = Pal.yellow, fontWeight = FontWeight.Bold)
                }
                AnimatedVisibility(showHint) {
                    Text(
                        step.hint, color = Pal.textDim, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Pal.surface).padding(14.dp),
                    )
                }
            }
        }
        when (result) {
            true -> ResultBar("Doğru! Harikasın.", Pal.green, Icons.Filled.CheckCircle)
            false -> ResultBar("Tam olmadı, tekrar dene.", Pal.red, Icons.Filled.ErrorOutline)
            null -> {}
        }
        if (passed) BottomButton("Devam", Pal.green, Pal.greenEdge, onNext)
        else BottomButton("Kontrol Et", Pal.blue, Pal.blueEdge) { result = checkAnswer(code, step.expected) }
    }
}

@Composable
private fun ResultBar(msg: String, color: Color, icon: ImageVector) {
    Row(
        Modifier.fillMaxWidth().background(color.copy(alpha = 0.16f)).padding(20.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(msg, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/* ================= CELEBRATION ================= */

@Composable
private fun CelebrationOverlay(xp: Int, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Pal.bg.copy(alpha = 0.97f)), contentAlignment = Alignment.Center) {
        Confetti()
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val t = rememberInfiniteTransition(label = "bob")
            val y by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "y")
            Image(
                painter = painterResource(R.drawable.mascot_snake),
                contentDescription = null,
                modifier = Modifier.size(150.dp).offset(y = y.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text("Ders Tamamlandı!", color = Pal.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.clip(RoundedCornerShape(24.dp)).background(Pal.yellow).padding(20.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, null, tint = Color(0xFF3A2E00), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("+${Progress.XP_PER_LESSON} XP", color = Color(0xFF3A2E00), fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("Toplam $xp XP", color = Pal.textDim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            BottomButton("Devam Et", Pal.green, Pal.greenEdge, onDismiss)
        }
    }
}

@Composable
private fun Confetti() {
    val colors = listOf(Pal.yellow, Pal.green, Pal.blue, Pal.purple, Pal.orange, Pal.red)
    val parts = remember {
        List(40) {
            ConfettiP(
                x = Random.nextFloat(),
                delay = Random.nextFloat(),
                size = Random.nextInt(8, 18).toFloat(),
                color = colors[Random.nextInt(colors.size)],
                spin = Random.nextFloat() * 360f,
            )
        }
    }
    val t = rememberInfiniteTransition(label = "confetti")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "p")
    Canvas(Modifier.fillMaxSize()) {
        parts.forEach { c ->
            val prog = ((p + c.delay) % 1f)
            val y = prog * (size.height + 60f) - 30f
            val x = c.x * size.width + sin((prog * 6f) + c.delay * 6f) * 24f
            rotate(c.spin + prog * 720f, pivot = Offset(x, y)) {
                drawRect(c.color, topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(c.size, c.size * 0.5f))
            }
        }
    }
}

private data class ConfettiP(val x: Float, val delay: Float, val size: Float, val color: Color, val spin: Float)

/* ================= SHARED ================= */

@Composable
private fun Chip(text: String, bg: Color, fg: Color = Color.White) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(bg).padding(horizontal = 13.dp, vertical = 6.dp)) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BottomButton(label: String, face: Color, edge: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "btn")
    Box(Modifier.fillMaxWidth().padding(20.dp)) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(16.dp)).background(edge))
            Box(
                Modifier.fillMaxWidth().height(52.dp).offset(y = drop).clip(RoundedCornerShape(16.dp)).background(face)
                    .clickable(interaction, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White) }
        }
    }
}

@Composable
private fun CodeBlock(text: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Pal.bg).border(1.dp, Pal.line, RoundedCornerShape(14.dp))) {
        Row(Modifier.fillMaxWidth().background(Pal.surface).padding(14.dp, 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Dot(Pal.red); Spacer(Modifier.width(6.dp)); Dot(Pal.yellow); Spacer(Modifier.width(6.dp)); Dot(Pal.green)
            Spacer(Modifier.width(10.dp))
            Text("python", color = Pal.textDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            highlightPython(text),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 23.sp,
        )
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

private val pyHighlight = VisualTransformation { text ->
    TransformedText(highlightPython(text.text), OffsetMapping.Identity)
}

@Composable
private fun CodeInput(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        textStyle = TextStyle(color = Pal.text, fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 23.sp),
        cursorBrush = SolidColor(Pal.blue),
        visualTransformation = pyHighlight,
        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp)
            .clip(RoundedCornerShape(14.dp)).background(Pal.bg)
            .border(1.dp, Pal.line, RoundedCornerShape(14.dp)).padding(16.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("buraya yaz...", color = Pal.textDim, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
            inner()
        },
    )
}
