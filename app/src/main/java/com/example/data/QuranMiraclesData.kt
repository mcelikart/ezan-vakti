package com.example.data

enum class MiracleCategory(val displayName: String, val iconName: String) {
    ALL("Tümü", "Dashboard"),
    RUMUZAT_ESRAR("Rumuzat & Esrar", "AutoAwesome"),
    COSMOLOGY("Evren & Uzay", "Public"),
    GEOLOGY("Yeryüzü & Doğa", "Landscape"),
    BIOLOGY("Biyoloji & İnsan", "Person"),
    PHYSICS_CHEMISTRY("Fizik & Kimya", "Science"),
    ZOOLOGY_BOTANY("Canlılar Alemi", "Pets"),
    MATHEMATICS("Matematik & Sayılar", "Calculate"),
    HISTORY("Tarih & Arkeoloji", "HistoryEdu")
}

data class MiracleItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: MiracleCategory,
    val surahName: String,
    val verseNumber: String,
    val surahIndex: Int, // 1-114
    val verseIndex: Int, // 1-based
    val arabicVerse: String,
    val verseTranslation: String,
    val scientificDiscoveryTitle: String,
    val discoveryCentury: String,
    val scientificExplanation: String,
    val keyScientificFacts: List<String>,
    val quotesAndReferences: List<String>,
    val tags: List<String>
)

object QuranMiraclesRepository {
    private val FEATURED_IDS = setOf(
        "expanding_universe", "iron_sent_down", "protective_sky_roof", "big_bang_ratk_fatk",
        "smoke_primordial_universe", "solar_apex_sun_orbit", "sun_lamp_moon_reflected_light",
        "sure_necm_sirius_cift_yildiz", "pulsar_tarik_yildizi", "black_holes_kunnas",
        "pairs_matter_antimatter", "seas_not_mixing", "internal_waves_darkness_deep_ocean",
        "mountains_as_pegs_roots", "continental_drift_plate_tectonics", "earth_crust_fault_lines",
        "dead_sea_lowest_point", "rain_measured_water_cycle", "clouds_weight_tons",
        "wind_fertilizing_clouds", "frontal_lobe_lying", "embryology_stages_alaq_mudghah",
        "fingerprint_resurrection", "pain_receptors_skin", "atmospheric_pressure_chest_tightening",
        "water_origin_of_all_life", "hearing_before_sight_embryo", "gender_determined_by_male_sperm",
        "bone_formation_before_muscles", "blood_circulation_milk_production", "female_honey_bee_worker",
        "female_spider_web_fragility", "ants_vocal_chemical_communication", "photosynthesis_chlorophyll_green",
        "birds_flight_aerodynamics", "deniz_ve_kara_orani_71_ve_29", "dunya_ve_ahiret_kelime_simetrisi_115",
        "yevm_gun_ve_365_gunes_yili", "sehr_ay_ve_12_ay_takvimi", "kullun_fi_felekin_palindrom",
        "melek_ve_seytan_kelime_simetrisi_88", "cennet_ve_cehennem_kelime_simetrisi_77",
        "firavun_cesedinin_korunmasi", "haman_ve_antik_misir_hiyeroglifleri", "irem_ve_kayip_sehir_ubar",
        "ashabi_kehf_309_yil_ve_gunes_ay_farki"
    )

    val miraclesList: List<MiracleItem> by lazy {
        val rawList = QuranMiraclesRumuzatiSemniye.list +
            QuranMiraclesDeepEsrarTafsir.list +
            QuranMiraclesLinguisticRumuzatSublime.list +
            QuranMiraclesCentury21Discoveries.list +
            QuranMiraclesDeepMedicineGenetics.list +
            QuranMiraclesGeophysicsHydrology.list +
            QuranMiraclesArchaeologyHistoryLostCities.list +
            QuranMiraclesCosmology.list +
            QuranMiraclesGeology.list +
            QuranMiraclesBiology.list +
            QuranMiraclesPhysicsTech.list +
            QuranMiraclesZoologyBotany.list +
            QuranMiraclesMathHistory.list +
            QuranMiraclesDeepCosmology.list +
            QuranMiraclesDeepGeologyOcean.list +
            QuranMiraclesDeepBiologyMedicine.list +
            QuranMiraclesDeepPhysicsChemistry.list +
            QuranMiraclesDeepZoologyEcology.list +
            QuranMiraclesDeepMathHistoryArchaeology.list +
            QuranMiracles100NewPart1AstrophysicsQuantum.list +
            QuranMiracles100NewPart2OceanHydrologyGeology.list +
            QuranMiracles100NewPart3GeneticsNeuroscienceEmbryology.list +
            QuranMiracles100NewPart4ArchaeologyBotanyLinguistics.list +
            QuranMiracles100NewPart5MathematicalSublime.list +
            QuranMiracles100NewPart6CosmicLinguisticBiomimicry.list +
            QuranMiracles100NewPart7AstrophysicsCosmicFineTuning.list +
            QuranMiracles100NewPart8AtmosphericOceanicGeodynamics.list +
            QuranMiracles100NewPart9NeurologyEpigeneticsCellBiology.list +
            QuranMiracles100NewPart10ArchaeologyHistoryCivilizations.list +
            QuranMiracles100NewPart11BotanyZoologyBiomimeticsMath.list +
            QuranMiracles100NewPart12CosmologyRelativityAstrophysics.list +
            QuranMiracles100NewPart13AtmosphericMeteorologyHydrology.list +
            QuranMiracles100NewPart14MedicineImmunologyGenetics.list +
            QuranMiracles100NewPart15ArchaeologyAncientManuscriptsCivilizations.list +
            QuranMiracles100NewPart16BotanyZoologyBiophysicsMath.list

        deduplicateMiracles(rawList)
    }

    private fun deduplicateMiracles(rawList: List<MiracleItem>): List<MiracleItem> {
        val result = mutableListOf<MiracleItem>()
        val seenIds = mutableSetOf<String>()
        val seenSurahVerse = mutableMapOf<String, Int>()
        val seenNormalizedTitles = mutableMapOf<String, Int>()

        for (item in rawList) {
            val id = item.id.trim()
            if (id.isEmpty() || id in seenIds) continue

            val svKey = if (item.surahIndex > 0 && item.verseIndex > 0) "${item.surahIndex}:${item.verseIndex}" else null
            val cleanTitle = item.title.lowercase()
                .filter { it.isLetterOrDigit() }
                .take(20)

            val isFeatured = FEATURED_IDS.contains(id)

            // Deduplicate by surah + verse
            if (svKey != null && seenSurahVerse.containsKey(svKey)) {
                val existingIndex = seenSurahVerse[svKey]!!
                val existingItem = result[existingIndex]
                if (isFeatured && !FEATURED_IDS.contains(existingItem.id)) {
                    seenIds.remove(existingItem.id)
                    result[existingIndex] = item
                    seenIds.add(id)
                }
                continue
            }

            // Deduplicate by normalized title
            if (cleanTitle.isNotEmpty() && seenNormalizedTitles.containsKey(cleanTitle)) {
                val existingIndex = seenNormalizedTitles[cleanTitle]!!
                val existingItem = result[existingIndex]
                if (isFeatured && !FEATURED_IDS.contains(existingItem.id)) {
                    seenIds.remove(existingItem.id)
                    result[existingIndex] = item
                    seenIds.add(id)
                }
                continue
            }

            val newIndex = result.size
            result.add(item)
            seenIds.add(id)
            if (svKey != null) {
                seenSurahVerse[svKey] = newIndex
            }
            if (cleanTitle.isNotEmpty()) {
                seenNormalizedTitles[cleanTitle] = newIndex
            }
        }
        return result
    }
}
