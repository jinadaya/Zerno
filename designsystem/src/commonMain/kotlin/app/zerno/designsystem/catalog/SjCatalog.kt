package app.zerno.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.DayMark
import app.zerno.designsystem.component.SjAchievementSeal
import app.zerno.designsystem.component.SjAchievementSet
import app.zerno.designsystem.component.SjBigStat
import app.zerno.designsystem.component.SjBottomNav
import app.zerno.designsystem.component.SjChipGroup
import app.zerno.designsystem.component.SjCover
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjNavItem
import app.zerno.designsystem.component.SjOutlineButton
import app.zerno.designsystem.component.SjPlanKind
import app.zerno.designsystem.component.SjPlanList
import app.zerno.designsystem.component.SjPlanRow
import app.zerno.designsystem.component.SjPrimaryButton
import app.zerno.designsystem.component.SjProgressLine
import app.zerno.designsystem.component.SjReaderText
import app.zerno.designsystem.component.SjReflectionField
import app.zerno.designsystem.component.SjStreakWeek
import app.zerno.designsystem.component.SjStreakWeekLabels
import app.zerno.designsystem.component.SjSwitchRow
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.component.SjTextField
import app.zerno.designsystem.component.SjTodayCard
import app.zerno.designsystem.component.SjTodayProgress
import app.zerno.designsystem.component.SjVerse
import app.zerno.designsystem.component.SjVerseAction
import app.zerno.designsystem.component.SjVerseActionBar
import app.zerno.designsystem.component.SjWelcomeBack
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.designsystem.tokens.SjColors
import app.zerno.designsystem.type.SjTypeSample

/**
 * Every design system component and mascot on one scrolling page, with a switch for light and dark. For the debug
 * menu (PR-10) and for checking a token change on a device. Sample strings are English and Russian on purpose,
 * to catch Cyrillic and length problems; they are not app copy.
 */
@Composable
fun SjCatalog(modifier: Modifier = Modifier) {
    val systemDark = SjTheme.colors == SjColors.Dark
    var dark by remember { mutableStateOf(systemDark) }
    SjTheme(darkTheme = dark, reducedMotion = SjTheme.motion.reduced) {
        val colors = SjTheme.colors
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.paper)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
            verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space8),
        ) {
            BasicText("Catalog", style = SjTheme.type.headline.copy(color = colors.ink))
            SjSwitchRow("Dark theme", dark, { dark = it })
            Section("Type") { SjTypeSample() }
            Section("Actions") { Actions() }
            Section("Inputs") { Inputs() }
            Section("Progress") { Progress() }
            Section("Cards") { Cards() }
            Section("Reading") { Reading() }
            Section("Navigation") { Navigation() }
            Section("Icons") { Icons() }
            Section("Mascots") { Mascots() }
            Section("Cover") { SjCover("Scripture Journey", "Read a little every day. Collect the journey.") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
        SjTag(title)
        content()
    }
}

@Composable
private fun Actions() {
    var minutes by remember { mutableIntStateOf(1) }
    SjPrimaryButton("Start reading", onClick = {})
    SjOutlineButton("Change plan", onClick = {})
    SjLinkButton("Skip for now", onClick = {})
    SjPrimaryButton("Finish", onClick = {}, enabled = false)
    Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
        SjTag("Day 4 / 21")
        SjTag("7 мин")
        SjTag("New plan", solid = true)
    }
    SjChipGroup(listOf("5 min", "10 min", "15 min", "20+"), minutes, { minutes = it }, label = "Daily reading time")
    StickerSurface(SjTheme.colors.butter) {
        SjPrimaryButton("Продолжить", onClick = {}, modifier = Modifier.fillMaxWidth())
        SjOutlineButton("Сменить план", onClick = {})
        SjChipGroup(listOf("5", "10", "15", "20+"), minutes, { minutes = it }, label = "Время чтения")
    }
}

@Composable
private fun Inputs() {
    var name by remember { mutableStateOf("") }
    var reflection by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf(true) }
    SjTextField(name, { name = it }, label = "Your name", placeholder = "What should we call you?")
    SjTextField("25:00", {}, label = "Reminder time", error = "Enter a time between 00:00 and 23:59.")
    SjReflectionField(reflection, {
        reflection = it
    }, label = "Размышление", placeholder = "Что вам запомнилось сегодня?")
    SjSwitchRow("Daily reminder", reminder, { reminder = it })
}

@Composable
private fun Progress() {
    var progress by remember { mutableFloatStateOf(0.4f) }
    SjBigStat("38", "of John read", unit = "%")
    SjProgressLine(
        progress,
        "Chapter progress",
        startLabel = "Verse 22 / 54",
        endLabel = "${(progress * 100).toInt()}%",
    )
    SjOutlineButton("Advance", onClick = { progress = if (progress >= 1f) 0.1f else progress + 0.2f })
    SjStreakWeek(
        days = listOf(
            DayMark.Read,
            DayMark.Done,
            DayMark.Empty,
            DayMark.Read,
            DayMark.Read,
            DayMark.Empty,
            DayMark.Empty,
        ),
        dayLetters = listOf("M", "T", "W", "T", "F", "S", "S"),
        labels = SjStreakWeekLabels(read = "read", done = "read and reflected", today = "today", notRead = "not read"),
        today = 5,
    )
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
    ) {
        SjAchievementSeal("First step", "First chapter read", SjAchievementSet.Sheep, unlocked = true)
        SjAchievementSeal("Week", "7 days with Scripture", SjAchievementSet.Bird, unlocked = true, tilt = 6f)
        SjAchievementSeal("Reflective", "10 reflections", SjAchievementSet.Lamp, unlocked = true)
        SjAchievementSeal("Psalms", "Read all 150", SjAchievementSet.Sheep, unlocked = false)
    }
}

@Composable
private fun Cards() {
    SjTodayCard(
        tags = listOf("Today", "Day 4 / 21"),
        theme = "the woman\nat the well",
        reference = "John 4 · Gospel of John",
        minutes = "7",
        minutesUnit = "min",
        actionLabel = "Continue",
        onAction = {},
        progress = SjTodayProgress(0.4f, "Verse 22 / 54", "40%", "Chapter progress"),
    )
    SjPlanList {
        SjPlanRow("Gospel of John", "Day 8 / 21 · 10 min a day", SjPlanKind.Sequential, 0.38f, onClick = {})
        SjPlanRow("Псалмы утешения", "День 2 / 14 · 5 мин в день", SjPlanKind.Psalms, 0.12f, onClick = {})
        SjPlanRow("First steps", "Finished 2 Oct", SjPlanKind.Introductory, 1f, finished = "7/7")
    }
    SjWelcomeBack(
        headline = "welcome\nback",
        message = "You were reading John 4. Pick up where you left off, or start with today's passage.",
        resumeLabel = "Resume",
        onResume = {},
        todayLabel = "Today's reading",
        onToday = {},
        illustration = { Mascot(MascotCharacter.Pip, MascotPose.WelcomeBack, size = 96.dp) },
    )
}

@Composable
private fun Reading() {
    var selected by remember { mutableStateOf<Int?>(1) }
    SjReaderText(
        translation = "Psalms · KJV",
        chapterTitle = "Psalm 119",
        verses = listOf(
            SjVerse(105, AnnotatedString("Thy word is a lamp unto my feet, and a light unto my path.")),
            SjVerse(
                106,
                AnnotatedString("I have sworn, and I will perform it, that I will keep thy righteous judgments."),
            ),
            SjVerse(107, AnnotatedString("Сильно угнетён я, Господи; оживи меня по слову Твоему.")),
        ),
        credit = "Psalm 119:105–107, King James Version (public domain).",
        highlighted = setOf(0),
        selected = selected,
        onVerseClick = { selected = if (selected == it) null else it },
    )
    SjVerseActionBar(
        listOf(
            SjVerseAction("Highlight", SjIcons.Highlight, applied = true, onClick = {}),
            SjVerseAction("Save", SjIcons.Save, onClick = {}),
            SjVerseAction("Reflect", SjIcons.Reflect, onClick = {}),
            SjVerseAction("Share", SjIcons.Share, onClick = {}),
        ),
    )
}

@Composable
private fun Navigation() {
    var tab by remember { mutableIntStateOf(0) }
    SjBottomNav(
        items = listOf(
            SjNavItem("Today", SjIcons.Today),
            SjNavItem("Bible", SjIcons.Bible),
            SjNavItem("Plans", SjIcons.Plans),
            SjNavItem("Journey", SjIcons.Journey),
        ),
        selected = tab,
        onSelect = { tab = it },
    )
}

@Composable
private fun Icons() {
    val icons = with(SjIcons) {
        listOf(
            Today, Bible, Plans, Journey, Highlight, Save, Reflect, Share, Copy, ArrowRight, ArrowLeft, ChevronLeft,
            ChevronRight, ChevronDown, Close, Check, Lock, Settings, Reminder, Streak, TextSize, Translation, Search,
            Add,
        )
    }
    icons.chunked(8).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
            row.forEach { SjIcon(it, contentDescription = null) }
        }
    }
}

@Composable
private fun Mascots() {
    val colors = SjTheme.colors
    listOf(
        MascotCharacter.Woolly to colors.sage,
        MascotCharacter.Lumi to colors.butter,
        MascotCharacter.Pip to colors.rose,
    ).forEach { (character, sticker) ->
        StickerSurface(sticker) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2),
            ) {
                MascotPose.entries.forEach { pose -> Mascot(character, pose, size = 96.dp) }
            }
        }
    }
}

@Preview
@Composable
private fun SjCatalogPreview() {
    SjTheme { SjCatalog() }
}
