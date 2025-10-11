package com.example.template.ui.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.ui.theme.appBackgroundColor
import com.example.template.ui.theme.appTextPrimaryColor
import com.example.template.ui.theme.appTextSecondaryColor
import com.example.template.ui.theme.getContrastingTextColor

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
    val faqItems = remember {
        listOf(
            FAQItem(
                question = "How do I add a meal?",
                answer = "To add a meal, tap the '+' button on the dashboard. You can then search for food items, scan a barcode, or manually enter nutrition information."
            ),
            FAQItem(
                question = "How do I track my calories?",
                answer = "Your calories are automatically tracked when you add meals. The dashboard shows your daily calorie intake and progress toward your goals."
            ),
            FAQItem(
                question = "How do I set my nutrition goals?",
                answer = "Go to Settings and scroll down to 'Nutrition Goals'. You can set daily targets for calories, protein, carbs, fat, fiber, and sodium."
            ),
            FAQItem(
                question = "How do I add an exercise?",
                answer = "Tap the '+' button on the dashboard and select 'Add Exercise'. You can choose from cardio, strength training, or bodyweight exercises."
            ),
            FAQItem(
                question = "How do I view my progress?",
                answer = "Tap the analytics icon (bar chart) on the dashboard to view your weekly and monthly progress, including exercise history and nutrition trends."
            ),
            FAQItem(
                question = "How do I change the date?",
                answer = "Use the calendar icon on the dashboard to select a different date, or use the arrow buttons to navigate day by day."
            ),
            FAQItem(
                question = "How do I scan a barcode?",
                answer = "When adding a meal, tap the barcode scanner icon. Point your camera at the product's barcode to automatically find nutrition information."
            ),
            FAQItem(
                question = "How do I edit or delete entries?",
                answer = "Tap on any meal or exercise entry in your daily log to view details. From there, you can edit or delete the entry."
            ),
            FAQItem(
                question = "How do I change the app theme?",
                answer = "Go to Settings and select your preferred theme: Light, Dark, or System Default."
            ),
            FAQItem(
                question = "How do I change the language?",
                answer = "Go to Settings and select your preferred language from the Language option."
            )
        )
    }

    var expandedItems by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & FAQ",
                        color = getContrastingTextColor(appBackgroundColor()),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                    text = "Frequently Asked Questions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = getContrastingTextColor(appBackgroundColor()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Find answers to common questions about using the app",
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
                            text = "Still need help?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appTextPrimaryColor(),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "If you can't find the answer you're looking for, try exploring the app features or check the settings for more options.",
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
