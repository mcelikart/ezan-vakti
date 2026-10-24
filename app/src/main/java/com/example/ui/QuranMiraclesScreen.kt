package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MiracleCategory
import com.example.data.MiracleItem
import com.example.data.QuranMiraclesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranMiraclesScreen(
    viewModel: PrayerViewModel,
    onNavigateToQuranReading: () -> Unit = {},
    onBack: () -> Unit
) {
    KeepScreenOn()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var rawSearchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MiracleCategory.ALL) }
    var showOnlyBookmarked by remember { mutableStateOf(false) }
    var selectedMiracleDetail by remember { mutableStateOf<MiracleItem?>(null) }
    var textScale by remember { mutableFloatStateOf(1.0f) }

    // Fast Debounce for search (120ms) for smooth 60/120fps typing
    LaunchedEffect(rawSearchQuery) {
        if (rawSearchQuery.isBlank()) {
            debouncedSearchQuery = ""
        } else {
            kotlinx.coroutines.delay(120)
            debouncedSearchQuery = rawSearchQuery.trim().lowercase()
        }
    }

    // Persistent Bookmarks in SharedPreferences
    val prefs = remember { context.getSharedPreferences("quran_miracles_prefs", Context.MODE_PRIVATE) }
    var bookmarkedIds by remember {
        mutableStateOf(prefs.getStringSet("bookmarked_miracles", emptySet()) ?: emptySet())
    }

    val toggleBookmark: (String) -> Unit = remember {
        { id ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val newSet = if (bookmarkedIds.contains(id)) {
                bookmarkedIds - id
            } else {
                bookmarkedIds + id
            }
            bookmarkedIds = newSet
            prefs.edit().putStringSet("bookmarked_miracles", newSet).apply()
        }
    }

    val allMiracles = remember { QuranMiraclesRepository.miraclesList }
    val allCategories = remember { MiracleCategory.values() }

    val targetMiracleId by viewModel.targetMiracleId.collectAsState()
    LaunchedEffect(targetMiracleId) {
        targetMiracleId?.let { id ->
            val found = allMiracles.find { it.id == id }
            if (found != null) {
                selectedMiracleDetail = found
            }
            viewModel.clearTargetMiracleId()
        }
    }

    // High performance search & filter with instant short-circuiting
    val filteredMiracles = remember(debouncedSearchQuery, selectedCategory, showOnlyBookmarked, bookmarkedIds) {
        val query = debouncedSearchQuery
        val list = allMiracles.filter { item ->
            if (showOnlyBookmarked && !bookmarkedIds.contains(item.id)) return@filter false
            if (selectedCategory != MiracleCategory.ALL && item.category != selectedCategory) return@filter false
            if (query.isEmpty()) return@filter true

            item.title.contains(query, ignoreCase = true) ||
            item.subtitle.contains(query, ignoreCase = true) ||
            item.surahName.contains(query, ignoreCase = true) ||
            item.verseTranslation.contains(query, ignoreCase = true) ||
            item.tags.any { it.contains(query, ignoreCase = true) } ||
            item.scientificDiscoveryTitle.contains(query, ignoreCase = true)
        }

        // When in "Tümü" (ALL) and not searching, place the most interesting discoveries at the top
        if (selectedCategory == MiracleCategory.ALL && query.isEmpty() && !showOnlyBookmarked) {
            list.sortedByDescending { getMiracleInterestScore(it) }
        } else {
            list
        }
    }

    // Category count lookup map
    val categoryCounts = remember(allMiracles) {
        allMiracles.groupBy { it.category }.mapValues { it.value.size }
    }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 4 }
    }

    // When a miracle is selected, render full MiracleDetailScreen
    selectedMiracleDetail?.let { miracle ->
        MiracleDetailScreen(
            item = miracle,
            isBookmarked = bookmarkedIds.contains(miracle.id),
            textScale = textScale,
            onTextScaleChange = { textScale = it },
            onBookmarkToggle = { toggleBookmark(miracle.id) },
            onNavigateToQuran = {
                viewModel.setTargetQuranVerse(miracle.surahIndex, miracle.verseIndex)
                selectedMiracleDetail = null
                onNavigateToQuranReading()
            },
            onBack = { selectedMiracleDetail = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Kur'an Mucizeleri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${allMiracles.size} İlmi Keşif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "14 Asır Önceki Âyetler & Modern Bilimsel Gerçekler",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // Bookmark Filter Icon
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showOnlyBookmarked = !showOnlyBookmarked
                        }
                    ) {
                        Icon(
                            imageVector = if (showOnlyBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Kaydedilenler",
                            tint = if (showOnlyBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Random Miracle Discover Action Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val randomItem = allMiracles.random()
                            selectedMiracleDetail = randomItem
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Rastgele Keşfet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Yukarı Çık")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = com.example.ui.theme.navBarBottomPadding(extra = 32.dp))
        ) {
            // 1. Sleek Search Bar
            item(key = "search_field") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = rawSearchQuery,
                        onValueChange = { rawSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("419 mucize arasında konu, âyet veya bilimsel terim ara...", fontSize = 12.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (rawSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    rawSearchQuery = ""
                                    debouncedSearchQuery = ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // 2. Categories Horizontal Filter Chips with Count Badges
            item(key = "categories_filter") {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategories, key = { it.name }) { category ->
                        val isSelected = selectedCategory == category
                        val count = if (category == MiracleCategory.ALL) allMiracles.size else (categoryCounts[category] ?: 0)
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = category
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = category.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            },
                            leadingIcon = {
                                val icon = getMiracleCategoryIcon(category)
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // 3. Section Header & Results Count
            item(key = "header_count") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (showOnlyBookmarked) "Kaydedilen Başlıklar" else "Mucize Başlıkları (${if (selectedCategory == MiracleCategory.ALL) "Tümü" else selectedCategory.displayName})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${filteredMiracles.size} Başlık",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 4. Empty State or List of Cards
            if (filteredMiracles.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (showOnlyBookmarked) Icons.Default.BookmarkBorder else Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (showOnlyBookmarked) "Henüz kaydedilmiş bir mucize bulunmuyor." else "Aramanızla eşleşen mucize başlığı bulunamadı.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredMiracles,
                    key = { it.id },
                    contentType = { "miracle_card" }
                ) { item ->
                    MiracleTitleCardItem(
                        item = item,
                        isBookmarked = bookmarkedIds.contains(item.id),
                        onBookmarkToggle = { toggleBookmark(item.id) },
                        onClick = { selectedMiracleDetail = item }
                    )
                }
            }
        }
    }
}

/**
 * Priority list of the most scientifically interesting and astonishing Quranic miracles
 */
private val FEATURED_INTERESTING_MIRACLE_IDS = listOf(
    // 1. Evren, Kozmoloji ve Astrofizik
    "expanding_universe",                        // Göğün Genişletilmesi (Hubble 1929) - Zâriyât 47
    "iron_sent_down",                            // Demirin Gökten İndirilmesi (Süpernova Çekirdeği) - Hadîd 25
    "protective_sky_roof",                       // Korunmuş Tavan (Atmosfer & Manyetosfer) - Enbiyâ 32
    "big_bang_ratk_fatk",                        // Big Bang & Ratk-Fatk (Evrenin Bitişikken Ayrılması) - Enbiyâ 30
    "smoke_primordial_universe",                 // Evrenin Başlangıçta Duman/Gaz Hali - Fussilet 11
    "solar_apex_sun_orbit",                      // Güneş'in Kendi Yörüngesinde Akışı (Solar Apex) - Yâsîn 38
    "sun_lamp_moon_reflected_light",             // Güneş Kandil/Işık Kaynağı, Ay Nurdur/Yansıtıcı - Furkân 61
    "sure_necm_sirius_cift_yildiz",              // Sirius Çift Yıldızı ve 49.9 Yıllık Yörünge Periyodu - Necm 49/9
    "pulsar_tarik_yildizi",                      // Tarık Yıldızı: Vuran / Nabız Atan Yıldız (Pulsar) - Târık 1-3
    "black_holes_kunnas",                        // Karadelikler: Süpürücü ve Gizlenen Yıldızlar - Tekvîr 15-16
    "pairs_matter_antimatter",                   // Her Şeyin Çift Yaratılışı (Madde & Antimadde) - Zâriyât 49
    
    // 2. Denizler, Okyanuslar ve Jeoloji
    "seas_not_mixing",                           // Birbirine Karışmayan Denizler (Haloklin Bariyeri) - Rahmân 19-20
    "internal_waves_darkness_deep_ocean",        // Derin Okyanus İç Dalgaları ve Kat Kat Karanlıklar - Nûr 40
    "mountains_as_pegs_roots",                   // Dağların Kazık Kökleri (İzostasi Dengesi) - Nebe 6-7
    "continental_drift_plate_tectonics",         // Dağların Bulutlar Gibi Yürümesi (Levha Tektoniği) - Neml 88
    "earth_crust_fault_lines",                   // Yeryüzünün Çatlaklı Fay Hatları - Târık 12
    "dead_sea_lowest_point",                     // Dünyanın En Alçak Noktası (Lut Gölü Havzası -430m) - Rûm 1-4
    "rain_measured_water_cycle",                 // Yağmurun Belli Bir Ölçü ile İndirilmesi - Zuhruf 11
    "clouds_weight_tons",                        // Bulutların Tonlarca Ağırlıkta Olması - Ra'd 12
    "wind_fertilizing_clouds",                   // Rüzgarların Aşılayıcı Olması (Bulut & Bitkiler) - Hicr 22
    
    // 3. Tıp, Embriyoloji ve İnsan Biyolojisi
    "frontal_lobe_lying",                        // Alın Bölgesi (Prefrontal Korteks): Yalan ve Karar Merkezi - Alak 15-16
    "embryology_stages_alaq_mudghah",            // Anne Karnındaki Embriyo Evreleri (Alaka, Mudga) - Mü'minûn 12-14
    "fingerprint_resurrection",                  // Parmak İzi ve Dermatoglif Çizgileri - Kıyâme 3-4
    "pain_receptors_skin",                       // Derideki Ağrı Reseptörleri (Nosiseptörler) - Nisâ 56
    "atmospheric_pressure_chest_tightening",     // Göğe Yükseldikçe Göğsün Daralması (Hipoksi) - En'âm 125
    "water_origin_of_all_life",                  // Bütün Canlıların Sudan Yaratılması (%80 Sitoplazma) - Enbiyâ 30
    "hearing_before_sight_embryo",               // Anne Karnında İşitmenin Görmeden Önce Gelişmesi - Secde 9
    "gender_determined_by_male_sperm",           // Cinsiyetin Sperm Tarafından Belirlenmesi (XY) - Necm 45-46
    "bone_formation_before_muscles",             // Kemiklerin Oluşup Sonra Kasla Kaplanması - Mü'minûn 14
    "blood_circulation_milk_production",         // Kan Dolaşımı ve Süt Oluşumu - Nahl 66
    
    // 4. Canlılar Alemi (Zooloji & Botanik)
    "female_honey_bee_worker",                   // Bal Arısının Dişi Olarak Kodlanması (İşçi Arı) - Nahl 68-69
    "female_spider_web_fragility",               // Örümcek Ağının Dişi Tarafından Örülmesi - Ankebût 41
    "ants_vocal_chemical_communication",         // Karıncaların Sesli & Kimyasal İletişimi - Neml 18
    "photosynthesis_chlorophyll_green",          // Fotosentez ve Yeşil Madde (Ahdar / Klorofil) - Yâsîn 80
    "birds_flight_aerodynamics",                 // Kuşların Havada Tutulması ve Aerodinamik - Mülk 19
    
    // 5. Matematik ve Kelime Simetrileri
    "deniz_ve_kara_orani_71_ve_29",              // Deniz ve Kara Kelimelerinin Oranı: %71.11 Deniz, %28.89 Kara
    "dunya_ve_ahiret_kelime_simetrisi_115",      // Dünya ve Ahiret Eşitliği: 115 / 115
    "yevm_gun_ve_365_gunes_yili",                // "Gün" (Yevm) Kelimesinin 365 Kez Geçmesi
    "sehr_ay_ve_12_ay_takvimi",                  // "Ay" (Şehr) Kelimesinin 12 Kez Geçmesi
    "kullun_fi_felekin_palindrom",               // Küllün fî Felek Palindromu (Yörünge Simetrisi) - Yâsîn 40
    "melek_ve_seytan_kelime_simetrisi_88",       // Melek ve Şeytan Eşitliği (88 / 88)
    "cennet_ve_cehennem_kelime_simetrisi_77",    // Cennet ve Cehennem Eşitliği (77 / 77)
    
    // 6. Tarih ve Arkeolojik Keşifler
    "firavun_cesedinin_korunmasi",               // Firavun Cesedinin İbret Olarak Korunması (Merneptah) - Yûnus 92
    "haman_ve_antik_misir_hiyeroglifleri",       // Haman İsmi ve Antik Mısır Hiyeroglif Keşfi - Kasas 38
    "irem_ve_kayip_sehir_ubar",                  // İrem Şehri ve Yüksek Sütunlar (NASA Uzay Taraması) - Fecr 6-8
    "ashabi_kehf_309_yil_ve_gunes_ay_farki"      // Ashab-ı Kehf 300 Güneş = 309 Ay Yılı Dönüşümü - Kehf 25
)

/**
 * Calculates interest score to rank the most astonishing miracles at the top of the "ALL" view
 */
private fun getMiracleInterestScore(item: MiracleItem): Int {
    val priorityIndex = FEATURED_INTERESTING_MIRACLE_IDS.indexOf(item.id)
    if (priorityIndex != -1) {
        return 10000 - priorityIndex
    }

    val title = item.title.lowercase()
    return when {
        title.contains("genişleyen") || title.contains("evrenin genişlemesi") -> 9500
        title.contains("demir") && (title.contains("gökten") || title.contains("süpernova")) -> 9400
        title.contains("deniz") && (title.contains("karışma") || title.contains("engel") || title.contains("bariyer")) -> 9300
        title.contains("alın") || title.contains("prefrontal") || title.contains("yalan") -> 9200
        title.contains("korunmuş tavan") || title.contains("van allen") || title.contains("atmosfer") -> 9100
        title.contains("dağ") && (title.contains("kazık") || title.contains("kök") || title.contains("izostasi")) -> 9000
        title.contains("embriyo") || title.contains("alaka") || title.contains("mudga") -> 8900
        title.contains("deniz ve kara") || (title.contains("71") && title.contains("28")) -> 8800
        title.contains("dünya ve ahiret") || (title.contains("115") && title.contains("simetri")) -> 8700
        title.contains("sirius") || title.contains("şira") -> 8600
        title.contains("iç dalga") || title.contains("kat kat karanlık") -> 8500
        title.contains("parmak izi") -> 8400
        title.contains("ağrı") && title.contains("reseptör") -> 8300
        title.contains("tarık") || title.contains("pulsar") -> 8200
        title.contains("karadelik") || title.contains("hunnas") -> 8100
        title.contains("lut gölü") || title.contains("en alçak") -> 8000
        title.contains("firavun") && title.contains("cesed") -> 7900
        title.contains("bal arı") || title.contains("dişi arı") -> 7800
        title.contains("karınca") && title.contains("iletişim") -> 7700
        title.contains("örümcek") && title.contains("dişi") -> 7600
        title.contains("küllün fî felek") || title.contains("palindrom") -> 7500
        title.contains("levha tektoniği") || title.contains("dağların yürümesi") -> 7300
        title.contains("fotosentez") || title.contains("klorofil") -> 7200
        title.contains("irem") || title.contains("ubar") -> 7100
        title.contains("haman") && title.contains("hiyeroglif") -> 7000
        title.contains("madde ve antimadde") || title.contains("çift yaratılış") -> 6900
        else -> 0
    }
}

private fun getMiracleCategoryIcon(category: MiracleCategory): ImageVector {
    return when (category) {
        MiracleCategory.ALL -> Icons.Default.GridView
        MiracleCategory.RUMUZAT_ESRAR -> Icons.Default.AutoAwesome
        MiracleCategory.COSMOLOGY -> Icons.Default.Public
        MiracleCategory.GEOLOGY -> Icons.Default.Landscape
        MiracleCategory.BIOLOGY -> Icons.Default.Person
        MiracleCategory.PHYSICS_CHEMISTRY -> Icons.Default.Science
        MiracleCategory.ZOOLOGY_BOTANY -> Icons.Default.Pets
        MiracleCategory.MATHEMATICS -> Icons.Default.Calculate
        MiracleCategory.HISTORY -> Icons.Default.HistoryEdu
    }
}

/**
 * Title-Only Clean List Item Card (Without verse text in list, opens details on click)
 */
@Composable
private fun MiracleTitleCardItem(
    item: MiracleItem,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onClick: () -> Unit
) {
    val categoryColor = when (item.category) {
        MiracleCategory.COSMOLOGY -> Color(0xFF1E3A8A)
        MiracleCategory.BIOLOGY -> Color(0xFF047857)
        MiracleCategory.GEOLOGY -> Color(0xFFB45309)
        MiracleCategory.PHYSICS_CHEMISTRY -> Color(0xFF6D28D9)
        MiracleCategory.HISTORY -> Color(0xFFBE185D)
        MiracleCategory.RUMUZAT_ESRAR -> Color(0xFF0F766E)
        MiracleCategory.MATHEMATICS -> Color(0xFF4338CA)
        MiracleCategory.ZOOLOGY_BOTANY -> Color(0xFF15803D)
        MiracleCategory.ALL -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon Badge with thematic category color
            val categoryIcon = getMiracleCategoryIcon(item.category)

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Title & Badges
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.category.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• ${item.surahName} (${item.verseNumber})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.title,
                    fontSize = 14.5.sp,
                    lineHeight = 19.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.subtitle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bookmark Action & Forward Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Kaydet",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Detay",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Rich Detail Screen containing Full Ayah, Translation, Quran Jump Button, Scientific Analysis & References
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiracleDetailScreen(
    item: MiracleItem,
    isBookmarked: Boolean,
    textScale: Float,
    onTextScaleChange: (Float) -> Unit,
    onBookmarkToggle: () -> Unit,
    onNavigateToQuran: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kur'an Mucizesi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.surahName} Sûresi, ${item.verseNumber}. Âyet",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                    actions = {
                        // Font Zoom Buttons
                        IconButton(
                            onClick = {
                                if (textScale > 0.85f) onTextScaleChange(textScale - 0.1f)
                            }
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        IconButton(
                            onClick = {
                                if (textScale < 1.4f) onTextScaleChange(textScale + 0.1f)
                            }
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        // Bookmark Action
                        IconButton(onClick = onBookmarkToggle) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Kaydet",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Share Action
                        IconButton(
                            onClick = {
                                val fullText = """
                                    📖 Kur'an Mucizesi: ${item.title}
                                    
                                    📌 ${item.surahName}, ${item.verseNumber}
                                    ${item.arabicVerse}
                                    
                                    Meal: "${item.verseTranslation}"
                                    
                                    🔬 ${item.scientificDiscoveryTitle} (${item.discoveryCentury}):
                                    ${item.scientificExplanation}
                                    
                                    💡 Önemli Bilgiler:
                                    ${item.keyScientificFacts.joinToString("\n• ", prefix = "• ")}
                                    
                                    📚 Kaynaklar:
                                    ${item.quotesAndReferences.joinToString("\n- ", prefix = "- ")}
                                """.trimIndent()

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, fullText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Mucize Detayını Paylaş"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Paylaş")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val copyText = "${item.title}\n${item.surahName}, ${item.verseNumber}: \"${item.verseTranslation}\"\n\n${item.scientificExplanation}"
                                clipboardManager.setText(AnnotatedString(copyText))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "Metin panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kopyala", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = onNavigateToQuran,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kur'an'da Oku", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 0. Ana Başlık ve Açıklama Kartı (Tam Okunabilir & Sığan Tasarım)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Category & Discovery Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        val catIcon = getMiracleCategoryIcon(item.category)
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = item.category.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Keşif: ${item.discoveryCentury}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Full Miracle Title - Completely readable, responsive, non-truncated
                            Text(
                                text = item.title,
                                fontSize = (18 * textScale).sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (25 * textScale).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (item.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.subtitle,
                                    fontSize = (13 * textScale).sp,
                                    lineHeight = (19 * textScale).sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                // 1. Ayet Kartı (Arapça & Türkçe Meal)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${item.surahName} (${item.surahIndex}. Sûre), ${item.verseNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Arapça Metin
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.arabicVerse,
                                    fontSize = (21 * textScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    lineHeight = (32 * textScale).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Türkçe Meal
                            Text(
                                text = "Ayet Meali:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"${item.verseTranslation}\"",
                                fontSize = (14 * textScale).sp,
                                lineHeight = (20 * textScale).sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 2. Direct Prominent Quran Jump Action Button
                item {
                    Button(
                        onClick = onNavigateToQuran,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kur'an-ı Kerim'de Bu Ayete Git (${item.surahName})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 3. Bilimsel Keşif ve Araştırma Makalesi
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Biotech,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Bilimsel Keşif & Analiz",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.scientificDiscoveryTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )

                            Text(
                                text = item.scientificExplanation,
                                fontSize = (13.5f * textScale).sp,
                                lineHeight = (22 * textScale).sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // 4. Önemli Bilimsel Vurgular Kartı
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Kritik Bilimsel Noktalar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            item.keyScientificFacts.forEach { fact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Text(
                                        text = fact,
                                        fontSize = (12.5f * textScale).sp,
                                        lineHeight = (18 * textScale).sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Akademik Kaynaklar ve Alıntılar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Akademik & Tefsir Kaynakları",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            item.quotesAndReferences.forEach { ref ->
                                Text(
                                    text = "• $ref",
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
