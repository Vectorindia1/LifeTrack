package com.lifetrack.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun LifeTrackApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.fromRoute(backStackEntry?.destination?.route)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Destination.bottomBarDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination,
                        onClick = { navController.navigateToTab(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                // The label Text below is gone, so this is now the
                                // only description a screen reader has for the tab —
                                // it must carry what the label used to say.
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        // Icon-only nav bar, per the user's request: the icons are
                        // distinct enough (calendar/check/wallet/book/gear) not to
                        // need text labels for a sighted user.
                        label = null,
                    )
                }
            }
        },
    ) { innerPadding ->
        LifeTrackNavHost(
            navController = navController,
            contentPadding = innerPadding,
        )
    }
}
