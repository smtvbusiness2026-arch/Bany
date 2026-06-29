package com.example

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SubscriptionEntity
import com.example.ui.SubscriptionViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for beautiful Arabic localization
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SubscriptionApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionApp(viewModel: SubscriptionViewModel) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<SubscriptionEntity?>(null) }
    
    // Tab Navigation State
    var currentTab by remember { mutableStateOf("home") } // "home", "reports", "cards", "settings"
    
    // Filters and Search State
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var selectedBankFilter by remember { mutableStateOf("الكل") }

    // Aggregate values
    val (monthlyTotal, bankTotals) = remember(subscriptions) {
        calculateTotals(subscriptions)
    }

    // Filtered lists
    val filteredSubscriptions = remember(subscriptions, searchQuery, selectedCategoryFilter, selectedBankFilter) {
        subscriptions.filter { sub ->
            val matchesSearch = sub.name.contains(searchQuery, ignoreCase = true) || 
                                sub.notes.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "الكل" || sub.category == selectedCategoryFilter
            val matchesBank = selectedBankFilter == "الكل" || sub.bankName == selectedBankFilter
            matchesSearch && matchesCategory && matchesBank
        }
    }

    // Subscriptions requiring alert (Renewal near or past due)
    val alertSubscriptions = remember(subscriptions) {
        subscriptions.filter { sub ->
            val remainingDays = getRemainingDays(sub.nextRenewalDate)
            remainingDays <= sub.alertDaysBefore
        }
    }

    val categories = listOf("الكل", "ترفيه", "خدمات", "صحة", "عمل", "تعليم", "أخرى")
    val uniqueBanks = remember(subscriptions) {
        listOf("الكل") + subscriptions.map { it.bankName }.distinct().filter { it.isNotBlank() }
    }

    Scaffold(
        floatingActionButton = {
            if (currentTab == "home") {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFD0BCFF), // GeoFabBg
                    contentColor = Color(0xFF21005D), // GeoOnPrimaryContainer
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .testTag("add_subscription_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة اشتراك جديد",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    label = { Text("الرئيسية", style = MaterialTheme.typography.labelSmall) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "الرئيسية"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "reports",
                    onClick = { currentTab = "reports" },
                    label = { Text("التقارير", style = MaterialTheme.typography.labelSmall) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "التقارير"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "cards",
                    onClick = { currentTab = "cards" },
                    label = { Text("البطاقات", style = MaterialTheme.typography.labelSmall) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "البطاقات"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    label = { Text("الإعدادات", style = MaterialTheme.typography.labelSmall) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
            }
        },
        containerColor = Color(0xFFFEF7FF), // GeoBackground
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header bar (Geometric Balance theme layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEADDFF)), // GeoPrimaryContainer
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF21005D), // GeoOnPrimaryContainer
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "إدارة الاشتراكات",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFF1D1B20) // GeoTextPrimary
                        )
                        Text(
                            text = "التحكم والمتابعة الذكية",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F) // GeoTextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { }
                        .background(Color(0xFFF3EDF7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "إشعارات",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(20.dp)
                    )
                    if (alertSubscriptions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9B1C1C)) // GeoAlertRedText
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                        )
                    }
                }
            }

            // Tab Content Router
            when (currentTab) {
                "home" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        // Summary Dashboard Card
                        item {
                            GeometricSummaryDashboard(
                                monthlyTotal = monthlyTotal,
                                alertCount = alertSubscriptions.size
                            )
                        }

                        // Auto-renewal Alerts section
                        if (alertSubscriptions.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "تنبيه",
                                            tint = Color(0xFF9B1C1C), // GeoAlertRedText
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تجديدات حرجة قادمة (${alertSubscriptions.size})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF9B1C1C)
                                        )
                                    }
                                    
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(alertSubscriptions) { sub ->
                                            AlertSubscriptionCard(
                                                subscription = sub,
                                                onRenew = { viewModel.renewSubscription(sub) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Filter & Search Controls
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("البحث السريع عن اشتراك...") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6750A4),
                                        unfocusedBorderColor = Color(0xFFCAC4D0),
                                        focusedLabelColor = Color(0xFF6750A4),
                                        unfocusedLabelColor = Color(0xFF49454F)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                )

                                // Category filter label & row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "التصنيف الهندسي للخدمات:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF49454F),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    if (selectedCategoryFilter != "الكل" || selectedBankFilter != "الكل") {
                                        Text(
                                            text = "إعادة ضبط الفلاتر",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF6750A4),
                                            modifier = Modifier
                                                .clickable {
                                                    selectedCategoryFilter = "الكل"
                                                    selectedBankFilter = "الكل"
                                                }
                                                .padding(bottom = 4.dp)
                                        )
                                    }
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                ) {
                                    items(categories) { cat ->
                                        FilterChip(
                                            label = cat,
                                            isSelected = selectedCategoryFilter == cat,
                                            onClick = { selectedCategoryFilter = cat }
                                        )
                                    }
                                }

                                // Bank filter row
                                if (uniqueBanks.size > 2) {
                                    Text(
                                        text = "تصفية حسب وسيلة الدفع أو البنك:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF49454F),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(uniqueBanks) { bank ->
                                            FilterChip(
                                                label = bank,
                                                isSelected = selectedBankFilter == bank,
                                                onClick = { selectedBankFilter = bank }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Main Upcoming list title
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الاشتراكات القريبة",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFF49454F)
                                )
                                Text(
                                    text = "عرض الكل (${filteredSubscriptions.size})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF6750A4)
                                )
                            }
                        }

                        // List Items
                        if (filteredSubscriptions.isEmpty()) {
                            item {
                                EmptyStateView(
                                    hasFilters = searchQuery.isNotEmpty() || selectedCategoryFilter != "الكل" || selectedBankFilter != "الكل"
                                )
                            }
                        } else {
                            items(filteredSubscriptions) { sub ->
                                SubscriptionListItem(
                                    subscription = sub,
                                    onRenew = { viewModel.renewSubscription(sub) },
                                    onEdit = { subscriptionToEdit = sub },
                                    onDelete = { viewModel.deleteSubscription(sub) }
                                )
                            }
                        }

                        // Bottom spacer
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
                "reports" -> {
                    GeometricReportsView(subscriptions = subscriptions, monthlyTotal = monthlyTotal)
                }
                "cards" -> {
                    GeometricCardsView(bankTotals = bankTotals, subscriptions = subscriptions)
                }
                "settings" -> {
                    GeometricSettingsView()
                }
            }
        }
    }

    // Add Subscription Dialog
    if (showAddDialog) {
        AddEditSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, amount, billingCycle, nextRenewalDate, bankName, category, isAutoRenew, alertDaysBefore, notes ->
                viewModel.addSubscription(
                    name = name,
                    amount = amount,
                    currency = "SAR",
                    billingCycle = billingCycle,
                    nextRenewalDate = nextRenewalDate,
                    bankName = bankName,
                    category = category,
                    isAutoRenew = isAutoRenew,
                    alertDaysBefore = alertDaysBefore,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    // Edit Subscription Dialog
    subscriptionToEdit?.let { sub ->
        AddEditSubscriptionDialog(
            subscription = sub,
            onDismiss = { subscriptionToEdit = null },
            onSave = { name, amount, billingCycle, nextRenewalDate, bankName, category, isAutoRenew, alertDaysBefore, notes ->
                viewModel.updateSubscription(
                    sub.copy(
                        name = name,
                        amount = amount,
                        billingCycle = billingCycle,
                        nextRenewalDate = nextRenewalDate,
                        bankName = bankName,
                        category = category,
                        isAutoRenew = isAutoRenew,
                        alertDaysBefore = alertDaysBefore,
                        notes = notes
                    )
                )
                subscriptionToEdit = null
            }
        )
    }
}

// Geometric Summary Dashboard implementation
@Composable
fun GeometricSummaryDashboard(
    monthlyTotal: Double,
    alertCount: Int
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)), // GeoPrimaryContainer
        border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "إجمالي التجديدات القادمة",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF21005D),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(
                    text = String.format(Locale("en"), "%,.1f", monthlyTotal),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ريال سعودي",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF21005D).copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Beautiful geometric status inner row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (alertCount > 0) Color(0xFFFDE8E8) else Color(0xFFDEF7EC)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (alertCount > 0) Icons.Default.Info else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (alertCount > 0) Color(0xFF9B1C1C) else Color(0xFF03543F),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (alertCount > 0) {
                        "لديك $alertCount اشتراكات مستحقة خلال الأيام القادمة"
                    } else {
                        "جميع الاشتراكات مأمنة وفي حالة سليمة"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF21005D)
                )
            }
        }
    }
}

// Reports page visualization
@Composable
fun GeometricReportsView(
    subscriptions: List<SubscriptionEntity>,
    monthlyTotal: Double
) {
    val categoryTotals = remember(subscriptions) {
        val map = mutableMapOf<String, Double>()
        subscriptions.forEach { sub ->
            val amount = sub.amount
            val monthlyEquivalent = when (sub.billingCycle) {
                "DAILY" -> amount * 30.0
                "WEEKLY" -> amount * 4.33
                "MONTHLY" -> amount
                "ANNUAL" -> amount / 12.0
                else -> amount
            }
            map[sub.category] = map.getOrDefault(sub.category, 0.0) + monthlyEquivalent
        }
        map.toList().sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = "تقارير التصنيف والنفقات",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1B20),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "توزيع الميزانية التقديرية شهرياً بناءً على التصنيفات الرياضية",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (categoryTotals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد اشتراكات كافية لحساب التحليلات البصرية.", color = Color(0xFF49454F))
                }
            }
        } else {
            items(categoryTotals) { (category, total) ->
                val percentage = if (monthlyTotal > 0) (total / monthlyTotal) else 0.0
                val colorPair = when (category) {
                    "ترفيه" -> Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8))
                    "خدمات" -> Pair(Color(0xFFFDF6B2), Color(0xFF723B13))
                    "صحة" -> Pair(Color(0xFFDEF7EC), Color(0xFF03543F))
                    "عمل" -> Pair(Color(0xFFE1EFFE), Color(0xFF1E429F))
                    "تعليم" -> Pair(Color(0xFFFCE8E6), Color(0xFFC81E1E))
                    else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorPair.first),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (category) {
                                            "ترفيه" -> Icons.Default.Star
                                            "خدمات" -> Icons.Default.Info
                                            "صحة" -> Icons.Default.Check
                                            "عمل" -> Icons.Default.Refresh
                                            "تعليم" -> Icons.Default.Star
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = colorPair.second,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1D1B20)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format(Locale("en"), "%.1f", total)} ر.س",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colorPair.second
                                )
                                Text(
                                    text = "${String.format(Locale("en"), "%.1f", percentage * 100)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom geometric layout progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF3EDF7))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = percentage.toFloat().coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colorPair.second)
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Bank Cards view representation
@Composable
fun GeometricCardsView(
    bankTotals: Map<String, Double>,
    subscriptions: List<SubscriptionEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = "البطاقات المربوطة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1B20),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "إحصائيات الخصم والمستحقات المباشرة لكل محفظة أو حساب بنكي",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (bankTotals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لم يتم العثور على أي بطاقات نشطة مضافة.", color = Color(0xFF49454F))
                }
            }
        } else {
            items(bankTotals.toList()) { (bankName, total) ->
                if (bankName.isNotBlank()) {
                    val count = subscriptions.count { it.bankName == bankName }
                    
                    // Card resembling premium credit card design (Geometric Balance)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)), // Slate Dark
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = bankName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "وسيلة دفع مسجلة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Text(
                                text = "مجموع الرسوم الشهرية",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${String.format(Locale("en"), "%,.1f", total)} ريال",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFD0BCFF)
                                )
                                Text(
                                    text = "$count اشتراكات نشطة",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Settings view representation
@Composable
fun GeometricSettingsView() {
    var alertsEnabled by remember { mutableStateOf(true) }
    var currencyType by remember { mutableStateOf("ريال سعودي (SAR)") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "الإعدادات العامة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1B20),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "تخصيص تجربة راصد الاشتراكات للتحكم الذاتي الأمثل",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "تنبيهات الإشعارات النشطة",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "إرسال تذكيرات عبر شريط الإشعارات قبل الخصم",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454F)
                            )
                        }
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { alertsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFCAC4D0).copy(alpha = 0.5f))
                    )

                    Column {
                        Text(
                            text = "العملة الافتراضية",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Surface(
                            color = Color(0xFFF3EDF7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currencyType,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF21005D),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "حول التطبيق",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = "نسخة التطبيق: 2.1.0 (Geometric Balance)\nتم التطوير باستخدام لغة Kotlin وإطار عمل Jetpack Compose بالكامل لتوفير تجربة أداء رائدة وحفاظ تام على بيانات المشترك.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF49454F)
                    )
                }
            }
        }
    }
}

// Calculate totals helper
fun calculateTotals(subscriptions: List<SubscriptionEntity>): Pair<Double, Map<String, Double>> {
    var monthlyTotal = 0.0
    val bankMap = mutableMapOf<String, Double>()

    for (sub in subscriptions) {
        val amount = sub.amount
        val monthlyEquivalent = when (sub.billingCycle) {
            "DAILY" -> amount * 30.0
            "WEEKLY" -> amount * 4.33
            "MONTHLY" -> amount
            "ANNUAL" -> amount / 12.0
            else -> amount
        }
        monthlyTotal += monthlyEquivalent

        val currentBankSum = bankMap.getOrDefault(sub.bankName, 0.0)
        bankMap[sub.bankName] = currentBankSum + monthlyEquivalent
    }
    return Pair(monthlyTotal, bankMap)
}

// Format Date helper
fun formatRenewalDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "غير محدد"
    }
}

// Calculate remaining days
fun getRemainingDays(timestamp: Long): Int {
    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val renewal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val diff = renewal - now
    return (diff / (24 * 60 * 60 * 1000L)).toInt()
}

@Composable
fun getDaysDescription(days: Int): Pair<String, Color> {
    return when {
        days < 0 -> Pair("متأخر التجديد!", Color(0xFF9B1C1C))
        days == 0 -> Pair("يتجدد اليوم!", Color(0xFF9B1C1C))
        days == 1 -> Pair("يتجدد غداً", Color(0xFF723B13))
        days == 2 -> Pair("يتجدد بعد يومين", Color(0xFF1E429F))
        else -> Pair("بقي $days يوم", Color(0xFF6750A4))
    }
}

// Filter Chip Composable
@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFFEADDFF) else Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20)
            )
        }
    }
}

// Horizontal alert items
@Composable
fun AlertSubscriptionCard(
    subscription: SubscriptionEntity,
    onRenew: () -> Unit
) {
    val remainingDays = getRemainingDays(subscription.nextRenewalDate)
    val (desc, textColor) = getDaysDescription(remainingDays)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E6)), // Warning red bg
        border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF9B1C1C),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFF9B1C1C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${subscription.amount} ريال",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF9B1C1C)
                )
                Text(
                    text = " / " + when (subscription.billingCycle) {
                        "DAILY" -> "يوم"
                        "WEEKLY" -> "أسبوع"
                        "MONTHLY" -> "شهر"
                        "ANNUAL" -> "سنة"
                        else -> "شهر"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9B1C1C).copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "خصم من: ${subscription.bankName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9B1C1C).copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRenew,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9B1C1C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تم التجديد التلقائي ✅",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
            }
        }
    }
}

// Vertical list items - Geometric Balance style
@Composable
fun SubscriptionListItem(
    subscription: SubscriptionEntity,
    onRenew: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingDays = getRemainingDays(subscription.nextRenewalDate)
    val (desc, daysColor) = getDaysDescription(remainingDays)

    val colorPair = when (subscription.category) {
        "ترفيه" -> Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8))
        "خدمات" -> Pair(Color(0xFFFDF6B2), Color(0xFF723B13))
        "صحة" -> Pair(Color(0xFFDEF7EC), Color(0xFF03543F))
        "عمل" -> Pair(Color(0xFFE1EFFE), Color(0xFF1E429F))
        "تعليم" -> Pair(Color(0xFFFCE8E6), Color(0xFFC81E1E))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorPair.first),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (subscription.category) {
                        "ترفيه" -> Icons.Default.Star
                        "خدمات" -> Icons.Default.Info
                        "صحة" -> Icons.Default.Check
                        "عمل" -> Icons.Default.Refresh
                        "تعليم" -> Icons.Default.Star
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = colorPair.second,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1D1B20)
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bank badge
                    Text(
                        text = "البنك: ${subscription.bankName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCAC4D0))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التجديد: ${formatRenewalDate(subscription.nextRenewalDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                }
                
                if (subscription.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subscription.notes,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF49454F).copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cost and remaining days
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${subscription.amount} ر.س",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = when (subscription.billingCycle) {
                        "DAILY" -> "يومياً"
                        "WEEKLY" -> "أسبوعياً"
                        "MONTHLY" -> "شهرياً"
                        "ANNUAL" -> "سنوياً"
                        else -> "شهرياً"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF49454F)
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(daysColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = daysColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions (Delete and Renew)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onRenew,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تجديد الآن",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = Color(0xFF9B1C1C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Empty State representation
@Composable
fun EmptyStateView(hasFilters: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFCAC4D0),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasFilters) "لم نجد أي اشتراكات تطابق البحث!" else "لا توجد اشتراكات مضافة بعد!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1D1B20),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasFilters) "جرب تغيير خيارات الفلترة أو ابحث باسم آخر." else "اضغط على زر (+) في الأسفل لإضافة أول اشتراك لك الآن.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF49454F),
            textAlign = TextAlign.Center
        )
    }
}

// Add/Edit Subscription Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionDialog(
    subscription: SubscriptionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        amount: Double,
        billingCycle: String,
        nextRenewalDate: Long,
        bankName: String,
        category: String,
        isAutoRenew: Boolean,
        alertDaysBefore: Int,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(subscription?.name ?: "") }
    var amountText by remember { mutableStateOf(subscription?.amount?.toString() ?: "") }
    var billingCycle by remember { mutableStateOf(subscription?.billingCycle ?: "MONTHLY") }
    var nextRenewalDate by remember { mutableStateOf(subscription?.nextRenewalDate ?: System.currentTimeMillis()) }
    var bankName by remember { mutableStateOf(subscription?.bankName ?: "") }
    var category by remember { mutableStateOf(subscription?.category ?: "ترفيه") }
    var isAutoRenew by remember { mutableStateOf(subscription?.isAutoRenew ?: true) }
    var alertDaysBefore by remember { mutableIntStateOf(subscription?.alertDaysBefore ?: 3) }
    var notes by remember { mutableStateOf(subscription?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var bankError by remember { mutableStateOf(false) }

    val billingCycles = listOf(
        Pair("DAILY", "يومي"),
        Pair("WEEKLY", "أسبوعي"),
        Pair("MONTHLY", "شهري"),
        Pair("ANNUAL", "سنوي")
    )
    val categories = listOf("ترفيه", "خدمات", "صحة", "عمل", "تعليم", "أخرى")
    val popularBanks = listOf("الراجحي", "الأهلي", "الرياض", "الإنماء", "البلاد", "أبل باي", "Visa", "STC Pay")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = if (subscription == null) "إضافة اشتراك جديد" else "تعديل الاشتراك",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF6750A4),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Name Input
                item {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = false
                            },
                            label = { Text("اسم الاشتراك (مثال: نتفليكس)") },
                            isError = nameError,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("sub_name_input")
                        )
                        if (nameError) {
                            Text(
                                text = "حقل الاسم مطلوب",
                                color = Color(0xFF9B1C1C),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }

                // Amount Input
                item {
                    Column {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                amountError = false
                            },
                            label = { Text("مبلغ الاشتراك (ريال سعودي)") },
                            isError = amountError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("sub_amount_input")
                        )
                        if (amountError) {
                            Text(
                                text = "الرجاء إدخال رقم صحيح للمبلغ",
                                color = Color(0xFF9B1C1C),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }

                // Billing Cycle row
                item {
                    Column {
                        Text(
                            text = "دورة التجديد:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF49454F),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            billingCycles.forEach { (key, name) ->
                                val isSelected = billingCycle == key
                                Surface(
                                    onClick = { billingCycle = key },
                                    color = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7),
                                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Next Renewal Date picker trigger
                item {
                    Column {
                        Text(
                            text = "تاريخ التجديد التلقائي القادم:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF49454F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = nextRenewalDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = Calendar.getInstance()
                                        selectedCal.set(year, month, dayOfMonth)
                                        nextRenewalDate = selectedCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            color = Color(0xFFF3EDF7),
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatRenewalDate(nextRenewalDate),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF1D1B20)
                                )
                                Text(
                                    text = "تغيير التاريخ",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF6750A4)
                                )
                            }
                        }
                    }
                }

                // Bank Name Input
                item {
                    Column {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = {
                                bankName = it
                                bankError = false
                            },
                            label = { Text("البنك / وسيلة الدفع (مثال: الراجحي)") },
                            isError = bankError,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("sub_bank_input")
                        )
                        if (bankError) {
                            Text(
                                text = "حقل البنك / وسيلة الدفع مطلوب",
                                color = Color(0xFF9B1C1C),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }

                        // Popular suggestions chips
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popularBanks) { bank ->
                                Surface(
                                    onClick = {
                                        bankName = bank
                                        bankError = false
                                    },
                                    color = Color(0xFFF3EDF7),
                                    border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = bank,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF1D1B20),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Selection
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تصنيف الاشتراك") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Auto renew Switch & Alert slider
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3EDF7))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "تنبيه للتجديد التلقائي",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1D1B20)
                                )
                                Text(
                                    text = "إشعار للتذكير قبل تاريخ الخصم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF49454F)
                                )
                            }
                            Switch(
                                checked = isAutoRenew,
                                onCheckedChange = { isAutoRenew = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6750A4)
                                )
                            )
                        }

                        if (isAutoRenew) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "تنبيه قبل الاستحقاق بـ:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF1D1B20)
                                )
                                Text(
                                    text = "$alertDaysBefore أيام",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF6750A4)
                                )
                            }
                            
                            // Simple interactive days chooser
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(1, 2, 3, 5, 7, 10).forEach { day ->
                                    Surface(
                                        onClick = { alertDaysBefore = day },
                                        color = if (alertDaysBefore == day) Color(0xFF6750A4) else Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(width = 44.dp, height = 30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$day ي",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = if (alertDaysBefore == day) Color.White else Color(0xFF1D1B20)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes Input
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action buttons (Save/Cancel)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                var hasError = false
                                if (name.trim().isBlank()) {
                                    nameError = true
                                    hasError = true
                                }
                                val amountVal = amountText.toDoubleOrNull()
                                if (amountVal == null || amountVal <= 0) {
                                    amountError = true
                                    hasError = true
                                }
                                if (bankName.trim().isBlank()) {
                                    bankError = true
                                    hasError = true
                                }
                                if (!hasError && amountVal != null) {
                                    onSave(
                                        name.trim(),
                                        amountVal,
                                        billingCycle,
                                        nextRenewalDate,
                                        bankName.trim(),
                                        category,
                                        isAutoRenew,
                                        alertDaysBefore,
                                        notes.trim()
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .testTag("submit_button")
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
