package com.example.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.template.ui.screens.dashboard.DashboardScreen
import com.example.template.ui.theme.FoodLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodLogTheme {
                DashboardScreen()
            }
        }
    }
}
