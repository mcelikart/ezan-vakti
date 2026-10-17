package com.example.ui
import android.view.HapticFeedbackConstants

import android.content.Context
import androidx.compose.ui.platform.LocalView
import android.view.SoundEffectConstants
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.window.Dialog

data class DhikrItem(val name: String, val arabic: String, val target: Int, val isCustom: Boolean = false, val meaning: String = "")
data class HistoryItem(val date: String, val dhikrName: String, val count: Int)

@Composable
fun DhikrScreen(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // SharedPreferences setup for robust, lightweight, crash-free local persistence
    val prefs = remember { context.getSharedPreferences("dhikr_prefs", Context.MODE_PRIVATE) }

    // Load settings
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var soundType by remember { mutableStateOf(prefs.getInt("sound_type", 0)) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var interactionMode by remember { mutableStateOf(prefs.getInt("interaction_mode", 0)) }

    LaunchedEffect(context) {
        NotificationSoundManager.initDhikrSounds(context)
    }

    // Load custom dhikrs list
    // Format: Name|Arabic|Target;Name2|Arabic2|Target2
    var customDhikrsString by remember { mutableStateOf(prefs.getString("custom_dhikrs", "") ?: "") }

    val customDhikrs = remember(customDhikrsString) {
        if (customDhikrsString.isEmpty()) {
            emptyList<DhikrItem>()
        } else {
            customDhikrsString.split(";").mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 3) {
                    DhikrItem(
                        name = parts[0],
                        arabic = parts[1],
                        target = parts[2].toIntOrNull() ?: 33,
                        isCustom = true
                    )
                } else null
            }
        }
    }

    // Default presets
    val presets = listOf(
        DhikrItem("Sübhanallah", "سُبْحَانَ ٱللَّٰهِ", 33, false, "Allah'ı tüm noksanlıklardan tenzih ederim."),
        DhikrItem("Elhamdülillah", "ٱلْحَمْدُ لِلَّٰهِ", 33, false, "Hamd ve övgü yalnızca Allah'adır."),
        DhikrItem("Allahu Ekber", "ٱللَّٰهُ أَكْبَرُ", 33, false, "Allah en büyüktür."),
        DhikrItem("Lâ ilâhe illallah", "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", 99, false, "Allah'tan başka ilah yoktur."),
        DhikrItem("Estağfirullah", "أَسْتَغْفِرُ ٱللَّٰهَ", 100, false, "Allah'tan bağışlanma ve af dilerim."),
        DhikrItem("Salavat-ı Şerife", "اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ", 100, false, "Peygamber Efendimiz'e (s.a.v.) selam ve salat olsun.")
    )

    // Combine presets and custom ones
    val allDhikrs = remember(customDhikrs) { presets + customDhikrs }

    val activeDhikrTarget by viewModel.selectedDhikrTarget.collectAsState()

    var selectedIndex by remember { 
        mutableStateOf(
            prefs.getInt("selected_index", 0).coerceIn(0, allDhikrs.lastIndex.coerceAtLeast(0))
        ) 
    }

    LaunchedEffect(activeDhikrTarget) {
        val targetObj = activeDhikrTarget
        if (targetObj != null) {
            var idx = allDhikrs.indexOfFirst { it.name == targetObj.name }
            if (idx < 0) {
                // Add to custom dhikrs list
                val newCustom = DhikrItem(
                    name = targetObj.name,
                    arabic = targetObj.arabic,
                    target = targetObj.target,
                    isCustom = true
                )
                val currentCustomList = if (customDhikrsString.isEmpty()) {
                    emptyList()
                } else {
                    customDhikrsString.split(";").mapNotNull {
                        val parts = it.split("|")
                        if (parts.size >= 3) DhikrItem(parts[0], parts[1], parts[2].toIntOrNull() ?: 33, true) else null
                    }
                }
                val updatedList = currentCustomList.filter { it.name != targetObj.name } + newCustom
                val updatedString = updatedList.joinToString(";") { "${it.name}|${it.arabic}|${it.target}" }
                prefs.edit().putString("custom_dhikrs", updatedString).apply()
                customDhikrsString = updatedString
                val combined = presets + updatedList
                idx = combined.indexOfFirst { it.name == targetObj.name }
            }
            if (idx >= 0) {
                selectedIndex = idx
                prefs.edit().putInt("selected_index", idx).apply()
                prefs.edit().putInt("target_${targetObj.name}", targetObj.target).apply()
            }
            viewModel.clearDhikrTarget()
        }
    }
    
    // Ensure index remains valid if custom dhikrs are deleted
    if (selectedIndex >= allDhikrs.size) {
        selectedIndex = 0
    }

    val currentDhikr = allDhikrs[selectedIndex]

    // Count persistent per-dhikr progress
    var count by remember(currentDhikr.name) {
        mutableStateOf(prefs.getInt("count_${currentDhikr.name}", 0))
    }

    // Dynamic target choice (presets or customized target per dhikr)
    var currentTarget by remember(currentDhikr.name, currentDhikr.target) {
        mutableStateOf(prefs.getInt("target_${currentDhikr.name}", currentDhikr.target))
    }

    // Total counts ever clicked
    var totalCount by remember {
        mutableStateOf(prefs.getInt("total_count", 0))
    }

    val volumeKeyDhikrEnabled by viewModel.volumeKeyDhikrEnabled.collectAsState()

    // Completion log history
    var historyString by remember { mutableStateOf(prefs.getString("history", "") ?: "") }
    val historyList = remember(historyString) {
        if (historyString.isEmpty()) {
            emptyList<HistoryItem>()
        } else {
            historyString.split(";").mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 3) {
                    HistoryItem(parts[0], parts[1], parts[2].toIntOrNull() ?: 0)
                } else null
            }
        }
    }

    // UI state states
    var showAddDhikrDialog by remember { mutableStateOf(false) }
    var showCustomTargetDialog by remember { mutableStateOf(false) }
    var showDhikrListDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var buttonPressed by remember { mutableStateOf(false) }

    val view = LocalView.current

    val vibrator = remember { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator 
        }
    }

    fun vibrateShort() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12)
            }
        } catch (e: Exception) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (e2: Exception) {}
        }
    }

    fun vibrateLong() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {}
    }

    fun playBeep() {
        if (soundEnabled) {
            try {
                NotificationSoundManager.playDhikrClick(context, soundType)
            } catch (e: Exception) {}
        }
    }

    // SharedPreferences State Save Functions
    fun saveCount(newCount: Int) {
        count = newCount
        prefs.edit().putInt("count_${currentDhikr.name}", newCount).apply()
    }

    fun saveTotalCount(newTotal: Int) {
        totalCount = newTotal
        prefs.edit().putInt("total_count", newTotal).apply()
    }

    fun saveTarget(newTarget: Int) {
        currentTarget = newTarget
        prefs.edit().putInt("target_${currentDhikr.name}", newTarget).apply()
    }

    fun toggleSound() {
        val nextVal = !soundEnabled
        soundEnabled = nextVal
        prefs.edit().putBoolean("sound_enabled", nextVal).apply()
    }

    fun cycleSoundType() {
        val nextVal = (soundType + 1) % 4
        soundType = nextVal
        prefs.edit().putInt("sound_type", nextVal).apply()
        // Play it once to preview
        if (soundEnabled) {
            playBeep()
        }
    }

    fun toggleVibration() {
        val nextVal = !vibrationEnabled
        vibrationEnabled = nextVal
        prefs.edit().putBoolean("vibration_enabled", nextVal).apply()
    }

    fun addCustomDhikr(name: String, arabic: String, target: Int) {
        val cleanName = name.replace("|", "").replace(";", "")
        val cleanArabic = arabic.replace("|", "").replace(";", "")
        val newItemStr = "$cleanName|$cleanArabic|$target"
        val newString = if (customDhikrsString.isEmpty()) {
            newItemStr
        } else {
            "$customDhikrsString;$newItemStr"
        }
        customDhikrsString = newString
        prefs.edit().putString("custom_dhikrs", newString).apply()
    }

    fun deleteCustomDhikr(indexInCustom: Int) {
        val itemToDelete = customDhikrs[indexInCustom]
        val updatedList = customDhikrs.filterIndexed { idx, _ -> idx != indexInCustom }
        val newString = updatedList.joinToString(";") { "${it.name}|${it.arabic}|${it.target}" }
        customDhikrsString = newString
        prefs.edit().putString("custom_dhikrs", newString).apply()

        // Clean up stored metrics
        prefs.edit()
            .remove("count_${itemToDelete.name}")
            .remove("target_${itemToDelete.name}")
            .apply()

        selectedIndex = 0
    }

    fun addHistoryEntry(name: String, reachedCount: Int) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date())
        val entry = "$dateStr|$name|$reachedCount"
        val newHistory = if (historyString.isEmpty()) {
            entry
        } else {
            "$entry;$historyString"
        }


        val limited = newHistory.split(";").take(30).joinToString(";")
        historyString = limited
        prefs.edit().putString("history", limited).apply()
    }

    fun clearHistory() {
        historyString = ""
        prefs.edit().remove("history").apply()
    }

    // Observe volume down events for zikir
    LaunchedEffect(Unit) {
        viewModel.volumeDownEvent.collect {
            buttonPressed = true
            val nextCount = count + 1
            saveCount(nextCount)
            saveTotalCount(totalCount + 1)
            // Check target threshold reached (multiples)
            if (currentTarget > 0 && nextCount > 0 && nextCount % currentTarget == 0) {
                vibrateLong()
                playBeep()
                addHistoryEntry(currentDhikr.name, currentTarget)
            } else {
                vibrateShort()
                playBeep()
            }
        }
    }

    // Animation scale for big central button click
    val buttonScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.94f else 1.0f,
        animationSpec = tween(75),
        finishedListener = { buttonPressed = false },
        label = "button_scale"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                // Reserve the floating bar so the last card can scroll clear.
                bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER (Title and Sound/Vibration settings)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zikirmatik",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dualar & Zikirler",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (count > 0) {
                                    addHistoryEntry(currentDhikr.name, count)
                                }
                                saveCount(0)
                                if (vibrationEnabled) vibrateShort()
                                playBeep()
                            },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sıfırla",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { toggleSound() },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Ses Aç/Kapat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { toggleVibration() },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Titreşim Aç/Kapat",
                                tint = if (vibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            if (soundEnabled) {
                item {
                    val soundTypeNames = listOf("🪵 Ahşap Tesbih", "🪨 Taş Tesbih", "🌾 Kuka & Kehribar", "🦴 Kemik & Fildişi")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(soundTypeNames) { idx, name ->
                            val isSel = (soundType == idx)
                            val bg = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            val tc = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            val bc = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .border(1.dp, bc, RoundedCornerShape(16.dp))
                                    .clickable {
                                        soundType = idx
                                        prefs.edit().putInt("sound_type", idx).apply()
                                        NotificationSoundManager.playDhikrClick(context, idx)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = tc
                                )
                            }
                        }
                    }
                }
            }

            // 3. MINIMAL TITLE & MODE SWITCHER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val targetText = if (currentTarget > 0) "Hedef: $currentTarget" else "Hedef: Sınırsız"
                        val completedCycles = if (currentTarget > 0) count / currentTarget else 0
                        val tourText = if (completedCycles > 0) " • Tur: ${completedCycles + 1}" else ""
                        Text(
                            text = "$targetText$tourText",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (interactionMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    interactionMode = 0
                                    prefs.edit().putInt("interaction_mode", 0).apply()
                                    if (vibrationEnabled) vibrateShort()
                                    playBeep()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Klasik",
                                color = if (interactionMode == 0) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (interactionMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    interactionMode = 1
                                    prefs.edit().putInt("interaction_mode", 1).apply()
                                    if (vibrationEnabled) vibrateShort()
                                    playBeep()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Tesbih",
                                color = if (interactionMode == 1) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 4. BIG TACTILE GLOWING DIGI-DIAL / TESBİH SWIPER
            item {
                Spacer(modifier = Modifier.height(14.dp))
                if (interactionMode == 0) {
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .scale(buttonScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                buttonPressed = true
                                val nextCount = count + 1
                                saveCount(nextCount)
                                saveTotalCount(totalCount + 1)

                                // Check target threshold reached (multiples)
                                if (currentTarget > 0 && nextCount > 0 && nextCount % currentTarget == 0) {
                                    vibrateLong()
                                    playBeep()
                                    addHistoryEntry(currentDhikr.name, currentTarget)
                                } else {
                                    vibrateShort()
                                    playBeep()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Circular Progress Canvas rim drawing
                        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            val completedCycles = if (currentTarget > 0) count / currentTarget else 0
                            val currentProgress = if (currentTarget > 0) (count % currentTarget).toFloat() / currentTarget else 0f
                            val sweep = if (currentTarget > 0) currentProgress * 360f else 360f
                            
                            val cycleColors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green (Primary)
                                androidx.compose.ui.graphics.Color(0xFF26A69A), // Teal
                                androidx.compose.ui.graphics.Color(0xFFFFA726), // Orange
                                androidx.compose.ui.graphics.Color(0xFFAB47BC), // Purple
                                androidx.compose.ui.graphics.Color(0xFFEC407A), // Pink
                                androidx.compose.ui.graphics.Color(0xFF29B6F6)  // Light Blue
                            )
                            
                            val strokeWidth = 11.dp.toPx()
                            val gap = 4.dp.toPx()
                            
                            // Draw completed inner rings
                            val maxInnerRings = 4
                            val visibleCompletedRings = completedCycles.coerceAtMost(maxInnerRings)
                            
                            for (i in 0 until visibleCompletedRings) {
                                val cycleIndex = (completedCycles - 1 - i)
                                val ringColor = cycleColors[cycleIndex % cycleColors.size]
                                
                                val inset = (i + 1) * (strokeWidth + gap)
                                
                                drawArc(
                                    color = ringColor.copy(alpha = 0.4f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth - 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                                )
                            }
                            
                            // Current outer ring
                            val currentRingColor = cycleColors[completedCycles % cycleColors.size]
                            val trailColor = currentRingColor.copy(alpha = 0.08f)
                            
                            // Draw trail for current ring
                            drawArc(
                                color = trailColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            
                            // Draw progress for current ring
                            drawArc(
                                color = currentRingColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }

                        // Numeric reading core
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val completedCycles = if (currentTarget > 0) count / currentTarget else 0
                            val isExactMultiple = currentTarget > 0 && count > 0 && count % currentTarget == 0
                            val displayStatus = if (isExactMultiple) "TAMAMLANDI" else if (completedCycles > 0) "${completedCycles + 1}. TUR" else "ZİKİR"
                            val statusColor = if (isExactMultiple) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            
                            Text(
                                text = displayStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = count.toString(),
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.displayMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (currentTarget > 0) "Hedef: $currentTarget" else "Hedef: Sınırsız",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    TesbihBeadSwiper(
                        count = count,
                        currentTarget = currentTarget,
                        currentDhikrName = currentDhikr.name,
                        onIncrement = {
                            val nextCount = count + 1
                            saveCount(nextCount)
                            saveTotalCount(totalCount + 1)
                            if (currentTarget > 0 && nextCount > 0 && nextCount % currentTarget == 0) {
                                vibrateLong()
                                playBeep()
                                addHistoryEntry(currentDhikr.name, currentTarget)
                            } else {
                                vibrateShort()
                                playBeep()
                            }
                        },
                        onDecrement = {
                            if (count > 0) {
                                saveCount(count - 1)
                                if (vibrationEnabled) vibrateShort()
                                playBeep()
                            }
                        }
                    )
                }
            }

            // 5. TARGET SELECTOR CHIPS & CONTROL BUTTONS
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Target Chips + Manual Custom Target Button ("Özel")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hedef Sayı:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presetTargets = listOf(33, 99, 100, 0)
                            val isCustomTarget = currentTarget !in presetTargets

                            for (targetVal in presetTargets) {
                                val isSel = (currentTarget == targetVal)
                                val textLabel = if (targetVal == 0) "Sınırsız" else "$targetVal"

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            saveTarget(targetVal)
                                            if (vibrationEnabled) vibrateShort()
                                            playBeep()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = textLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Custom Target Button ("Özel" or custom value e.g. 500)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isCustomTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCustomTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        showCustomTargetDialog = true
                                        if (vibrationEnabled) vibrateShort()
                                        playBeep()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "El ile Hedef Belirle",
                                        tint = if (isCustomTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isCustomTarget) "$currentTarget" else "Özel",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomTarget) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCustomTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Aligned Control & Action Buttons (Sıfırla + Ses Tuşları Zikir)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sıfırla (Reset) Button
                        OutlinedButton(
                            onClick = {
                                if (count > 0) {
                                    addHistoryEntry(currentDhikr.name, count)
                                }
                                saveCount(0)
                                if (vibrationEnabled) vibrateShort()
                                playBeep()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sıfırla",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Sıfırla",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Ses Tuşları Zikir Toggle Button
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleVolumeKeyDhikr()
                                val msg = if (!volumeKeyDhikrEnabled) "Ses tuşları zikir çekmek için aktif." else "Ses tuşları varsayılan işlevine döndürüldü."
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                if (vibrationEnabled) vibrateShort()
                                playBeep()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = "Ses Tuşları ile Zikir",
                                    tint = if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (volumeKeyDhikrEnabled) "Ses Tuşları: Açık" else "Ses Tuşları",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (volumeKeyDhikrEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 7. STATS AND DETAILED COMPLETION LOG
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zikir Geçmişi & Analiz",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Genel Toplam:",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = totalCount.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (historyList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Henüz tamamlanmış zikir kaydı bulunmuyor.\nZikri sıfırladığınızda veya tamamladığınızda buraya eklenir.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.take(5).forEach { hist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = hist.dhikrName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = hist.date,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                            )
                                        }
                                        Text(
                                            text = "${hist.count} Adet",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (historyList.size > 5) {
                                    var showAllHistoryDialog by remember { mutableStateOf(false) }

                                    Text(
                                        text = "Tüm Geçmişi Gör (${historyList.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clickable { showAllHistoryDialog = true }
                                            .padding(vertical = 6.dp)
                                    )

                                    if (showAllHistoryDialog) {
                                        Dialog(onDismissRequest = { showAllHistoryDialog = false }) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(20.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Tüm Zikir Geçmişi",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        IconButton(onClick = { showAllHistoryDialog = false }) {
                                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    LazyColumn(
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.height(300.dp)
                                                    ) {
                                                        items(historyList) { hist ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = hist.dhikrName,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                    Text(
                                                                        text = hist.date,
                                                                        fontSize = 10.sp,
                                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "${hist.count} Adet",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Button(
                                                        onClick = {
                                                            clearHistory()
                                                            showAllHistoryDialog = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text(text = "Geçmişi Temizle", color = Color.White)
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
        }

        // 8. CUSTOM DHIKR CREATION DIALOG
        if (showAddDhikrDialog) {
            var newName by remember { mutableStateOf("") }
            var newArabic by remember { mutableStateOf("") }
            var newTargetStr by remember { mutableStateOf("33") }
            var isError by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showAddDhikrDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Özel Zikir Ekle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Zikir Adı *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = { Text("Örn: Lâ havle ve lâ kuvvete...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Arapça Okunuşu (İsteğe bağlı)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newArabic,
                                onValueChange = { newArabic = it },
                                placeholder = { Text("Arapça metin veya açıklama") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Hedef Sayı (Limitsiz için 0 girin)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newTargetStr,
                                onValueChange = { newTargetStr = it.filter { char -> char.isDigit() } },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (isError) {
                            Text(
                                text = "Lütfen geçerli bir zikir adı girin.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddDhikrDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "İptal")
                            }

                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        val target = newTargetStr.toIntOrNull() ?: 33
                                        addCustomDhikr(newName, newArabic, target)
                                        showAddDhikrDialog = false
                                        // Auto-select the newly added custom dhikr
                                        selectedIndex = allDhikrs.size
                                        count = 0
                                        currentTarget = target
                                    } else {
                                        isError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "Kaydet", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 9. CUSTOM TARGET DIALOG
        if (showCustomTargetDialog) {
            var newTargetStr by remember { mutableStateOf("") }
            var isError by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showCustomTargetDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Özel Hedef Belirle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Hedef Sayı (Limitsiz için 0 girin)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = newTargetStr,
                                onValueChange = { newTargetStr = it.filter { char -> char.isDigit() } },
                                singleLine = true,
                                placeholder = { Text("Örn: 500") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (isError) {
                            Text(
                                text = "Lütfen geçerli bir sayı girin.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCustomTargetDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "İptal")
                            }

                            Button(
                                onClick = {
                                    val target = newTargetStr.toIntOrNull()
                                    if (target != null && target >= 0) {
                                        saveTarget(target)
                                        showCustomTargetDialog = false
                                    } else {
                                        isError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "Kaydet", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCubicPoint(
    p0: androidx.compose.ui.geometry.Offset,
    p1: androidx.compose.ui.geometry.Offset,
    p2: androidx.compose.ui.geometry.Offset,
    p3: androidx.compose.ui.geometry.Offset,
    t: Float
): androidx.compose.ui.geometry.Offset {
    val clampedT = t.coerceIn(0f, 1f)
    val u = 1f - clampedT
    val tt = clampedT * clampedT
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * clampedT

    val x = uuu * p0.x + 3 * uu * clampedT * p1.x + 3 * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3 * uu * clampedT * p1.y + 3 * u * tt * p2.y + ttt * p3.y
    return androidx.compose.ui.geometry.Offset(x, y)
}

/**
 * First derivative of the same cubic Bézier.
 *
 * Used to orient each bead along the strand: a bead travelling around a real
 * loop turns to follow the cord, so its highlight and its printed numeral
 * rotate with the path tangent instead of staying screen-aligned.
 */
private fun getCubicTangent(
    p0: androidx.compose.ui.geometry.Offset,
    p1: androidx.compose.ui.geometry.Offset,
    p2: androidx.compose.ui.geometry.Offset,
    p3: androidx.compose.ui.geometry.Offset,
    t: Float
): androidx.compose.ui.geometry.Offset {
    val clampedT = t.coerceIn(0f, 1f)
    val u = 1f - clampedT
    val dx = 3 * u * u * (p1.x - p0.x) + 6 * u * clampedT * (p2.x - p1.x) + 3 * clampedT * clampedT * (p3.x - p2.x)
    val dy = 3 * u * u * (p1.y - p0.y) + 6 * u * clampedT * (p2.y - p1.y) + 3 * clampedT * clampedT * (p3.y - p2.y)
    return androidx.compose.ui.geometry.Offset(dx, dy)
}

@Composable
fun TesbihBeadSwiper(
    count: Int,
    currentTarget: Int,
    currentDhikrName: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    var dragAccumulator by remember { mutableStateOf(0f) }
    var pulledInGesture by remember { mutableStateOf(false) }
    val dragAnimatable = remember { Animatable(0f) }
    // Signed push strength of the last flick, -1..1. Real beads keep travelling
    // for a moment after the finger lifts; the strand overshoots then settles.
    val flingEnergy = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Physical bead-on-string: a stiff push that overshoots once and settles,
    // rather than the previous low-damping spring that wobbled for ~1s.
    val beadSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 620f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(370.dp)
            .padding(horizontal = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onIncrement()
                scope.launch {
                    flingEnergy.snapTo(-1f)
                    flingEnergy.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 220f))
                    dragAnimatable.snapTo(-0.78f)
                    dragAnimatable.animateTo(0f, beadSpring)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragAccumulator = 0f
                        pulledInGesture = false
                    },
                    onDragEnd = {
                        pulledInGesture = false
                        scope.launch {
                            dragAnimatable.animateTo(0f, beadSpring)
                        }
                    },
                    onDragCancel = {
                        pulledInGesture = false
                        scope.launch {
                            dragAnimatable.animateTo(0f, beadSpring)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        val currentShift = (dragAccumulator / 220f).coerceIn(-0.6f, 0.6f)
                        scope.launch {
                            dragAnimatable.snapTo(currentShift)
                        }
                        if (!pulledInGesture && (dragAccumulator > 24f || dragAccumulator < -24f)) {
                            pulledInGesture = true
                            onIncrement()
                            scope.launch {
                                flingEnergy.snapTo(if (dragAccumulator > 0) -0.8f else 0.8f)
                                flingEnergy.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 220f))
                                dragAnimatable.snapTo(if (dragAccumulator > 0) -0.6f else 0.6f)
                                dragAnimatable.animateTo(0f, beadSpring)
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val dragShift = dragAnimatable.value
        val energy = flingEnergy.value

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Continuous curved path modeling the Tesbih loop draped in hand
            val p0 = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.05f)
            val p1 = androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.10f)
            val p2 = androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.62f)
            val p3 = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.94f)

            // Thread lags behind the beads while the strand is moving, the way a
            // real cord trails under inertia instead of moving rigidly.
            val trailOffset = Offset(energy * 5.dp.toPx(), energy * 9.dp.toPx())

            val stringPath = Path().apply {
                moveTo(p0.x + trailOffset.x, p0.y + trailOffset.y)
                cubicTo(
                    p1.x + trailOffset.x, p1.y + trailOffset.y,
                    p2.x + trailOffset.x, p2.y + trailOffset.y,
                    p3.x + trailOffset.x, p3.y + trailOffset.y
                )
            }

            drawPath(
                path = stringPath,
                color = Color.Black.copy(alpha = 0.25f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = stringPath,
                color = Color(0xFFC5A059), // Gold thread
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Thumb Contact / Focal Ring Indicator (t = 0.50f)
            val tFocal = 0.50f
            val focalPos = getCubicPoint(p0, p1, p2, p3, tFocal)

            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.15f),
                radius = 32.dp.toPx(),
                center = focalPos
            )
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.35f),
                radius = 26.dp.toPx(),
                center = focalPos,
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )

            // 3. Render Continuous Beads relative to current count (C)
            val dt = 0.082f // Parametric step spacing between beads along Bezier curve
            val range = -5..6

            for (i in range) {
                val beadIndex = count + i
                if (beadIndex < 0) continue

                val t = tFocal - (i - dragShift) * dt
                if (t < 0.02f || t > 0.98f) continue

                val beadPos = getCubicPoint(p0, p1, p2, p3, t)
                val isTargetBead = (currentTarget > 0 && beadIndex > 0 && beadIndex % currentTarget == 0)
                val isImame = (beadIndex == 0)
                val isDurak = (beadIndex > 0 && beadIndex % 33 == 0 && !isImame && !isTargetBead)

                // --- Depth: beads further from the thumb recede. A real strand
                // curves away from the hand, so distant beads read smaller,
                // dimmer and softer rather than as flat equal-sized circles.
                val depth = ((t - tFocal) / 0.5f).coerceIn(-1f, 1f)
                val depthAbs = kotlin.math.abs(depth)
                val depthScale = 1f - 0.22f * depthAbs
                val depthAlpha = 1f - 0.28f * depthAbs

                // --- Squash across the thumb: the bead compresses along the
                // strand as it is pressed past the fingertip, then relaxes.
                val thumbSquash = (1f - 0.13f * (1f - depthAbs).coerceIn(0f, 1f)).coerceIn(0.7f, 1f)

                // --- Rotation: beads turn as they travel around the loop, so
                // their orientation follows the path tangent instead of
                // staying axis-aligned.
                val tangent = getCubicTangent(p0, p1, p2, p3, t)
                val tangentAngle = Math.toDegrees(
                    kotlin.math.atan2(tangent.y.toDouble(), tangent.x.toDouble())
                ).toFloat()

                val baseRadius = when {
                    isTargetBead || isDurak -> 23.dp.toPx()
                    isImame -> 16.dp.toPx()
                    else -> 14.5.dp.toPx()
                }
                val shadowRadius = if (isTargetBead || isDurak) 22.dp.toPx() else if (isImame) 24.dp.toPx() else 14.dp.toPx()

                // Contact shadow tightens under the thumb and softens away from it.
                val shadowAlpha = (0.30f + 0.18f * (1f - depthAbs)) * depthAlpha
                val shadowOffset = 3.dp.toPx() * depthScale
                drawCircle(
                    color = Color.Black.copy(alpha = shadowAlpha.coerceIn(0f, 0.6f)),
                    radius = shadowRadius * depthScale,
                    center = androidx.compose.ui.geometry.Offset(beadPos.x + shadowOffset, beadPos.y + shadowOffset * 1.3f)
                )

                if (isImame) {
                    // --- Render İmame (Master Ornament) ---
                    val imameHeight = 44.dp.toPx() * depthScale
                    val imameWidth = 18.dp.toPx() * depthScale * thumbSquash

                    val imameBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8E1), // Ivory top
                            Color(0xFFD4AF37), // Gold body
                            Color(0xFF5D4037)  // Deep amber base
                        ),
                        startY = beadPos.y - imameHeight / 2,
                        endY = beadPos.y + imameHeight / 2
                    )

                    rotate(degrees = tangentAngle, pivot = beadPos) {
                        drawRoundRect(
                            brush = imameBrush,
                            topLeft = androidx.compose.ui.geometry.Offset(beadPos.x - imameWidth / 2, beadPos.y - imameHeight / 2),
                            size = androidx.compose.ui.geometry.Size(imameWidth, imameHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx() * depthScale, 9.dp.toPx() * depthScale)
                        )

                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = depthAlpha),
                            radius = 6.dp.toPx() * depthScale,
                            center = androidx.compose.ui.geometry.Offset(beadPos.x, beadPos.y - imameHeight * 0.25f)
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = depthAlpha),
                            radius = 6.dp.toPx() * depthScale,
                            center = androidx.compose.ui.geometry.Offset(beadPos.x, beadPos.y + imameHeight * 0.25f)
                        )
                    }
                } else if (isTargetBead || isDurak) {
                    // --- Render Target / Durak (Büyük Tesbih Tanesi) ---
                    val durakRadius = baseRadius * depthScale

                    val haloColor = if (isTargetBead) Color(0xFF00E676) else Color(0xFFFFD54F)
                    drawCircle(
                        color = haloColor.copy(alpha = 0.35f * depthAlpha),
                        radius = durakRadius + 5.dp.toPx(),
                        center = beadPos
                    )

                    val durakBrush = Brush.radialGradient(
                        colors = if (isTargetBead) listOf(
                            Color(0xFFE8F5E9), // Emerald highlight
                            Color(0xFF26A69A), // Rich teal/emerald
                            Color(0xFF1B5E20)  // Deep forest green rim
                        ) else listOf(
                            Color(0xFFFFFDE7), // Bright highlight
                            Color(0xFFFFB300), // Rich amber gold
                            Color(0xFF3E2723)  // Dark rim
                        ),
                        center = androidx.compose.ui.geometry.Offset(beadPos.x - durakRadius * 0.35f, beadPos.y - durakRadius * 0.35f),
                        radius = durakRadius * 1.35f
                    )

                    // Squash/stretch is applied by scaling along the tangent.
                    rotate(degrees = tangentAngle, pivot = beadPos) {
                        scale(scaleX = thumbSquash, scaleY = 2f - thumbSquash, pivot = beadPos) {
                            drawCircle(
                                brush = durakBrush,
                                radius = durakRadius,
                                center = beadPos
                            )
                            drawCircle(
                                color = (if (isTargetBead) Color(0xFFA7F3D0) else Color(0xFFFFF9C4)).copy(alpha = depthAlpha),
                                radius = durakRadius,
                                center = beadPos,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    val targetNumStr = if (isTargetBead) "$beadIndex" else "33"
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 12.dp.toPx() * depthScale
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                    }
                    textPaint.alpha = (255 * depthAlpha).toInt()
                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.rotate(tangentAngle, beadPos.x, beadPos.y)
                    drawContext.canvas.nativeCanvas.drawText(
                        targetNumStr,
                        beadPos.x,
                        beadPos.y + 4.5.dp.toPx(),
                        textPaint
                    )
                    drawContext.canvas.nativeCanvas.restore()
                } else {
                    // --- Render Regular Polished Wooden/Amber Bead ---
                    val beadRadius = baseRadius * depthScale

                    val beadBrush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD7CCC8), // Top highlight reflection
                            Color(0xFF8D5B41), // Rich polished wood
                            Color(0xFF3E2723)  // Deep shadow
                        ),
                        center = androidx.compose.ui.geometry.Offset(beadPos.x - beadRadius * 0.38f, beadPos.y - beadRadius * 0.38f),
                        radius = beadRadius * 1.40f
                    )

                    rotate(degrees = tangentAngle, pivot = beadPos) {
                        scale(scaleX = thumbSquash, scaleY = 2f - thumbSquash, pivot = beadPos) {
                            drawCircle(
                                brush = beadBrush,
                                radius = beadRadius,
                                center = beadPos
                            )

                            // Specular highlight travels with the bead's rotation.
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f * depthAlpha),
                                radius = beadRadius * 0.35f,
                                center = androidx.compose.ui.geometry.Offset(beadPos.x - beadRadius * 0.30f, beadPos.y - beadRadius * 0.30f)
                            )

                            drawCircle(
                                color = Color.White.copy(alpha = 0.20f * depthAlpha),
                                radius = beadRadius,
                                center = beadPos,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // String entry/exit hole accent
                            drawCircle(
                                color = Color(0xFF212121).copy(alpha = depthAlpha),
                                radius = 2.dp.toPx() * depthScale,
                                center = beadPos
                            )
                        }
                    }
                }
            }

            // Motion blur: a faint ghost of the strand while it is travelling.
            // A real bead smeared by motion when a flick is fast.
            if (kotlin.math.abs(energy) > 0.02f) {
                val ghostAlpha = kotlin.math.abs(energy) * 0.16f
                var gi = -5
                while (gi <= 6) {
                    val gIndex = count + gi
                    if (gIndex < 0) { gi++; continue }
                    val gt = tFocal - (gi - dragShift + energy * 0.22f) * dt
                    if (gt in 0.02f..0.98f) {
                        val gPos = getCubicPoint(p0, p1, p2, p3, gt)
                        drawCircle(
                            color = Color.White.copy(alpha = ghostAlpha),
                            radius = 12.dp.toPx(),
                            center = gPos
                        )
                    }
                    gi++
                }
            }
        }

        // Left Side: Modern Counter & Zikir Status Display
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            Text(
                text = "Zikir",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = count.toString(),
                fontSize = 110.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 110.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (currentTarget > 0) {
                val cycleNum = (count / currentTarget) + 1
                val remainder = count % currentTarget
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Tur $cycleNum • $remainder / $currentTarget",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
