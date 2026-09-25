package com.aegis.safety.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aegis.safety.core.constants.AegisRoutes
import com.aegis.safety.presentation.about.AboutScreen
import com.aegis.safety.presentation.authentication.ForgotPasswordScreen
import com.aegis.safety.presentation.authentication.LoginScreen
import com.aegis.safety.presentation.authentication.RegisterScreen
import com.aegis.safety.presentation.chat.ChatScreen
import com.aegis.safety.presentation.contacts.EmergencyContactsScreen
import com.aegis.safety.presentation.demo.DemoScreen
import com.aegis.safety.presentation.help.HelpScreen
import com.aegis.safety.presentation.home.HomeScreen
import com.aegis.safety.presentation.incidents.IncidentDetailsScreen
import com.aegis.safety.presentation.incidents.IncidentsScreen
import com.aegis.safety.presentation.location.LiveLocationScreen
import com.aegis.safety.presentation.monitoring.MonitoringSettingsScreen
import com.aegis.safety.presentation.notifications.NotificationsScreen
import com.aegis.safety.presentation.onboarding.OnboardingScreen
import com.aegis.safety.presentation.permissions.PermissionManagerScreen
import com.aegis.safety.presentation.permissions.RequiredPermissionsScreen
import com.aegis.safety.presentation.privacy.PrivacyScreen
import com.aegis.safety.presentation.profile.ProfileScreen
import com.aegis.safety.presentation.safezones.SafeZonesScreen
import com.aegis.safety.presentation.settings.SettingsScreen
import com.aegis.safety.presentation.sos.SosCountdownScreen
import com.aegis.safety.presentation.splash.SplashDestination
import com.aegis.safety.presentation.splash.SplashScreen
import com.aegis.safety.presentation.timer.SafetyTimerScreen

private const val ROUTE_REQUIRED_PERMISSIONS = "required_permissions"

/** Recognized deeplinks that jump straight into the SOS flow. */
private const val DEEPLINK_SOS_AUTO_DETECTION = "sos:auto_detection"
private const val DEEPLINK_SOS_VOICE_TRIGGER = "sos:voice_trigger"

/**
 * @param initialDeepLink optional deeplink string passed by the host
 *        Activity when it was launched with an intent extra `"deeplink"`.
 *        Recognized values trigger an immediate navigation into SOS.
 */
@Composable
fun AegisNavHost(initialDeepLink: String? = null) {
    val nav = rememberNavController()

    // Consume the deeplink on first composition and whenever it changes.
    // Fires once per unique value — no re-entry on recomposition.
    LaunchedEffect(initialDeepLink) {
        when (initialDeepLink) {
            DEEPLINK_SOS_AUTO_DETECTION, DEEPLINK_SOS_VOICE_TRIGGER -> {
                nav.navigate(AegisRoutes.SOS_COUNTDOWN)
            }
        }
    }

    NavHost(navController = nav, startDestination = AegisRoutes.SPLASH) {

        composable(AegisRoutes.SPLASH) {
            SplashScreen(onNavigate = { dest ->
                val route = when (dest) {
                    SplashDestination.Onboarding -> AegisRoutes.ONBOARDING
                    SplashDestination.Login -> AegisRoutes.LOGIN
                    SplashDestination.Home -> ROUTE_REQUIRED_PERMISSIONS
                }
                nav.navigate(route) { popUpTo(AegisRoutes.SPLASH) { inclusive = true } }
            })
        }

        composable(AegisRoutes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                nav.navigate(AegisRoutes.LOGIN) {
                    popUpTo(AegisRoutes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(AegisRoutes.LOGIN) {
            LoginScreen(
                onNavigateRegister = { nav.navigate(AegisRoutes.REGISTER) },
                onForgotPassword = { nav.navigate(AegisRoutes.FORGOT_PASSWORD) },
                onSuccess = {
                    nav.navigate(ROUTE_REQUIRED_PERMISSIONS) {
                        popUpTo(AegisRoutes.LOGIN) { inclusive = true }
                    }
                })
        }

        composable(AegisRoutes.REGISTER) {
            RegisterScreen(
                onNavigateLogin = { nav.popBackStack() },
                onSuccess = {
                    nav.navigate(ROUTE_REQUIRED_PERMISSIONS) {
                        popUpTo(AegisRoutes.REGISTER) { inclusive = true }
                    }
                })
        }

        composable(AegisRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { nav.popBackStack() })
        }

        composable(ROUTE_REQUIRED_PERMISSIONS) {
            RequiredPermissionsScreen(onContinue = {
                nav.navigate(AegisRoutes.HOME) {
                    popUpTo(ROUTE_REQUIRED_PERMISSIONS) { inclusive = true }
                }
            })
        }

        composable(AegisRoutes.HOME) {
            HomeScreen(
                onSos = { nav.navigate(AegisRoutes.SOS_COUNTDOWN) },
                onContacts = { nav.navigate(AegisRoutes.EMERGENCY_CONTACTS) },
                onSafeZones = { nav.navigate(AegisRoutes.SAFE_ZONES) },
                onTimer = { nav.navigate(AegisRoutes.SAFETY_TIMER) },
                onLiveLocation = { nav.navigate(AegisRoutes.LIVE_LOCATION) },
                onIncidents = { nav.navigate(AegisRoutes.INCIDENTS) },
                onNotifications = { nav.navigate(AegisRoutes.NOTIFICATIONS) },
                onProfile = { nav.navigate(AegisRoutes.PROFILE) },
                onPrivacy = { nav.navigate(AegisRoutes.PRIVACY) },
                onDemo = { nav.navigate(AegisRoutes.DEMO) },
                onMonitoring = { nav.navigate(AegisRoutes.MONITORING) },
                onChat = { nav.navigate(AegisRoutes.CHAT) }
            )
        }

        composable(AegisRoutes.CHAT) {
            ChatScreen(onBack = { nav.popBackStack() })
        }

        // Single SOS screen — handles countdown → sending → active → resolve
        composable(AegisRoutes.SOS_COUNTDOWN) {
            SosCountdownScreen(onFinished = {
                nav.popBackStack(AegisRoutes.HOME, inclusive = false)
            })
        }

        composable(AegisRoutes.EMERGENCY_CONTACTS) {
            EmergencyContactsScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.SAFETY_TIMER) {
            SafetyTimerScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.LIVE_LOCATION) {
            LiveLocationScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.SAFE_ZONES) {
            SafeZonesScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.INCIDENTS) {
            IncidentsScreen(
                onBack = { nav.popBackStack() },
                onOpen = { id -> nav.navigate(AegisRoutes.incidentDetails(id)) }
            )
        }
        composable(
            AegisRoutes.INCIDENT_DETAILS,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType })
        ) { backStack ->
            IncidentDetailsScreen(
                incidentId = backStack.arguments?.getString("incidentId") ?: "",
                onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.PRIVACY) {
            PrivacyScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.PERMISSIONS) {
            PermissionManagerScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.HELP) {
            HelpScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.ABOUT) {
            AboutScreen(onBack = { nav.popBackStack() })
        }
        composable(AegisRoutes.DEMO) {
            DemoScreen(onBack = { nav.popBackStack() })
        }

        // Auto-detection settings
        composable(AegisRoutes.MONITORING) {
            MonitoringSettingsScreen(onBack = { nav.popBackStack() })
        }

        composable(AegisRoutes.PROFILE) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLogout = { nav.navigate(AegisRoutes.LOGIN) { popUpTo(0) } },
                onSettings = { nav.navigate(AegisRoutes.SETTINGS) },
                onPrivacy = { nav.navigate(AegisRoutes.PRIVACY) },
                onHelp = { nav.navigate(AegisRoutes.HELP) },
                onAbout = { nav.navigate(AegisRoutes.ABOUT) })
        }
        composable(AegisRoutes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}