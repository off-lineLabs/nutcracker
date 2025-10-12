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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.appBackgroundColor
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor

data class FAQItem(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean
) {
    val faqItems = listOf(
        // Basic Usage
        FAQItem(
            question = stringResource(R.string.faq_how_add_meal),
            answer = stringResource(R.string.faq_answer_add_meal)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_track_calories),
            answer = stringResource(R.string.faq_answer_track_calories)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_set_goals),
            answer = stringResource(R.string.faq_answer_set_goals)
        ),
        FAQItem(
            question = stringResource(R.string.faq_how_add_exercise),
            answer = stringResource(R.string.faq_answer_add_exercise)
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
            question = stringResource(R.string.faq_how_scan_barcode),
            answer = stringResource(R.string.faq_answer_scan_barcode)
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
        // App Information & Sources
        FAQItem(
            question = stringResource(R.string.faq_what_tef_bonus),
            answer = stringResource(R.string.faq_answer_tef_bonus)
        ),
        FAQItem(
            question = stringResource(R.string.faq_what_food_source),
            answer = stringResource(R.string.faq_answer_food_source)
        ),
        FAQItem(
            question = stringResource(R.string.faq_what_exercise_source),
            answer = stringResource(R.string.faq_answer_exercise_source)
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
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            items(faqItems.size) { index ->
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
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
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
                                
                                Text(
                                    text = faq.answer,
                                    fontSize = 14.sp,
                                    color = appTextSecondaryColor(),
                                    lineHeight = 20.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
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
