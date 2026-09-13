package dev.lisfox.geyserlauncher.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.lisfox.geyserlauncher.data.ConfigCategory
import dev.lisfox.geyserlauncher.ui.files.FileBrowserScreen
import dev.lisfox.geyserlauncher.ui.files.YamlEditorScreen
import dev.lisfox.geyserlauncher.ui.home.HomeScreen
import dev.lisfox.geyserlauncher.ui.logs.LogsScreen
import dev.lisfox.geyserlauncher.ui.settings.AboutScreen
import dev.lisfox.geyserlauncher.ui.settings.ConfigCategoryScreen
import dev.lisfox.geyserlauncher.ui.settings.SettingsScreen
import dev.lisfox.geyserlauncher.ui.settings.JvmSettingsScreen
import dev.lisfox.geyserlauncher.ui.settings.GeyserUpdateScreen
import dev.lisfox.geyserlauncher.ui.theme.AppBackground
import dev.lisfox.geyserlauncher.viewmodel.ConfigViewModel
import dev.lisfox.geyserlauncher.viewmodel.FilesViewModel
import dev.lisfox.geyserlauncher.viewmodel.LauncherViewModel
import dev.lisfox.geyserlauncher.viewmodel.GeyserUpdateViewModel

private object Route {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CONFIG = "config/{category}"
    const val FILES = "files"
    const val EDITOR = "editor/{path}"
    const val LOGS = "logs"
    const val JVM = "jvm"
    const val GEYSER_UPDATE = "geyser-update"
    const val ABOUT = "about"
}

@Composable
fun GeyserLauncherApp(
    launcherViewModel: LauncherViewModel = viewModel(),
    configViewModel: ConfigViewModel = viewModel(),
    filesViewModel: FilesViewModel = viewModel(),
    geyserUpdateViewModel: GeyserUpdateViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val phase by launcherViewModel.phase.collectAsStateWithLifecycle()
    val logs by launcherViewModel.logs.collectAsStateWithLifecycle()
    val status by launcherViewModel.lastMessage.collectAsStateWithLifecycle()
    val error by launcherViewModel.error.collectAsStateWithLifecycle()
    val distribution by geyserUpdateViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun navigateRoot(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Route.HOME,
                    onClick = { navigateRoot(Route.HOME) },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text("主页") }
                )
                NavigationBarItem(
                    selected = currentRoute != Route.HOME,
                    onClick = { navigateRoot(Route.SETTINGS) },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280)
                )
            }
        ) {
            composable(Route.HOME) {
                HomeScreen(
                    phase = phase,
                    status = status,
                    logs = logs,
                    geyserVersion = distribution.installedVersion,
                    onToggle = launcherViewModel::toggle
                )
            }
            composable(Route.SETTINGS) {
                SettingsScreen(
                    onCategory = { category -> navController.navigate("config/${category.route}") },
                    onFiles = { navController.navigate(Route.FILES) },
                    onRawConfig = { navController.navigate("editor/${encodePath("server/config.yml")}") },
                    onLogs = { navController.navigate(Route.LOGS) },
                    onJvm = { navController.navigate(Route.JVM) },
                    onGeyserUpdate = { navController.navigate(Route.GEYSER_UPDATE) },
                    onBattery = { requestBatteryWhitelist(context) },
                    onAbout = { navController.navigate(Route.ABOUT) }
                )
            }
            composable(
                route = Route.CONFIG,
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("category")
                val category = ConfigCategory.entries.firstOrNull { it.route == id } ?: ConfigCategory.GENERAL
                ConfigCategoryScreen(category, configViewModel, navController::popBackStack)
            }
            composable(Route.FILES) {
                FileBrowserScreen(
                    viewModel = filesViewModel,
                    onBack = navController::popBackStack,
                    onEdit = { path -> navController.navigate("editor/${encodePath(path)}") }
                )
            }
            composable(
                route = Route.EDITOR,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { entry ->
                val path = decodePath(entry.arguments?.getString("path").orEmpty())
                YamlEditorScreen(path, filesViewModel.repository(), navController::popBackStack)
            }
            composable(Route.LOGS) {
                LogsScreen(
                    logs = logs,
                    phase = phase,
                    onBack = navController::popBackStack,
                    onClear = launcherViewModel::clearLogs,
                    onSendCommand = launcherViewModel::sendCommand
                )
            }
            composable(Route.JVM) {
                JvmSettingsScreen(navController::popBackStack)
            }
            composable(Route.GEYSER_UPDATE) {
                GeyserUpdateScreen(
                    state = distribution,
                    runtimePhase = phase,
                    onBack = navController::popBackStack,
                    onCheck = geyserUpdateViewModel::check,
                    onUpdate = geyserUpdateViewModel::update
                )
            }
            composable(Route.ABOUT) {
                AboutScreen(distribution.installedVersion, navController::popBackStack)
            }
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = launcherViewModel::consumeError,
            title = { Text("Geyser 启动失败") },
            text = { Text(error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = launcherViewModel::consumeError) { Text("确定") }
            }
        )
    }
}

private fun encodePath(path: String): String = Base64.encodeToString(
    path.toByteArray(Charsets.UTF_8),
    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
)

private fun decodePath(value: String): String = String(
    Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
    Charsets.UTF_8
)

private fun requestBatteryWhitelist(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
        Toast.makeText(context, "应用已在后台白名单中", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            )
        )
    }.onFailure {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
