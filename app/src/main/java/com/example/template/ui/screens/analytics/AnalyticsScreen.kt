package com.example.template.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.R
import com.example.template.ui.theme.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean
) {
    // Animation state for the circular reveal effect
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
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
                        appBackgroundColor(),
                        appBackgroundColor()
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
                        containerColor = appBackgroundColor()
                    )
                )
            },
            containerColor = appBackgroundColor()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Analytics Screen",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = appTextPrimaryColor()
                )
            }
        }
    }
}

