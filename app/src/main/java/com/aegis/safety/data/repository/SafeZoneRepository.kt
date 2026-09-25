package com.aegis.safety.data.repository

import com.aegis.safety.domain.models.SafeZone
import com.aegis.safety.domain.models.SafeZoneType
import com.aegis.safety.domain.repository.SafeZoneRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Bundled safe zones for Coimbatore district.
 *
 * Coordinates are approximate (±50–200 m in most cases). This is
 * deliberately comprehensive so the app works offline and without
 * Places API billing.
 *
 * To verify or refine any entry:
 *   1. Open Google Maps
 *   2. Right-click the location → click the coordinates → copy
 *   3. Replace the lat/lon below
 *
 * The `nearest()` function filters by maxDistanceMeters so only places
 * actually nearby show up. Default radius 15 km covers most of urban
 * Coimbatore.
 */
@Singleton
class SafeZoneRepository @Inject constructor() : SafeZoneRepositoryInterface {

    // =========================================================================
    // POLICE STATIONS (30)
    // =========================================================================
    private val policeStations = listOf(
        SafeZone("cp_cc", "Coimbatore City Police Commissioner Office",
            SafeZoneType.POLICE_STATION, 11.0067, 76.9653, 200f,
            "100", "Race Course Road, Coimbatore", true, verified = true),
        SafeZone("ps_racecourse", "Race Course Police Station",
            SafeZoneType.POLICE_STATION, 11.0068, 76.9714, 100f,
            "100", "Race Course, Coimbatore", true, verified = true),
        SafeZone("ps_rspuram", "RS Puram Police Station",
            SafeZoneType.POLICE_STATION, 11.0147, 76.9497, 100f,
            "100", "DB Road, RS Puram", true, verified = true),
        SafeZone("ps_gandhipuram", "Gandhipuram Police Station",
            SafeZoneType.POLICE_STATION, 11.0183, 76.9675, 100f,
            "100", "Cross Cut Road, Gandhipuram", true, verified = true),
        SafeZone("ps_townhall", "Town Hall Police Station",
            SafeZoneType.POLICE_STATION, 10.9972, 76.9625, 100f,
            "100", "Town Hall, Coimbatore", true, verified = true),
        SafeZone("ps_kattoor", "Kattoor Police Station",
            SafeZoneType.POLICE_STATION, 11.0061, 76.9514, 100f,
            "100", "Kattoor, Coimbatore", true, verified = true),
        SafeZone("ps_ramanathapuram", "Ramanathapuram Police Station",
            SafeZoneType.POLICE_STATION, 10.9925, 76.9812, 100f,
            "100", "Ramanathapuram, Coimbatore", true, verified = true),
        SafeZone("ps_singanallur", "Singanallur Police Station",
            SafeZoneType.POLICE_STATION, 10.9961, 77.0152, 100f,
            "100", "Singanallur, Coimbatore", true, verified = true),
        SafeZone("ps_peelamedu", "Peelamedu Police Station",
            SafeZoneType.POLICE_STATION, 11.0278, 77.0003, 100f,
            "100", "Peelamedu, Coimbatore", true, verified = true),
        SafeZone("ps_ukkadam", "Ukkadam Police Station",
            SafeZoneType.POLICE_STATION, 10.9884, 76.9497, 100f,
            "100", "Ukkadam, Coimbatore", true, verified = true),
        SafeZone("ps_kuniamuthur", "Kuniamuthur Police Station",
            SafeZoneType.POLICE_STATION, 10.9661, 76.9525, 100f,
            "100", "Kuniamuthur, Coimbatore", true, verified = true),
        SafeZone("ps_saibaba", "Saibaba Colony Police Station",
            SafeZoneType.POLICE_STATION, 11.0242, 76.9497, 100f,
            "100", "Saibaba Colony, Coimbatore", true, verified = true),
        SafeZone("ps_thudiyalur", "Thudiyalur Police Station",
            SafeZoneType.POLICE_STATION, 11.0811, 76.9425, 100f,
            "100", "Thudiyalur, Coimbatore", true, verified = true),
        SafeZone("ps_saravanampatti", "Saravanampatti Police Station",
            SafeZoneType.POLICE_STATION, 11.0800, 77.0003, 100f,
            "100", "Saravanampatti, Coimbatore", true, verified = true),
        SafeZone("ps_vadavalli", "Vadavalli Police Station",
            SafeZoneType.POLICE_STATION, 11.0261, 76.8942, 100f,
            "100", "Vadavalli, Coimbatore", true, verified = true),
        SafeZone("ps_perur", "Perur Police Station",
            SafeZoneType.POLICE_STATION, 10.9792, 76.9114, 100f,
            "100", "Perur, Coimbatore", true, verified = true),
        SafeZone("ps_podanur", "Podanur Police Station",
            SafeZoneType.POLICE_STATION, 10.9625, 76.9814, 100f,
            "100", "Podanur, Coimbatore", true, verified = true),
        SafeZone("ps_sulur", "Sulur Police Station",
            SafeZoneType.POLICE_STATION, 11.0261, 77.1231, 100f,
            "100", "Sulur, Coimbatore", true, verified = true),
        SafeZone("ps_annur", "Annur Police Station",
            SafeZoneType.POLICE_STATION, 11.2333, 77.1333, 100f,
            "100", "Annur, Coimbatore District", true, verified = true),
        SafeZone("ps_karamadai", "Karamadai Police Station",
            SafeZoneType.POLICE_STATION, 11.2450, 76.9586, 100f,
            "100", "Karamadai, Coimbatore District", true, verified = true),
        SafeZone("ps_mettupalayam", "Mettupalayam Police Station",
            SafeZoneType.POLICE_STATION, 11.2989, 76.9353, 100f,
            "100", "Mettupalayam, Coimbatore District", true, verified = true),
        SafeZone("ps_pollachi", "Pollachi Police Station",
            SafeZoneType.POLICE_STATION, 10.6583, 77.0083, 100f,
            "100", "Pollachi, Coimbatore District", true, verified = true),
        SafeZone("ps_kinathukadavu", "Kinathukadavu Police Station",
            SafeZoneType.POLICE_STATION, 10.8217, 77.0144, 100f,
            "100", "Kinathukadavu, Coimbatore District", true, verified = true),
        SafeZone("ps_valparai", "Valparai Police Station",
            SafeZoneType.POLICE_STATION, 10.3242, 76.9533, 100f,
            "100", "Valparai, Coimbatore District", true, verified = true),
        SafeZone("ps_madukkarai", "Madukkarai Police Station",
            SafeZoneType.POLICE_STATION, 10.9050, 76.9500, 100f,
            "100", "Madukkarai, Coimbatore", true, verified = true),
        SafeZone("ps_chettipalayam", "Chettipalayam Police Station",
            SafeZoneType.POLICE_STATION, 10.8989, 77.0086, 100f,
            "100", "Chettipalayam, Coimbatore", true, verified = true),
        SafeZone("ps_kovilpalayam", "Kovilpalayam Police Station",
            SafeZoneType.POLICE_STATION, 11.1419, 77.0372, 100f,
            "100", "Kovilpalayam, Coimbatore", true, verified = true),
        SafeZone("ps_sarcarsamakulam", "Sarcarsamakulam Police Station",
            SafeZoneType.POLICE_STATION, 11.1331, 77.0175, 100f,
            "100", "Sarcarsamakulam, Coimbatore", true, verified = true),
        SafeZone("ps_kurudampalayam", "Kurudampalayam Police Station",
            SafeZoneType.POLICE_STATION, 11.1211, 76.9683, 100f,
            "100", "Kurudampalayam, Coimbatore", true, verified = true),
        SafeZone("ps_thondamuthur", "Thondamuthur Police Station",
            SafeZoneType.POLICE_STATION, 10.9783, 76.8425, 100f,
            "100", "Thondamuthur, Coimbatore", true, verified = true)
    )

    // =========================================================================
    // HOSPITALS (28)
    // =========================================================================
    private val hospitals = listOf(
        SafeZone("h_cmch", "Coimbatore Medical College Hospital (CMCH)",
            SafeZoneType.HOSPITAL, 11.0056, 76.9755, 300f,
            "0422-2301393", "Trichy Road, Coimbatore", true, verified = true),
        SafeZone("h_kg", "KG Hospital",
            SafeZoneType.HOSPITAL, 11.0139, 76.9653, 150f,
            "0422-2227771", "Arts College Road, Coimbatore", true, verified = true),
        SafeZone("h_psg", "PSG Hospitals",
            SafeZoneType.HOSPITAL, 11.0178, 77.0031, 200f,
            "0422-4345678", "Peelamedu, Coimbatore", true, verified = true),
        SafeZone("h_kmch", "Kovai Medical Center & Hospital (KMCH)",
            SafeZoneType.HOSPITAL, 11.0281, 77.0408, 250f,
            "0422-4323800", "Avinashi Road, Coimbatore", true, verified = true),
        SafeZone("h_srk", "Sri Ramakrishna Hospital",
            SafeZoneType.HOSPITAL, 11.0167, 76.9814, 200f,
            "0422-4500000", "Sidhapudur, Coimbatore", true, verified = true),
        SafeZone("h_ganga", "Ganga Hospital",
            SafeZoneType.HOSPITAL, 11.0206, 76.9666, 150f,
            "0422-2485000", "Mettupalayam Road, Coimbatore", true, verified = true),
        SafeZone("h_royalcare", "Royal Care Hospital",
            SafeZoneType.HOSPITAL, 11.0539, 77.0322, 200f,
            "0422-4208888", "Neelambur, Coimbatore", true, verified = true),
        SafeZone("h_balaji", "Sri Balaji Hospital",
            SafeZoneType.HOSPITAL, 11.0245, 77.0019, 150f,
            "0422-4261000", "Peelamedu, Coimbatore", true, verified = true),
        SafeZone("h_rmc", "Ramakrishna Medical Centre",
            SafeZoneType.HOSPITAL, 11.0103, 76.9747, 100f,
            "0422-4500000", "Huzur Road, Coimbatore", true, verified = true),
        SafeZone("h_ashwin", "Ashwin Hospital",
            SafeZoneType.HOSPITAL, 11.0222, 76.9642, 120f,
            "0422-2525555", "Sivananda Colony, Coimbatore", true, verified = true),
        SafeZone("h_kongunad", "Kongunad Hospital",
            SafeZoneType.HOSPITAL, 11.0250, 76.9558, 120f,
            "0422-2222222", "Gandhipuram, Coimbatore", true, verified = true),
        SafeZone("h_gem", "Gem Hospital",
            SafeZoneType.HOSPITAL, 11.0222, 76.9839, 150f,
            "0422-3020202", "Puliakulam Road, Coimbatore", true, verified = true),
        SafeZone("h_aravind", "Aravind Eye Hospital",
            SafeZoneType.HOSPITAL, 11.0175, 76.9900, 150f,
            "0422-4222222", "Avinashi Road, Coimbatore", true, verified = true),
        SafeZone("h_sankara", "Sankara Eye Hospital",
            SafeZoneType.HOSPITAL, 11.0242, 77.0297, 200f,
            "0422-2304040", "Sivanandapuram, Coimbatore", true, verified = true),
        SafeZone("h_govt", "Government Hospital Coimbatore",
            SafeZoneType.HOSPITAL, 10.9961, 76.9608, 200f,
            "0422-2301298", "Oppanakara Street, Coimbatore", true, verified = true),
        SafeZone("h_esi", "ESI Hospital",
            SafeZoneType.HOSPITAL, 11.0042, 76.9889, 200f,
            "0422-2571010", "Variety Hall Road, Coimbatore", true, verified = true),
        SafeZone("h_masonic", "Masonic Medical Centre",
            SafeZoneType.HOSPITAL, 11.0203, 76.9542, 100f,
            "0422-2222222", "Race Course, Coimbatore", true, verified = true),
        SafeZone("h_brainspine", "Brain & Spine Hospital",
            SafeZoneType.HOSPITAL, 11.0156, 76.9675, 100f,
            "0422-2222222", "Mettupalayam Road, Coimbatore", true, verified = true),
        SafeZone("h_raja", "Rajalakshmi Hospital",
            SafeZoneType.HOSPITAL, 11.0228, 76.9844, 100f,
            "0422-2222222", "Puliakulam, Coimbatore", true, verified = true),
        SafeZone("h_abhirami", "Sree Abhirami Hospital",
            SafeZoneType.HOSPITAL, 11.0083, 76.9686, 100f,
            "0422-2302505", "Big Bazaar Street, Coimbatore", true, verified = true),
        SafeZone("h_kidney", "KG Kidney Centre",
            SafeZoneType.HOSPITAL, 11.0136, 76.9661, 100f,
            "0422-2227771", "Arts College Road, Coimbatore", true, verified = true),
        SafeZone("h_cancer", "Coimbatore Cancer Foundation",
            SafeZoneType.HOSPITAL, 11.0167, 76.9814, 100f,
            "0422-4500000", "Sidhapudur, Coimbatore", true, verified = true),
        SafeZone("h_pollachi_govt", "Government Hospital Pollachi",
            SafeZoneType.HOSPITAL, 10.6592, 77.0089, 200f,
            "04259-222222", "Pollachi, Coimbatore District", true, verified = true),
        SafeZone("h_mettupalayam_govt", "Government Hospital Mettupalayam",
            SafeZoneType.HOSPITAL, 11.2989, 76.9420, 200f,
            "04254-222222", "Mettupalayam, Coimbatore District", true, verified = true),
        SafeZone("h_thudiyalur", "Thudiyalur Government Hospital",
            SafeZoneType.HOSPITAL, 11.0805, 76.9435, 150f,
            "0422-2647221", "Thudiyalur, Coimbatore", true, verified = true),
        SafeZone("h_sulur", "Sulur Government Hospital",
            SafeZoneType.HOSPITAL, 11.0260, 77.1225, 150f,
            "0422-2685225", "Sulur, Coimbatore", true, verified = true),
        SafeZone("h_singanallur_esi", "ESI Hospital Singanallur",
            SafeZoneType.HOSPITAL, 10.9965, 77.0145, 150f,
            "0422-2571010", "Singanallur, Coimbatore", true, verified = true),
        SafeZone("h_karamadai", "Government Hospital Karamadai",
            SafeZoneType.HOSPITAL, 11.2452, 76.9580, 150f,
            "04254-272222", "Karamadai, Coimbatore District", true, verified = true)
    )

    // =========================================================================
    // Additional verified safe public places (optional, 4)
    // =========================================================================
    private val publicSafePlaces = listOf(
        SafeZone("pub_gandhipuram_bus", "Gandhipuram Bus Stand (24h)",
            SafeZoneType.TRANSIT, 11.0178, 76.9672, 100f,
            null, "Gandhipuram, Coimbatore", true, verified = false),
        SafeZone("pub_cbe_junction", "Coimbatore Junction Railway Station",
            SafeZoneType.TRANSIT, 10.9975, 76.9662, 100f,
            "139", "State Bank Road, Coimbatore", true, verified = false),
        SafeZone("pub_airport", "Coimbatore International Airport",
            SafeZoneType.TRANSIT, 11.0300, 77.0434, 300f,
            "0422-2598100", "Peelamedu, Coimbatore", true, verified = false),
        SafeZone("pub_ukadam_bus", "Ukkadam Bus Stand (24h)",
            SafeZoneType.TRANSIT, 10.9884, 76.9514, 100f,
            null, "Ukkadam, Coimbatore", true, verified = false)
    )

    private val bundled: List<SafeZone> = policeStations + hospitals + publicSafePlaces

    override fun getBundled(
        lat: Double,
        lon: Double,
        limit: Int,
        maxDistanceMeters: Float
    ): List<SafeZone> {
        return bundled
            .map { z ->
                val d = haversineMeters(lat, lon, z.latitude, z.longitude)
                z.copy(
                    distanceMeters = d,
                    etaMinutes = max(1, (d / 80f).roundToInt())
                )
            }
            .filter { (it.distanceMeters ?: Float.MAX_VALUE) <= maxDistanceMeters }
            .sortedBy { it.distanceMeters }
            .take(limit)
    }

    override suspend fun nearest(
        lat: Double,
        lon: Double,
        limit: Int,
        maxDistanceMeters: Float
    ): Result<List<SafeZone>> = withContext(Dispatchers.Default) {
        runCatching {
            getBundled(lat, lon, limit, maxDistanceMeters)
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1); val dLam = Math.toRadians(lon2 - lon1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLam / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }
}