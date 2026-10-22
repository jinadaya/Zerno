package app.zerno.designsystem.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
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
import app.zerno.designsystem.component.SjPlaceholderScreen
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
import app.zerno.designsystem.type.SjFontsSample
import app.zerno.designsystem.type.SjTypeSample
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/**
 * Every design system component in light and dark, with English and Russian copy and resources. The PNGs live in
 * `src/androidHostTest/screenshots`; a pixel change against them fails the test (README, Screenshot tests).
 * Language-free drawings (icons, mascots) are shot once per theme.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ComponentScreenshotTest(private val variant: Variant) {
    enum class Variant(val dark: Boolean, val language: String, val copy: SampleCopy) {
        LightEn(dark = false, language = "en", copy = SampleCopy.En),
        DarkEn(dark = true, language = "en", copy = SampleCopy.En),
        LightRu(dark = false, language = "ru", copy = SampleCopy.Ru),
        DarkRu(dark = true, language = "ru", copy = SampleCopy.Ru),
    }

    private val copy get() = variant.copy

    @Before
    fun setLanguage() {
        Locale.setDefault(Locale.forLanguageTag(variant.language))
        RuntimeEnvironment.setQualifiers("+${variant.language}")
    }

    @Test fun buttons() = shoot("buttons") {
        SjPrimaryButton(copy.primary, onClick = {})
        SjOutlineButton(copy.outline, onClick = {})
        SjLinkButton(copy.link, onClick = {})
        SjPrimaryButton(copy.disabled, onClick = {}, enabled = false)
        StickerSurface(SjTheme.colors.butter) {
            SjPrimaryButton(copy.primary, onClick = {}, modifier = Modifier.fillMaxWidth())
            SjOutlineButton(copy.outline, onClick = {})
        }
    }

    @Test fun tags() = shoot("tags") {
        Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
            copy.tags.forEach { SjTag(it) }
            SjTag(copy.solidTag, solid = true)
        }
    }

    @Test fun chips() = shoot("chips") {
        SjChipGroup(copy.chips, selected = 1, onSelect = {}, label = copy.chipsLabel)
        StickerSurface(SjTheme.colors.butter) {
            SjChipGroup(copy.chips, selected = 0, onSelect = {}, label = copy.chipsLabel)
        }
    }

    @Test fun textFields() = shoot("text-fields") {
        SjTextField("", {}, label = copy.nameLabel, placeholder = copy.namePlaceholder)
        SjTextField("25:00", {}, label = copy.timeLabel, error = copy.timeError)
        SjReflectionField("", {}, label = copy.reflectionLabel, placeholder = copy.reflectionPlaceholder)
        SjReflectionField(copy.reflection, {}, label = copy.reflectionLabel)
    }

    @Test fun switches() = shoot("switches") {
        SjSwitchRow(copy.switchLabel, checked = true, onCheckedChange = {})
        SjSwitchRow(copy.switchLabel, checked = false, onCheckedChange = {})
    }

    @Test fun progress() = shoot("progress") {
        SjBigStat(copy.statValue, copy.statLine, unit = "%")
        SjProgressLine(0.4f, copy.progressLabel, startLabel = copy.progressStart, endLabel = "40%")
        SjProgressLine(1f, copy.progressLabel)
    }

    @Test fun streakWeek() = shoot("streak-week") {
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
            dayLetters = copy.weekLetters,
            labels = SjStreakWeekLabels(copy.weekRead, copy.weekDone, copy.weekToday, copy.weekNotRead),
            today = 5,
        )
    }

    @Test fun achievementSeals() = shoot("achievement-seals") {
        val sets = listOf(SjAchievementSet.Sheep, SjAchievementSet.Bird, SjAchievementSet.Lamp, SjAchievementSet.Sheep)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
        ) {
            copy.seals.forEachIndexed { index, (name, reason) ->
                SjAchievementSeal(name, reason, sets[index], unlocked = index < copy.seals.lastIndex)
            }
        }
    }

    @Test fun todayCard() = shoot("today-card") {
        SjTodayCard(
            tags = copy.todayTags,
            theme = copy.todayTheme,
            reference = copy.todayReference,
            minutes = "7",
            minutesUnit = copy.minutesUnit,
            actionLabel = copy.primary,
            onAction = {},
        )
        SjTodayCard(
            tags = copy.todayTags,
            theme = copy.todayTheme,
            reference = copy.todayReference,
            minutes = "7",
            minutesUnit = copy.minutesUnit,
            actionLabel = copy.todayAction,
            onAction = {},
            progress = SjTodayProgress(0.4f, copy.progressStart, "40%", copy.progressLabel),
        )
    }

    @Test fun planList() = shoot("plan-list") {
        val (john, psalms, firstSteps) = copy.plans
        SjPlanList {
            SjPlanRow(john.first, john.second, SjPlanKind.Sequential, 0.38f, onClick = {})
            SjPlanRow(psalms.first, psalms.second, SjPlanKind.Psalms, 0.12f, onClick = {})
            SjPlanRow(firstSteps.first, firstSteps.second, SjPlanKind.Introductory, 1f, finished = copy.planFinished)
        }
    }

    @Test fun welcomeBack() = shoot("welcome-back") {
        SjWelcomeBack(
            headline = copy.welcomeHeadline,
            message = copy.welcomeMessage,
            resumeLabel = copy.welcomeResume,
            onResume = {},
            todayLabel = copy.welcomeToday,
            onToday = {},
            illustration = { Mascot(MascotCharacter.Pip, MascotPose.WelcomeBack, size = 96.dp) },
        )
    }

    @Test fun readerText() = shoot("reader-text") {
        SjReaderText(
            translation = copy.translation,
            chapterTitle = copy.chapterTitle,
            verses = copy.verses.mapIndexed { index, text -> SjVerse(105 + index, AnnotatedString(text)) },
            credit = copy.credit,
            highlighted = setOf(0),
            selected = 1,
            onVerseClick = {},
        )
    }

    @Test fun verseActions() = shoot("verse-actions") {
        val icons = listOf(SjIcons.Highlight, SjIcons.Save, SjIcons.Reflect, SjIcons.Share)
        SjVerseActionBar(
            copy.verseActions.mapIndexed { index, label ->
                SjVerseAction(label, icons[index], applied = index == 0, onClick = {})
            },
        )
    }

    @Test fun bottomNav() = shoot("bottom-nav") {
        val icons = listOf(SjIcons.Today, SjIcons.Bible, SjIcons.Plans, SjIcons.Journey)
        SjBottomNav(
            items = copy.navItems.mapIndexed { index, label -> SjNavItem(label, icons[index]) },
            selected = 0,
            onSelect = {},
        )
    }

    @Test fun cover() = shoot("cover") {
        SjCover(copy.coverTitle, copy.coverTagline)
    }

    @Test fun placeholderScreen() = shoot("placeholder-screen", padded = false) {
        // The back label comes from the module's strings, so this one also checks the resource locale.
        SjPlaceholderScreen(copy.placeholderTitle, copy.placeholderCaption, onBack = {})
    }

    @Test fun type() = shootOnce("type") {
        SjTypeSample()
    }

    @Test fun fonts() = shootOnce("fonts") {
        SjFontsSample()
    }

    @Test fun icons() = shootOnce("icons") {
        val icons = with(SjIcons) {
            listOf(
                Today, Bible, Plans, Journey, Highlight, Save, Reflect, Share, Copy, ArrowRight, ArrowLeft,
                ChevronLeft, ChevronRight, ChevronDown, Close, Check, Lock, Settings, Reminder, Streak, TextSize,
                Translation, Search, Add,
            )
        }
        icons.chunked(8).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
                row.forEach { SjIcon(it, contentDescription = null) }
            }
        }
    }

    @Test fun mascots() = shootOnce("mascots") {
        val colors = SjTheme.colors
        listOf(
            MascotCharacter.Woolly to colors.sage,
            MascotCharacter.Lumi to colors.butter,
            MascotCharacter.Pip to colors.rose,
        ).forEach { (character, sticker) ->
            StickerSurface(sticker) {
                Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
                    MascotPose.entries.forEach { pose -> Mascot(character, pose, size = 64.dp) }
                }
            }
        }
    }

    /** Shoots a drawing with no copy in it: once per theme, under the English variants. */
    private fun shootOnce(name: String, content: @Composable ColumnScope.() -> Unit) {
        assumeTrue(variant.language == "en")
        shoot(name, fileSuffix = if (variant.dark) "dark" else "light", content = content)
    }

    private fun shoot(
        name: String,
        padded: Boolean = true,
        fileSuffix: String = variant.name.toKebab(),
        content: @Composable ColumnScope.() -> Unit,
    ) {
        captureRoboImage("src/androidHostTest/screenshots/$name/$fileSuffix.png") {
            SjTheme(darkTheme = variant.dark, reducedMotion = true) {
                Column(
                    modifier = Modifier
                        .width(ScreenWidth)
                        .background(SjTheme.colors.paper)
                        .then(if (padded) Modifier.padding(SjTheme.spacing.gutter) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
                    content = content,
                )
            }
        }
    }

    private fun String.toKebab() = replace(Regex("(?<=[a-z])(?=[A-Z])"), "-").lowercase()

    companion object {
        private val ScreenWidth = 393.dp

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = Variant.entries.map { arrayOf(it) }
    }
}
