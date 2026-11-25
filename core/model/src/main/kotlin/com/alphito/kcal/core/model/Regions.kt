package com.alphito.kcal.core.model

/**
 * Cuisine clusters used by the food catalogue. A user picks a *country* (ISO 3166 code); the plan
 * draws from that country's dishes, its cluster's dishes and the global staples.
 */
enum class CuisineRegion(val key: String) {
    SouthAsia("south_asia"),
    MiddleEast("middle_east"),
    NorthAfrica("north_africa"),
    TurkiyeCaucasus("turkiye_caucasus"),
    Europe("europe"),
    Cis("cis"),
    EastAsia("east_asia"),
    SoutheastAsia("southeast_asia"),
    LatinAmerica("latin_america"),
    NorthAmericaOceania("north_america_oceania"),
    SubSaharanAfrica("sub_saharan_africa"),
}

object Regions {
    const val DEFAULT_COUNTRY = "PK"

    private val countries: Map<String, CuisineRegion> = buildMap {
        fun put(region: CuisineRegion, vararg codes: String) = codes.forEach { put(it, region) }
        put(CuisineRegion.SouthAsia, "PK", "IN", "BD", "NP", "LK", "AF", "BT", "MV")
        put(CuisineRegion.MiddleEast, "SA", "AE", "QA", "KW", "BH", "OM", "YE", "JO", "LB", "SY", "IQ", "PS", "IR")
        put(CuisineRegion.NorthAfrica, "EG", "MA", "DZ", "TN", "LY", "SD", "MR")
        put(CuisineRegion.TurkiyeCaucasus, "TR", "AZ", "GE", "AM", "CY")
        put(
            CuisineRegion.Europe,
            "GB", "IE", "DE", "FR", "NL", "BE", "LU", "AT", "CH", "IT", "ES", "PT", "GR", "SE", "NO", "DK", "FI", "IS",
            "PL", "CZ", "SK", "HU", "RO", "BG", "HR", "SI", "RS", "BA", "ME", "MK", "AL", "XK", "EE", "LV", "LT", "MT",
        )
        put(CuisineRegion.Cis, "RU", "UA", "BY", "MD", "KZ", "UZ", "KG", "TJ", "TM")
        put(CuisineRegion.EastAsia, "CN", "HK", "MO", "TW", "JP", "KR", "MN")
        put(CuisineRegion.SoutheastAsia, "ID", "MY", "SG", "BN", "PH", "TH", "VN", "KH", "LA", "MM", "TL")
        put(
            CuisineRegion.LatinAmerica,
            "MX", "GT", "BZ", "HN", "SV", "NI", "CR", "PA", "CO", "VE", "EC", "PE", "BO", "CL", "AR", "UY", "PY", "BR",
            "CU", "DO", "PR", "HT", "JM", "TT",
        )
        put(CuisineRegion.NorthAmericaOceania, "US", "CA", "AU", "NZ", "FJ", "PG")
        put(
            CuisineRegion.SubSaharanAfrica,
            "NG", "GH", "CI", "SN", "GM", "SL", "LR", "GN", "ML", "BF", "NE", "TG", "BJ", "CM", "TD", "CF", "GA", "CG",
            "CD", "AO", "ET", "ER", "DJ", "SO", "KE", "UG", "TZ", "RW", "BI", "SS", "MW", "ZM", "ZW", "MZ", "MG", "ZA",
            "NA", "BW", "LS", "SZ", "MU",
        )
    }

    /** Every supported country code, for the picker (display names come from the device locale). */
    val supportedCountries: List<String> get() = countries.keys.toList()

    fun isSupported(code: String): Boolean = code.uppercase() in countries

    fun regionOf(country: String): CuisineRegion = countries[country.uppercase()] ?: CuisineRegion.SouthAsia

    /**
     * Profiles created before the country picker stored "Pakistan — Lahore" style values.
     * Anything that is not a supported ISO code falls back to the default country.
     */
    fun normalize(stored: String?): String =
        stored?.trim()?.uppercase()?.takeIf { it.length == 2 && it in countries } ?: DEFAULT_COUNTRY

    /** Is a catalogue dish (with cluster/country tags) native to this country's cuisine? */
    fun isLocal(food: Food, country: String): Boolean =
        food.regions.any { it == country.uppercase() || it == regionOf(country).key }

    /** Global staples + dishes of the user's cluster or country. */
    fun isAvailable(food: Food, country: String): Boolean = food.isGlobal || isLocal(food, country)
}
