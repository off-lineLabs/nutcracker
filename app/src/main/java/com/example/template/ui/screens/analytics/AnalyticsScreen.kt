package com.example.template.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.R
import com.example.template.ui.theme.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Enum for analytics sections
enum class AnalyticsSection {
    NUTRITION,
    EXERCISE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean
) {
    // Animation state for the circular reveal effect
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Current section state
    var currentSection by remember { mutableStateOf(AnalyticsSection.NUTRITION) }
    
    // Start animation on first composition
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Animated values for scale and alpha
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    // Animated background colors based on selected section
    val backgroundStartColor by animateColorAsState(
        targetValue = appBackgroundColor(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "backgroundStart"
    )
    
    val backgroundEndColor by animateColorAsState(
        targetValue = when (currentSection) {
            AnalyticsSection.NUTRITION -> if (isDarkTheme) BrandGold else BrandGoldLightTheme
            AnalyticsSection.EXERCISE -> if (isDarkTheme) BrandRed else BrandRedLightTheme
        },
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "backgroundEnd"
    )
    
    // Handle back navigation with reverse animation
    fun handleBack() {
        isVisible = false
        // Delay to allow animation to complete
        coroutineScope.launch {
            delay(300)
            onNavigateBack()
        }
    }
    
    // Intercept system back button
    androidx.activity.compose.BackHandler {
        handleBack()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundStartColor,
                        backgroundEndColor.copy(alpha = 0.3f)
                    )
                )
            )
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
                // Center the scaling transformation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 0.1f)
            }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.analytics_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = appTextPrimaryColor()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { handleBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = appTextPrimaryColor()
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    
                    // Section tabs
                    SectionTabs(
                        currentSection = currentSection,
                        onSectionChange = { currentSection = it }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Animated content with swipe-like transition
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = {
                        // Determine slide direction based on section change
                        val slideDirection = if (targetState == AnalyticsSection.EXERCISE) {
                            // Moving from Nutrition to Exercise (right to left)
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        } else {
                            // Moving from Exercise to Nutrition (left to right)
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        }
                        slideDirection.using(SizeTransform(clip = false))
                    },
                    label = "sectionContent"
                ) { section ->
                    when (section) {
                        AnalyticsSection.NUTRITION -> NutritionAnalyticsContent()
                        AnalyticsSection.EXERCISE -> ExerciseAnalyticsContent()
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTabs(
    currentSection: AnalyticsSection,
    onSectionChange: (AnalyticsSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exercise Tab
        SectionTabButton(
            icon = { Icon(painterResource(R.drawable.ic_sprint), contentDescription = "Exercise") },
            label = "Exercise",
            isSelected = currentSection == AnalyticsSection.EXERCISE,
            onClick = { onSectionChange(AnalyticsSection.EXERCISE) },
            selectedColor = BrandRed
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Nutrition Tab
        SectionTabButton(
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = "Nutrition") },
            label = "Nutrition",
            isSelected = currentSection == AnalyticsSection.NUTRITION,
            onClick = { onSectionChange(AnalyticsSection.NUTRITION) },
            selectedColor = BrandGold
        )
    }
}

@Composable
fun SectionTabButton(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else appSurfaceColor(),
        animationSpec = tween(300),
        label = "tabBackground"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else appTextSecondaryColor(),
        animationSpec = tween(300),
        label = "tabContent"
    )
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .height(48.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                icon()
            }
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun NutritionAnalyticsContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nutrition Analytics\n(Coming soon)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = appTextPrimaryColor()
        )
    }
}

@Composable
fun ExerciseAnalyticsContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Exercise Analytics\n(Coming soon)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = appTextPrimaryColor()
        )
    }
}

