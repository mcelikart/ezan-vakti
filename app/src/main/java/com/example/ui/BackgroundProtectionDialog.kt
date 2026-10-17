package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat

/**
 * Checks if the app is exempt from Android's battery optimizations.
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    return true
}

/**
 * Direct request or intent for ignoring battery optimizations.
 */
fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

/**
 * Tries to open manufacturer auto-start / background settings (Xiaomi, Samsung, Huawei, Oppo, Vivo, etc.)
 */
fun openAutoStartSettings(context: Context) {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val intents = mutableListOf<Intent>()

    when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
            intents.add(Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            intents.add(Intent().setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"))
        }
        manufacturer.contains("samsung") -> {
            intents.add(Intent().setClassName("com.samsung.android.looper", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            intents.add(Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"))
        }
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
            intents.add(Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"))
            intents.add(Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"))
        }
        manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
            intents.add(Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
            intents.add(Intent().setClassName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"))
        }
        manufacturer.contains("vivo") -> {
            intents.add(Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"))
            intents.add(Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
        }
    }

    // Default fallbacks
    intents.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    })

    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // try next
        }
    }
}

/**
 * Checks if notification permission is granted (Android 13+).
 */
fun isNotificationPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * Interactive Dialog for First Launch & Background Protection Consent.
 */
@Composable
fun BackgroundProtectionDialog(
    onDismiss: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    var isBatteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var isNotifGranted by remember { mutableStateOf(isNotificationPermissionGranted(context)) }

    // Periodically re-check status when user returns from settings
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            isBatteryExempt = isIgnoringBatteryOptimizations(context)
            isNotifGranted = isNotificationPermissionGranted(context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Arka Planda Kesintisiz Çalışma Onayı",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Ezan vakti bildirimlerinin ve ezan seslerinin cihazınız kilitliyken bile tam vaktinde ve kesintisiz çalması için uygulamanın arka planda kısıtlanmadan çalışması gerekmektedir.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 19.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Item 1: Battery Optimization
                PermissionStatusItem(
                    title = "1. Pil Optimizasyonu Muafiyeti",
                    description = if (isBatteryExempt) 
                        "Uygulama pil kısıtlamasından muaf tutuldu. Sistem arka planda kapatmayacaktır."
                    else 
                        "Android sistemi arka planda ezan bildirimlerini geciktirmesin diye pil optimizasyonunu kapatın.",
                    isGranted = isBatteryExempt,
                    buttonText = if (isBatteryExempt) "Korumalı" else "Pil Optimizasyonunu Kaldır",
                    onButtonClick = {
                        requestIgnoreBatteryOptimizations(context)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Item 2: Notification Permission
                PermissionStatusItem(
                    title = "2. Bildirim & Ezan Ses İzni",
                    description = if (isNotifGranted) 
                        "Bildirim izni verilmiştir. Ezan vakitlerinde uyarılar görünecektir." 
                    else 
                        "Ezan vakti girdiğinde ekranda görsel ve sesli uyarı gösterilmesi için izin verin.",
                    isGranted = isNotifGranted,
                    buttonText = if (isNotifGranted) "İzin Verildi" else "Bildirim İznini Ver",
                    onButtonClick = {
                        onRequestPermissions()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Item 3: Auto-start OEM guide
                PermissionStatusItem(
                    title = "3. Otomatik Başlatma (Cihaz Ayarı)",
                    description = "Xiaomi, Samsung, Huawei, Oppo vb. cihazlarda uygulamanın arka planda öldürülmemesi için Otomatik Başlatma iznini kontrol edin.",
                    isGranted = false,
                    isInfoOnly = true,
                    buttonText = "Cihaz Arka Plan Ayarları",
                    onButtonClick = {
                        openAutoStartSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Daha Sonra",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (!isBatteryExempt) {
                                requestIgnoreBatteryOptimizations(context)
                            }
                            if (!isNotifGranted) {
                                onRequestPermissions()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Onayla & Koru",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusItem(
    title: String,
    description: String,
    isGranted: Boolean,
    isInfoOnly: Boolean = false,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    val cardBg = if (isGranted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = if (isGranted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isGranted -> Icons.Default.CheckCircle
                        isInfoOnly -> Icons.Default.Settings
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = when {
                        isGranted -> MaterialTheme.colorScheme.primary
                        isInfoOnly -> MaterialTheme.colorScheme.secondary
                        else -> Color(0xFFE53935)
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (isGranted) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Aktif",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            )

            if (!isGranted || isInfoOnly) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onButtonClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
