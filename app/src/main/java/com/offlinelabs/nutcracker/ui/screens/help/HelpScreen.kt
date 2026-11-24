package com.offlinelabs.nutcracker.ui.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import com.offlinelabs.nutcracker.R
import androidx.compose.foundation.lazy.items
import com.offlinelabs.nutcracker.ui.theme.appBackgroundColor
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor

/** What a question is about; Help lists the section of the tab it was opened from first. */
enum class FaqSection { FOOD, EXERCISE, GENERAL }

data class FAQItem(
    val question: String,
    val answer: String,
    val section: FaqSection = FaqSection.GENERAL
)

/**
 * The order of the sections: Food first when Help was opened from the Food tab, Exercise first
 * from the Exercise tab, General first from Home.
 */
fun faqSectionOrder(focusPage: Int): List<FaqSection> = when (focusPage) {
    com.offlinelabs.nutcracker.ui.screens.dashboard.DASHBOARD_PAGE_FOOD ->
        listOf(FaqSection.FOOD, FaqSection.EXERCISE, FaqSection.GENERAL)
    com.offlinelabs.nutcracker.ui.screens.dashboard.DASHBOARD_PAGE_EXERCISE ->
        listOf(FaqSection.EXERCISE, FaqSection.FOOD, FaqSection.GENERAL)
    else -> listOf(FaqSection.GENERAL, FaqSection.FOOD, FaqSection.EXERCISE)
}

@Composable
fun createClickableText(
    text: String,
    isDarkTheme: Boolean
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val linkPatterns = mapOf(
            "this comprehensive review" to "https://nutritionandmetabolism.biomedcentral.com/articles/10.1186/1743-7075-1-5",
            "their mobile app" to "https://es.openfoodfacts.org/open-food-facts-mobile-app",
            "esta revisión completa" to "https://nutritionandmetabolism.biomedcentral.com/articles/10.1186/1743-7075-1-5",
            "su aplicación móvil" to "https://es.openfoodfacts.org/open-food-facts-mobile-app",
            "esta revisão abrangente" to "https://nutritionandmetabolism.biomedcentral.com/articles/10.1186/1743-7075-1-5",
            "o aplicativo móvel deles" to "https://es.openfoodfacts.org/open-food-facts-mobile-app"
        )
        
        var currentText = text
        val linkRanges = mutableListOf<Pair<String, String>>()
        
        // Find all link patterns in the text
        linkPatterns.forEach { (pattern, url) ->
            val index = currentText.indexOf(pattern)
            if (index != -1) {
                linkRanges.add(Pair(pattern, url))
            }
        }
        
        // Sort by position in text
        linkRanges.sortBy { currentText.indexOf(it.first) }
        
        var lastIndex = 0
        linkRanges.forEach { (pattern, url) ->
            val startIndex = currentText.indexOf(pattern, lastIndex)
            if (startIndex != -1) {
                // Add text before the link
                if (startIndex > lastIndex) {
                    append(currentText.substring(lastIndex, startIndex))
                }
                
                // Add the clickable link with theme-aware colors
                withStyle(
                    style = SpanStyle(
                        color = if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF64B5F6) else androidx.compose.ui.graphics.Color(0xFF1976D2),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = url
                    )
                    append(pattern)
                    pop()
                }
                
                lastIndex = startIndex + pattern.length
            }
        }
        
        // Add remaining text
        if (lastIndex < currentText.length) {
            append(currentText.substring(lastIndex))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean,
    onReplayTutorial: () -> Unit = {},
    /** The dashboard tab Help was opened from: its questions come first. */
    focusPage: Int = com.offlinelabs.nutcracker.ui.screens.dashboard.DASHBOARD_PAGE_HOME
) {
    val context = LocalContext.current
    val allFaqItems = listOf(
        // Food
        FAQItem(
            question = stringResource(R.string.faq_how_add_meal),
            answer = stringResource(R.string.faq_answer_add_meal),
            section = FaqSection.FOOD
        ),
        FAQItem(stringResource(R.string.faq_quick_food_q), stringResource(R.string.faq_quick_food_a), FaqSection.FOOD),
        FAQItem(stringResource(R.string.faq_meal_types_q), stringResource(R.string.faq_meal_types_a), FaqSection.FOOD),
        FAQItem(stringResource(R.string.faq_food_swipe_q), stringResource(R.string.faq_food_swipe_a), FaqSection.FOOD),
        FAQItem(
            question = stringResource(R.string.faq_how_scan_barcode),
            answer = stringResource(R.string.faq_answer_scan_barcode),
            section = FaqSection.FOOD
        ),
        FAQItem(
            question = stringResource(R.string.faq_what_food_source),
            answer = stringResource(R.string.faq_answer_food_source),
            section = FaqSection.FOOD
        ),
        FAQItem(
            question = stringResource(R.string.faq_what_tef_bonus),
            answer = stringResource(R.string.faq_answer_tef_bonus),
            section = FaqSection.FOOD
        ),
        // Exercise
        FAQItem(
            question = stringResource(R.string.faq_how_add_exercise),
            answer = stringResource(R.string.faq_answer_add_exercise),
            section = FaqSection.EXERCISE
        ),
        FAQItem(stringResource(R.string.faq_quick_exercise_q), stringResource(R.string.faq_quick_exercise_a), FaqSection.EXERCISE),
        FAQItem(stringResource(R.string.faq_sets_q), stringResource(R.string.faq_sets_a), FaqSection.EXERCISE),
        FAQItem(stringResource(R.string.faq_routines_q), stringResource(R.string.faq_routines_a), FaqSection.EXERCISE),
        FAQItem(
            question = stringResource(R.string.faq_how_gyms),
            answer = stringResource(R.string.faq_answer_gyms),
            section = FaqSection.EXERCISE
        ),
        FAQItem(stringResource(R.string.faq_synced_copies_q), stringResource(R.string.faq_synced_copies_a), FaqSection.EXERCISE),
        FAQItem(stringResource(R.string.faq_exercise_swipe_q), stringResource(R.string.faq_exercise_swipe_a), FaqSection.EXERCISE),
        FAQItem(
            question = stringResource(R.string.faq_what_exercise_source),
            answer = stringResource(R.string.faq_answer_exercise_source),
            section = FaqSection.EXERCISE
        ),
        // General
        FAQItem(stringResource(R.string.faq_fine_control_q), stringResource(R.string.faq_fine_control_a)),
        FAQItem(stringResource(R.string.faq_sort_group_q), stringResource(R.string.faq_sort_group_a)),
        FAQItem(stringResource(R.string.faq_scroll_hand_q), stringResource(R.string.faq_scroll_hand_a)),
        FAQItem(
            question = stringResource(R.string.faq_how_track_calories),
            answer = stringResource(R.string.faq_answer_track_calories)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_set_goals),
            answer = stringResource(R.string.faq_answer_set_goals)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_view_progress),
            answer = stringResource(R.string.faq_answer_view_progress)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_change_date),
            answer = stringResource(R.string.faq_answer_change_date)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_edit_entries),
            answer = stringResource(R.string.faq_answer_edit_entries)
        ),
        // App Features & Settings
        FAQItem(
            question = stringResource(R.string.faq_how_change_theme),
            answer = stringResource(R.string.faq_answer_change_theme)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_change_language),
            answer = stringResource(R.string.faq_answer_change_language)
        ),
        // Privacy & Business
        FAQItem(
            question = stringResource(R.string.faq_what_data_collected),
            answer = stringResource(R.string.faq_answer_data_collected)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_make_money),
            answer = stringResource(R.string.faq_answer_make_money)
        ),
        FAQItem(
            question = stringResource(R.string.faq_what_logo_means),
            answer = stringResource(R.string.faq_answer_logo_means)
        )
    )
    // All questions, the tab's section first; faqItems indices stay stable for the expanded state
    val sectionOrder = faqSectionOrder(focusPage)
    val faqItems = sectionOrder.flatMap { s -> allFaqItems.filter { it.section == s } }
    val sectionTitles = mapOf(
        FaqSection.FOOD to stringResource(R.string.tab_food),
        FaqSection.EXERCISE to stringResource(R.string.tab_exercise),
        FaqSection.GENERAL to stringResource(R.string.faq_section_general)
    )

    var expandedItems by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.help_faq_title),
                        color = getContrastingTextColor(appBackgroundColor()),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = getContrastingTextColor(appBackgroundColor())
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBackgroundColor()
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.help_faq_subtitle),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = getContrastingTextColor(appBackgroundColor()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.help_faq_description),
                    fontSize = 16.sp,
                    color = appTextSecondaryColor(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Replay Tutorial Button
                Button(
                    onClick = onReplayTutorial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_replay),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            sectionOrder.forEach { section ->
            item(key = "faq_header_$section") {
                Text(
                    text = sectionTitles.getValue(section).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = appTextSecondaryColor(),
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            items(faqItems.indices.filter { faqItems[it].section == section }) { index ->
                val faq = faqItems[index]
                val isExpanded = expandedItems.contains(index)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appBackgroundColor()
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        // Question row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = faq.question,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = appTextPrimaryColor(),
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = {
                                    expandedItems = if (isExpanded) {
                                        expandedItems - index
                                    } else {
                                        expandedItems + index
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.Remove else Icons.Filled.Add,
                                    contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
                                    tint = appTextSecondaryColor()
                                )
                            }
                        }
                        
                        // Answer (expandable)
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = appTextSecondaryColor().copy(alpha = 0.3f),
                                    thickness = 1.dp
                                )
                                
                                val clickableText = createClickableText(faq.answer, isDarkTheme)
                                @Suppress("DEPRECATION")
                                ClickableText(
                                    text = clickableText,
                                    onClick = { offset ->
                                        clickableText.getStringAnnotations(
                                            tag = "URL",
                                            start = offset,
                                            end = offset
                                        ).firstOrNull()?.let { annotation ->
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // Handle error if no browser is available
                                            }
                                        }
                                    },
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        color = appTextSecondaryColor(),
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appTextSecondaryColor().copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.help_still_need_help),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appTextPrimaryColor(),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.help_still_need_help_description),
                            fontSize = 14.sp,
                            color = appTextSecondaryColor(),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
