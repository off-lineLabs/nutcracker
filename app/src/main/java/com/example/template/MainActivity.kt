package com.example.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
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

@Preview
@Composable
fun DefaultPreview() {
    FoodLogTheme {
        DashboardScreen()
    }
}
