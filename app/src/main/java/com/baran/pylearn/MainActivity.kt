package com.baran.pylearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
                    HomeScreen(lessons, progress, doneVersion) { openIndex = it }
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
                if (celebrating) CelebrationOverlay(progress.xp()) { celebrating = false; openIndex = null }
            }
        }
    }
}

/* ================= HOME ================= */

private fun unitColor(unit: String): Color = when (unit) {
    "Temeller" -> Pal.blue
    "Etkileşim" -> Pal.orange
    "Mantık" -> Pal.purple
    "Veri" -> Pal.green
    else -> Pal.blue
}

@Composable
fun HomeScreen(lessons: List<Lesson>, progress: Progress, doneVersion: Int, onOpen: (Int) -> Unit) {
    val ids = lessons.map { it.id }
    val completed = remember(doneVersion) { progress.completedCount(ids) }
    val xp = remember(doneVersion) { progress.xp() }
    val pct = if (lessons.isEmpty()) 0 else completed * 100 / lessons.size
    val firstOpen = remember(doneVersion) {
        lessons.indexOfFirst { !progress.isDone(it.id) }.let { if (it == -1) lessons.size else it }
    }

    val grouped = remember(lessons) {
        lessons.fold(LinkedHashMap<String, MutableList<Lesson>>()) { m, l ->
            m.getOrPut(l.unit) { mutableListOf() }.add(l); m
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // top bar
        Row(
            Modifier.fillMaxWidth().padding(18.dp, 18.dp, 18.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("PyLearn", color = Pal.text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Pal.surfaceHi).padding(12.dp, 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, null, tint = Pal.yellow, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("$xp XP", color = Pal.yellow, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }

        // hero
        HeroCard(completed, lessons.size)

        // progress
        Column(Modifier.padding(20.dp, 6.dp, 20.dp, 14.dp)) {
            Text("İlerlemen", color = Pal.textDim, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Pal.track)) {
                val w by animateFloatAsState(completed / lessons.size.toFloat(), label = "pw")
                Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(7.dp)).background(Pal.green))
            }
            Spacer(Modifier.height(8.dp))
            Text("$completed / ${lessons.size} ders tamamlandı", color = Pal.textDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // units
        grouped.forEachIndexed { gi, (unit, unitLessons) ->
            val isFirstUndone = unitLessons.indexOfFirst { it.id == (lessons.getOrNull(firstOpen)?.id ?: "") }
            UnitModule(
                title = "BÖLÜM ${gi + 1}",
                name = unit,
                color = unitColor(unit),
                lessons = unitLessons,
                progress = progress,
                firstUndoneId = lessons.getOrNull(firstOpen)?.id,
                onOpen = { id -> onOpen(lessons.indexOfFirst { it.id == id }) },
            )
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroCard(done: Int, total: Int) {
    val t = rememberInfiniteTransition(label = "bob")
    val y by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "y")
    Box(
        Modifier.padding(20.dp, 8.dp, 20.dp, 4.dp)
            .fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Pal.blue).padding(20.dp),
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(y = y.dp)) {
            Image(painter = painterResource(R.drawable.mascot_snake), contentDescription = null, modifier = Modifier.size(96.dp))
        }
        Text("Python öğreneceksin.", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(0.72f))
        Spacer(Modifier.height(10.dp))
        Text(
            "Günde birkaç dakika, küçük adımlarla. Öğren, hemen kodla, anında gör.",
            color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Bold,
            lineHeight = 22.sp, modifier = Modifier.fillMaxWidth(0.78f),
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f)).padding(12.dp, 8.dp)) {
            Icon(Icons.Filled.School, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("$total interaktif ders · ${total * 6} pratik", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun UnitModule(
    title: String, name: String, color: Color, lessons: List<Lesson>,
    progress: Progress, firstUndoneId: String?, onOpen: (String) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text(name.uppercase(), color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        lessons.forEach { lesson ->
            val isDone = progress.isDone(lesson.id)
            val isActive = lesson.id == firstUndoneId
            LessonRow(lesson, isDone, isActive, color, onOpen)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, isDone: Boolean, isActive: Boolean, color: Color, onOpen: (String) -> Unit) {
    val face = when {
        isDone -> Pal.surfaceHi
        isActive -> Pal.surfaceHi
        else -> Pal.surface
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(face)
            .border(1.dp, if (isActive) color else Pal.line, RoundedCornerShape(16.dp))
            .clickable { onOpen(lesson.id) }.padding(14.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(if (isActive) color else if (isDone) Pal.green else Pal.track),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isDone -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                isActive -> Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                else -> Icon(Icons.Filled.Lock, null, tint = Pal.textDim, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(lesson.title, color = if (isActive || isDone) Pal.text else Pal.textDim, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("${lesson.steps.size} adım · ${lesson.subtitle}", color = Pal.textDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(if (isDone) "Tamam" else if (isActive) "Başla" else "", color = if (isActive) color else Pal.green, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

/* ================= LESSON STEPPER ================= */

@Composable
fun LessonScreen(lesson: Lesson, onExit: () -> Unit, onComplete: () -> Unit) {
    var stepIndex by remember { mutableStateOf(0) }
    var dir by remember { mutableStateOf(1) }
    val step = lesson.steps[stepIndex]

    val advance: () -> Unit = { if (stepIndex < lesson.steps.lastIndex) { dir = 1; stepIndex++ } }
    val back: () -> Unit = { if (stepIndex > 0) { dir = -1; stepIndex-- } }

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            var sum = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, d -> sum += d },
                onDragEnd = {
                    if (sum > 60) back() else if (sum < -60) {
                        if (step.isTask && stepIndex < lesson.steps.lastIndex) advance()
                        else if (!step.isTask) advance()
                    }
                    sum = 0f
                },
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(16.dp, 18.dp, 16.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) { Icon(Icons.Filled.Close, "Kapat", tint = Pal.textDim) }
                Spacer(Modifier.width(6.dp))
                lesson.goal.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Pal.textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Row(Modifier.width(90.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    lesson.steps.indices.forEach { i ->
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (i <= stepIndex) Pal.green else Pal.track))
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = stepIndex,
                    transitionSpec = {
                        val inX = if (dir > 0) 60 else -60
                        val outX = if (dir > 0) -60 else 60
                        (slideInHorizontally(tween(320), initialOffsetX = { inX }) + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(320), targetOffsetX = { outX }) + fadeOut(tween(220)))
                    },
                    label = "step",
                ) { idx ->
                    val s = lesson.steps[idx]
                    if (s.isTask) TaskStep(s, advance, stepIndex == lesson.steps.lastIndex, onComplete)
                    else LearnStep(s, stepIndex == lesson.steps.lastIndex, advance)
                }
            }
        }
    }
}

@Composable
private fun LearnStep(step: Step, isLast: Boolean, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 8.dp, 20.dp, 20.dp)) {
            Chip("ÖĞREN", Pal.blue)
            Spacer(Modifier.height(16.dp))
            Text(step.text, color = Pal.text, fontSize = 19.sp, lineHeight = 29.sp)
            if (step.code.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                CodeBlock(step.code)
            }
        }
        BottomButton(if (isLast) "Bitir" else "Devam", Pal.blue, Pal.blueEdge, onNext)
    }
}

@Composable
private fun TaskStep(step: Step, onNext: () -> Unit, isLast: Boolean, onComplete: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var showHint by remember { mutableStateOf(false) }
    val passed = result == true
    val run = { result = checkAnswer(code, step.expected) }
    val after = { if (isLast) onComplete() else onNext() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 8.dp, 20.dp, 20.dp)) {
            Chip("ALIŞTIRMA", Pal.yellow, Color(0xFF3A2E00))
            Spacer(Modifier.height(16.dp))
            Text(step.text, color = Pal.text, fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
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
                    Text(step.hint, color = Pal.textDim, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Pal.surface).padding(14.dp))
                }
            }
        }
        when (result) {
            true -> ResultBar("Doğru! Harikasın.", Pal.green, Icons.Filled.CheckCircle) { after() }
            false -> ResultBar("Tam olmadı, tekrar dene.", Pal.red, Icons.Filled.ErrorOutline) { run() }
            null -> {}
        }
        if (passed) BottomButton("Devam", Pal.green, Pal.greenEdge, after)
        else BottomButton("Kontrol Et", Pal.blue, Pal.blueEdge, run)
    }
}

@Composable
private fun ResultBar(msg: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(color.copy(alpha = 0.16f)).clickable(onClick = onClick).padding(20.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(msg, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        Text(if (msg.startsWith("Doğru")) "İleri →" else "Tekrar", color = color, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

/* ================= CELEBRATION ================= */

@Composable
private fun CelebrationOverlay(xp: Int, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Pal.bg.copy(alpha = 0.97f)), contentAlignment = Alignment.Center) {
        Confetti()
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val t = rememberInfiniteTransition(label = "bob")
            val y by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "y")
            Image(painter = painterResource(R.drawable.mascot_snake), contentDescription = null, modifier = Modifier.size(150.dp).offset(y = y.dp))
            Spacer(Modifier.height(20.dp))
            Text("Ders Tamamlandı!", color = Pal.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.clip(RoundedCornerShape(24.dp)).background(Pal.yellow).padding(20.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
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
            ConfettiP(Random.nextFloat(), Random.nextFloat(), Random.nextInt(8, 18).toFloat(), colors[Random.nextInt(colors.size)], Random.nextFloat() * 360f)
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
        Text(highlightPython(text), modifier = Modifier.fillMaxWidth().padding(16.dp), fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 23.sp)
    }
}

@Composable
private fun Dot(color: Color) = Box(Modifier.size(10.dp).clip(CircleShape).background(color))

private val pyHighlight = VisualTransformation { text -> TransformedText(highlightPython(text.text), OffsetMapping.Identity) }

@Composable
private fun CodeInput(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        textStyle = TextStyle(color = Pal.text, fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 23.sp),
        cursorBrush = SolidColor(Pal.blue),
        visualTransformation = pyHighlight,
        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp).clip(RoundedCornerShape(14.dp)).background(Pal.bg)
            .border(1.dp, Pal.line, RoundedCornerShape(14.dp)).padding(16.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("buraya yaz...", color = Pal.textDim, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
            inner()
        },
    )
}
