package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.DateRange
import com.example.data.TimingsData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: PrayerViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("İmsakiye", "Dini Günler")

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Takvim",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTab == 0) {
            ImsakiyeTab(viewModel)
        } else {
            DiniGunlerTab()
        }
    }
}

@Composable
fun ImsakiyeTab(viewModel: PrayerViewModel) {
    val calendar by viewModel.calendar.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading && calendar == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Yükleniyor...", color = MaterialTheme.colorScheme.onBackground)
        }
    } else if (calendar.isNullOrEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("İmsakiye bilgisi bulunamadı.", color = MaterialTheme.colorScheme.onBackground)
        }
    } else {
        val todayStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date()) }
        val todayIndex = remember(calendar) {
            calendar?.indexOfFirst { it.date.readable == todayStr } ?: -1
        }
        val targetIndex = remember(todayIndex) {
            if (todayIndex > 0) todayIndex - 1 else 0
        }
        
        val listState = androidx.compose.foundation.lazy.rememberLazyListState(
            initialFirstVisibleItemIndex = targetIndex
        )
        
        androidx.compose.runtime.LaunchedEffect(targetIndex) {
            if (targetIndex >= 0 && listState.firstVisibleItemIndex != targetIndex) {
                listState.scrollToItem(targetIndex)
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                // Reserve the floating bar so the last card can scroll clear.
                bottom = com.example.ui.theme.navBarBottomPadding(extra = 8.dp)
            )
        ) {
            val daysList = calendar ?: emptyList()
            items(daysList) { dayData ->
                val isToday = dayData.date.readable == todayStr
                CalendarItem(dayData, isToday)
            }
        }
    }
}

data class ReligiousDay(val name: String, val date: String, val description: String = "")
data class ReligiousYear(val year: String, val days: List<ReligiousDay>)

val religiousYears = listOf(
    ReligiousYear(
        "2026",
        listOf(
            ReligiousDay("Miraç Kandili", "16 Ocak 2026", "Peygamber Efendimiz'in (s.a.v.) ilahi huzura yükseltildiği ve 5 vakit namazın farz kılındığı mübarek gecedir."),
            ReligiousDay("Berat Kandili", "3 Şubat 2026", "Şaban ayının 15. gecesidir. Af, mağfiret ve amellerin takdir edildiği arınma gecesidir."),
            ReligiousDay("Ramazan Başlangıcı", "18 Şubat 2026", "On iki ayın sultanı Ramazan-ı Şerif'in ve farz olan oruç ibadetinin ilk günüdür."),
            ReligiousDay("Kadir Gecesi", "16 Mart 2026", "Kur'an-ı Kerim'in indirilmeye başlandığı, bin aydan daha hayırlı olduğu bildirilen en mübarek gecedir."),
            ReligiousDay("Ramazan Bayramı Arefesi", "19 Mart 2026", "Ramazan ayının son günü; dua, ibadet ve bayram hazırlıklarının doruk noktasıdır."),
            ReligiousDay("Ramazan Bayramı (1. Gün)", "20 Mart 2026", "Bir ay süren oruç ibadetinin ardından Müslümanların şükürle kutladığı sevinç bayramıdır."),
            ReligiousDay("Kurban Bayramı Arefesi", "26 Mayıs 2026", "Hacıların Arafat'ta vakfeye durduğu, teşrik tekbirlerinin başladığı mübarek gündür."),
            ReligiousDay("Kurban Bayramı (1. Gün)", "27 Mayıs 2026", "Hz. İbrahim ve Hz. İsmail'in teslimiyetini simgeleyen, kurban ibadetinin eda edildiği bayram günüdür."),
            ReligiousDay("Hicri Yılbaşı", "16 Haziran 2026", "Hz. Muhammed'in (s.a.v.) Mekke'den Medine'ye hicretini esas alan Hicri 1448 yılının ilk günüdür (1 Muharrem)."),
            ReligiousDay("Aşure Günü", "25 Haziran 2026", "Muharrem ayının 10. günüdür. Birçok peygamberin kurtuluşa erdiği, birlik ve bereket günüdür."),
            ReligiousDay("Mevlid Kandili", "24 Ağustos 2026", "Peygamber Efendimiz Hz. Muhammed'in (s.a.v.) dünyaya teşrif ettiği kutlu doğum gecesidir."),
            ReligiousDay("Üç Ayların Başlangıcı", "10 Aralık 2026", "Recep, Şaban ve Ramazan aylarını kapsayan bereketli üç ayların (1 Recep 1448) başlangıcıdır."),
            ReligiousDay("Regaib Kandili", "10 Aralık 2026", "Recep ayının ilk cuma gecesidir. Rahmet ve bereket Kapılarının açıldığı, bağışlanma gecesidir.")
        )
    ),
    ReligiousYear(
        "2027",
        listOf(
            ReligiousDay("Miraç Kandili", "5 Ocak 2027", "Peygamber Efendimiz'in (s.a.v.) ilahi huzura yükseltildiği ve 5 vakit namazın farz kılındığı mübarek gecedir."),
            ReligiousDay("Berat Kandili", "23 Ocak 2027", "Şaban ayının 15. gecesidir. Af, mağfiret ve amellerin takdir edildiği arınma gecesidir."),
            ReligiousDay("Ramazan Başlangıcı", "8 Şubat 2027", "On iki ayın sultanı Ramazan-ı Şerif'in ve farz olan oruç ibadetinin ilk günüdür."),
            ReligiousDay("Kadir Gecesi", "6 Mart 2027", "Kur'an-ı Kerim'in indirilmeye başlandığı, bin aydan daha hayırlı olduğu bildirilen en mübarek gecedir."),
            ReligiousDay("Ramazan Bayramı Arefesi", "8 Mart 2027", "Ramazan ayının son günü; dua, ibadet ve bayram hazırlıklarının doruk noktasıdır."),
            ReligiousDay("Ramazan Bayramı (1. Gün)", "9 Mart 2027", "Bir ay süren oruç ibadetinin ardından Müslümanların şükürle kutladığı sevinç bayramıdır."),
            ReligiousDay("Kurban Bayramı Arefesi", "15 Mayıs 2027", "Hacıların Arafat'ta vakfeye durduğu, teşrik tekbirlerinin başladığı mübarek gündür."),
            ReligiousDay("Kurban Bayramı (1. Gün)", "16 Mayıs 2027", "Hz. İbrahim ve Hz. İsmail'in teslimiyetini simgeleyen, kurban ibadetinin eda edildiği bayram günüdür."),
            ReligiousDay("Hicri Yılbaşı", "5 Haziran 2027", "Hz. Muhammed'in (s.a.v.) Mekke'den Medine'ye hicretini esas alan Hicri 1449 yılının ilk günüdür (1 Muharrem)."),
            ReligiousDay("Aşure Günü", "14 Haziran 2027", "Muharrem ayının 10. günüdür. Birçok peygamberin kurtuluşa erdiği, birlik ve bereket günüdür."),
            ReligiousDay("Mevlid Kandili", "13 Ağustos 2027", "Peygamber Efendimiz Hz. Muhammed'in (s.a.v.) dünyaya teşrif ettiği kutlu doğum gecesidir."),
            ReligiousDay("Üç Ayların Başlangıcı", "30 Kasım 2027", "Recep, Şaban ve Ramazan aylarını kapsayan bereketli üç ayların başlangıcıdır."),
            ReligiousDay("Regaib Kandili", "30 Aralık 2027", "Recep ayının ilk cuma gecesidir. Rahmet ve bereket kapılarının açıldığı, bağışlanma gecesidir.")
        )
    )
)

fun parseReligiousDate(dateString: String): Date? {
    val months = mapOf(
        "Ocak" to 1, "Şubat" to 2, "Mart" to 3, "Nisan" to 4,
        "Mayıs" to 5, "Haziran" to 6, "Temmuz" to 7, "Ağustos" to 8,
        "Eylül" to 9, "Ekim" to 10, "Kasım" to 11, "Aralık" to 12
    )
    return try {
        val parts = dateString.split(" ")
        val day = parts[0].toInt()
        val monthStr = parts[1]
        val year = parts[2].toInt()
        val month = months[monthStr] ?: return null
        
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.time
    } catch (e: Exception) {
        null
    }
}

sealed class TimelineNode {
    data class YearHeader(val year: String) : TimelineNode()
    data class DayEntry(val day: ReligiousDay, val isLast: Boolean) : TimelineNode()
}

@Composable
fun DiniGunlerTab() {
    val now = remember { Date() }
    
    val timelineData = remember {
        val list = mutableListOf<TimelineNode>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val filteredYears = religiousYears.filter { 
            try {
                it.year.toInt() >= currentYear
            } catch (e: Exception) {
                true
            }
        }
        for (year in filteredYears) {
            list.add(TimelineNode.YearHeader(year.year))
            for (i in year.days.indices) {
                list.add(TimelineNode.DayEntry(year.days[i], i == year.days.size - 1))
            }
        }
        list
    }
    
    val nextDayInfo = remember {
        var closestDay: ReligiousDay? = null
        var closestDate: Date? = null
        var minDiff = Long.MAX_VALUE
        var closestIndex = -1
        
        val calNow = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        for (i in timelineData.indices) {
            val node = timelineData[i]
            if (node is TimelineNode.DayEntry) {
                val d = parseReligiousDate(node.day.date)
                if (d != null) {
                    val calTarget = Calendar.getInstance().apply { 
                        time = d
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val diff = calTarget.timeInMillis - calNow.timeInMillis
                    if (diff >= 0 && diff < minDiff) {
                        minDiff = diff
                        closestDay = node.day
                        closestDate = d
                        closestIndex = i
                    }
                }
            }
        }
        Triple(closestDay, closestDate, closestIndex)
    }

    val targetIndex = remember(nextDayInfo.third) {
        if (nextDayInfo.third >= 0) (nextDayInfo.third - 1).coerceAtLeast(0) else 0
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = targetIndex)

    androidx.compose.runtime.LaunchedEffect(targetIndex) {
        if (targetIndex >= 0 && listState.firstVisibleItemIndex != targetIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val targetDay = nextDayInfo.first
        val targetDate = nextDayInfo.second
        if (targetDay != null && targetDate != null) {
            val calNow = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calTarget = Calendar.getInstance().apply { 
                time = targetDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMillis = calTarget.timeInMillis - calNow.timeInMillis
            val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sıradaki Dini Gün",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = targetDay.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.DateRange,
                            contentDescription = "Tarih",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp).size(20.dp)
                        )
                        Text(
                            text = targetDay.date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (targetDay.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = targetDay.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        val remainingText = when {
                            diffDays == 0L -> "Bugün"
                            diffDays == 1L -> "Yarın"
                            else -> "${diffDays} Gün Kaldı"
                        }
                        Text(
                            text = remainingText,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)
            ),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            itemsIndexed(timelineData) { index, node ->
                when (node) {
                    is TimelineNode.YearHeader -> {
                        Text(
                            text = node.year,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, start = 8.dp)
                        )
                    }
                    is TimelineNode.DayEntry -> {
                        val dayDate = parseReligiousDate(node.day.date)
                        
                        val calNow = Calendar.getInstance().apply { 
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val calTarget = Calendar.getInstance().apply { 
                            if (dayDate != null) time = dayDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        val diffMillisItem = calTarget.timeInMillis - calNow.timeInMillis
                        val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillisItem)
                        
                        val isPast = diffDays < 0
                        val isToday = diffDays == 0L
                        
                        val alpha = if (isPast) 0.5f else 1f
                        val dotColor = if (isToday) MaterialTheme.colorScheme.primary else if (isPast) androidx.compose.ui.graphics.Color.Gray else MaterialTheme.colorScheme.secondary
                        
                        Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                            // Vertical line
                            if (!node.isLast) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 3.dp, top = 24.dp)
                                        .width(2.dp)
                                        .height(100.dp) // Approximate height to connect to next item
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Timeline dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                                                         else MaterialTheme.colorScheme.surface.copy(alpha = if (isPast) 0.4f else 0.8f)
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 0.dp)
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
                                            Text(
                                                text = node.day.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
                                            )
                                            
                                            if (!isPast && !isToday) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "${diffDays} gün",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            } else if (isToday) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Bugün",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = node.day.date,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = alpha * 0.7f)
                                        )
                                        if (node.day.description.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = node.day.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha * 0.75f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarItem(dayData: TimingsData, isToday: Boolean = false) {
    val bgColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val textColor = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (isToday) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isToday) 10.dp else 6.dp)
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 6.dp else 0.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getTurkishDateString(dayData.date.readable, dayData.date.timestamp),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "BUGÜN",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                dayData.date.hijri?.let { hijri ->
                    val monthName = getTurkishHijriMonth(hijri.month?.number, hijri.month?.en)
                    Text(
                        text = "${hijri.day} $monthName ${hijri.year}",
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(if (isToday) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CalendarTimeCol("İmsak", clean(dayData.timings.Fajr), textColor, secondaryTextColor, Modifier.weight(1f))
                CalendarTimeCol("Sabah", calculateSabahCal(clean(dayData.timings.Sunrise)), textColor, secondaryTextColor, Modifier.weight(1f))
                CalendarTimeCol("Güneş", clean(dayData.timings.Sunrise), textColor, secondaryTextColor, Modifier.weight(1f))
                CalendarTimeCol("Öğle", clean(dayData.timings.Dhuhr), textColor, secondaryTextColor, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CalendarTimeCol("İkindi", clean(dayData.timings.Asr), textColor, secondaryTextColor, Modifier.weight(1f))
                CalendarTimeCol("Akşam", clean(dayData.timings.Maghrib), textColor, secondaryTextColor, Modifier.weight(1f))
                CalendarTimeCol("Yatsı", clean(dayData.timings.Isha), textColor, secondaryTextColor, Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f)) // To balance the second row
            }
        }
    }
}

@Composable
fun CalendarTimeCol(name: String, time: String, textColor: androidx.compose.ui.graphics.Color, secondaryTextColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = secondaryTextColor, style = MaterialTheme.typography.bodyMedium)
        Text(time, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun clean(time: String) = time.take(5)

fun getTurkishDateString(readable: String, timestampStr: String): String {
    return try {
        val seconds = timestampStr.toLongOrNull() ?: return translateReadableDate(readable)
        val date = Date(seconds * 1000)
        val formatter = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR"))
        formatter.format(date)
    } catch (e: Exception) {
        translateReadableDate(readable)
    }
}

fun translateReadableDate(readable: String): String {
    var result = readable
    val replacements = mapOf(
        "Jan" to "Ocak", "Feb" to "Şubat", "Mar" to "Mart", "Apr" to "Nisan",
        "May" to "Mayıs", "Jun" to "Haziran", "Jul" to "Temmuz", "Aug" to "Ağustos",
        "Sep" to "Eylül", "Oct" to "Ekim", "Nov" to "Kasım", "Dec" to "Aralık",
        "Monday" to "Pazartesi", "Tuesday" to "Salı", "Wednesday" to "Çarşamba",
        "Thursday" to "Perşembe", "Friday" to "Cuma", "Saturday" to "Cumartesi",
        "Sunday" to "Pazar"
    )
    for ((eng, tr) in replacements) {
        result = result.replace(eng, tr, ignoreCase = true)
    }
    return result
}

private fun calculateSabahCal(sunrise: String): String {
    try {
        val parts = sunrise.split(":")
        if (parts.size == 2) {
            var h = parts[0].toInt()
            val m = parts[1].toInt()
            h -= 1
            if (h < 0) h += 24
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return sunrise
}

fun getTurkishHijriMonth(engMonth: String): String = getTurkishHijriMonth(null, engMonth)

fun getTurkishHijriMonth(monthNumber: Int?, engMonth: String? = null): String {
    val monthNames = listOf(
        "Muharrem", "Safer", "Rebi-ül Evvel", "Rebi-ül Ahir",
        "Cemazi-el Evvel", "Cemazi-el Ahir", "Recep", "Şaban",
        "Ramazan", "Şevval", "Zilkade", "Zilhicce"
    )
    if (monthNumber != null && monthNumber in 1..12) {
        return monthNames[monthNumber - 1]
    }
    val eng = engMonth ?: return ""
    return when {
        eng.contains("Muharram", true) -> "Muharrem"
        eng.contains("Safar", true) -> "Safer"
        eng.contains("Rabi", true) && (eng.contains("Awwal", true) || eng.contains("1", true)) -> "Rebi-ül Evvel"
        eng.contains("Rabi", true) -> "Rebi-ül Ahir"
        eng.contains("Jumad", true) || eng.contains("ūlá", true) || eng.contains("ula", true) -> {
            if (eng.contains("Thani", true) || eng.contains("Akhir", true) || eng.contains("2", true)) "Cemazi-el Ahir"
            else "Cemazi-el Evvel"
        }
        eng.contains("Rajab", true) -> "Recep"
        eng.contains("Shaban", true) || eng.contains("Sha'ban", true) || eng.contains("Shaʿbān", true) -> "Şaban"
        eng.contains("Ramadan", true) -> "Ramazan"
        eng.contains("Shawwal", true) -> "Şevval"
        eng.contains("Qidah", true) || eng.contains("Qi'dah", true) || eng.contains("Qiʿdah", true) -> "Zilkade"
        eng.contains("Hijjah", true) || eng.contains("Ḥijjah", true) -> "Zilhicce"
        else -> eng
    }
}
