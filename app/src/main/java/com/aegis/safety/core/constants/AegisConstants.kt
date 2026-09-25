package com.aegis.safety.core.constants

object AegisConstants {
    const val SOS_COUNTDOWN_SECONDS = 5
    const val SOS_HOLD_DURATION_MS = 1500L
    const val LOCATION_UPDATE_INTERVAL_MS = 10_000L
    const val LOCATION_FASTEST_INTERVAL_MS = 5_000L
    const val EMERGENCY_LOCATION_INTERVAL_MS = 5_000L
    const val SAFETY_TIMER_CHANNEL_ID = "aegis_safety_timer"
    const val DEFAULT_API_TIMEOUT_SECONDS = 30L
    const val WS_RECONNECT_BASE_DELAY_MS = 1_000L
    const val WS_RECONNECT_MAX_DELAY_MS = 30_000L
    const val OFFLINE_QUEUE_MAX_ATTEMPTS = 5

    // Auto-detection
    const val MONITORING_MIN_ESCALATION_INTERVAL_MS = 20_000L
    const val MONITORING_MAX_ESCALATIONS_PER_HOUR = 12
    const val MONITORING_DEFAULT_CONFIRMATION_SECONDS = 15

    // Voice trigger
    const val VOICE_TRIGGER_REFRACTORY_MS = 3_000L
}

object AegisRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val SOS_COUNTDOWN = "sos_countdown"
    const val ACTIVE_EMERGENCY = "active_emergency/{incidentId}"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val SAFETY_TIMER = "safety_timer"
    const val LIVE_LOCATION = "live_location"
    const val SAFE_ZONES = "safe_zones"
    const val MAP = "map"
    const val INCIDENTS = "incidents"
    const val INCIDENT_DETAILS = "incident_details/{incidentId}"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val PERMISSIONS = "permissions"
    const val HELP = "help"
    const val ABOUT = "about"
    const val DEMO = "demo"
    const val MONITORING = "monitoring"
    const val JOURNEY = "journey"
    const val JOURNEY_ACTIVE = "journey_active"
    const val VOICE_TRIGGER = "voice_trigger"
    const val CHAT = "chat"

    fun incidentDetails(id: String) = "incident_details/$id"
    fun activeEmergency(id: String) = "active_emergency/$id"
}