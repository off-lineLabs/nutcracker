package com.offlinelabs.nutcracker.ui.screens.dashboard

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.fabExerciseColor
import com.offlinelabs.nutcracker.ui.theme.fabMealColor
import com.offlinelabs.nutcracker.ui.theme.onTabPillColor
import com.offlinelabs.nutcracker.ui.theme.tabHomeColor

/** Pager pages of the dashboard, in swipe order. */
const val DASHBOARD_PAGE_FOOD = 0
const val DASHBOARD_PAGE_HOME = 1
const val DASHBOARD_PAGE_EXERCISE = 2

/**
 * Bottom tab bar: Food, Home, Exercise. The selected item's indicator pill is tinted with the
 * former FAB colours (gold / red) and a neutral grey for Home.
 *
 * The Food and Exercise items register their coordinates under the tutorial's
 * "food_tab" / "exercise_tab" keys.
 */
@Composable
fun DashboardTabBar(
    selectedPage: Int,
    onSelectPage: (Int) -> Unit,
    registerElementCoordinates: (String, Offset, Size) -> Unit
) {
    val density = LocalDensity.current

    fun Modifier.trackForTutorial(key: String?): Modifier =
        if (key == null) this else this.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val padding = with(density) { 4.dp.toPx() }
            registerElementCoordinates(
                key,
                bounds.center,
                Size(bounds.width + padding * 2, bounds.height + padding * 2)
            )
        }

    @Composable
    fun RowScope.TabItem(
        page: Int,
        icon: @Composable () -> Unit,
        labelRes: Int,
        pillColor: Color,
        tutorialKey: String?
    ) {
        NavigationBarItem(
            selected = selectedPage == page,
            onClick = { onSelectPage(page) },
            icon = icon,
            label = { Text(stringResource(labelRes), maxLines = 1) },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = pillColor,
                selectedIconColor = onTabPillColor(pillColor),
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.trackForTutorial(tutorialKey)
        )
    }

    NavigationBar {
        TabItem(
            page = DASHBOARD_PAGE_FOOD,
            icon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
            labelRes = R.string.tab_food,
            pillColor = fabMealColor(),
            tutorialKey = "food_tab"
        )
        TabItem(
            page = DASHBOARD_PAGE_HOME,
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            labelRes = R.string.tab_home,
            pillColor = tabHomeColor(),
            tutorialKey = null
        )
        TabItem(
            page = DASHBOARD_PAGE_EXERCISE,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_sprint),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            labelRes = R.string.tab_exercise,
            pillColor = fabExerciseColor(),
            tutorialKey = "exercise_tab"
        )
    }
}
