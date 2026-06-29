package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SubscriptionEntity
import com.example.data.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SubscriptionRepository
    val subscriptions: StateFlow<List<SubscriptionEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SubscriptionRepository(database.subscriptionDao())
        
        subscriptions = repository.allSubscriptions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate database with realistic Arabic subscriptions if empty
        viewModelScope.launch {
            repository.allSubscriptions.collect { list ->
                if (list.isEmpty()) {
                    prepopulateData()
                }
            }
        }
    }

    private suspend fun prepopulateData() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val defaults = listOf(
            SubscriptionEntity(
                name = "أبل ون (Apple One)",
                amount = 39.0,
                currency = "SAR",
                billingCycle = "MONTHLY",
                nextRenewalDate = now + (1 * oneDayMs),
                bankName = "الراجحي",
                category = "ترفيه",
                isAutoRenew = true,
                alertDaysBefore = 2,
                notes = "باقة أبل الموسيقية والسحابية العائلية"
            ),
            SubscriptionEntity(
                name = "نتفليكس (Netflix)",
                amount = 45.0,
                currency = "SAR",
                billingCycle = "MONTHLY",
                nextRenewalDate = now + (3 * oneDayMs),
                bankName = "الأهلي",
                category = "ترفيه",
                isAutoRenew = true,
                alertDaysBefore = 3,
                notes = "اشتراك الباقة الفائقة 4K"
            ),
            SubscriptionEntity(
                name = "فاتورة الكهرباء (شركة الكهرباء)",
                amount = 320.0,
                currency = "SAR",
                billingCycle = "MONTHLY",
                nextRenewalDate = now + (7 * oneDayMs),
                bankName = "الرياض",
                category = "خدمات",
                isAutoRenew = true,
                alertDaysBefore = 5,
                notes = "الفاتورة التقديرية للمنزل"
            ),
            SubscriptionEntity(
                name = "نادي وقت اللياقة (Fitness Time)",
                amount = 1200.0,
                currency = "SAR",
                billingCycle = "ANNUAL",
                nextRenewalDate = now + (45 * oneDayMs),
                bankName = "الإنماء",
                category = "صحة",
                isAutoRenew = false,
                alertDaysBefore = 7,
                notes = "اشتراك برو السنوي"
            ),
            SubscriptionEntity(
                name = "سبوتيفاي عائلي (Spotify)",
                amount = 29.0,
                currency = "SAR",
                billingCycle = "MONTHLY",
                nextRenewalDate = now + (5 * oneDayMs),
                bankName = "أبل باي",
                category = "ترفيه",
                isAutoRenew = true,
                alertDaysBefore = 2,
                notes = "باقة الموسيقى العائلية لـ 6 حسابات"
            )
        )

        for (item in defaults) {
            repository.insert(item)
        }
    }

    fun addSubscription(
        name: String,
        amount: Double,
        currency: String,
        billingCycle: String,
        nextRenewalDate: Long,
        bankName: String,
        category: String,
        isAutoRenew: Boolean,
        alertDaysBefore: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val entity = SubscriptionEntity(
                name = name,
                amount = amount,
                currency = currency,
                billingCycle = billingCycle,
                nextRenewalDate = nextRenewalDate,
                bankName = bankName,
                category = category,
                isAutoRenew = isAutoRenew,
                alertDaysBefore = alertDaysBefore,
                notes = notes
            )
            repository.insert(entity)
        }
    }

    fun updateSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.update(subscription)
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.delete(subscription)
        }
    }

    fun renewSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = subscription.nextRenewalDate
            
            when (subscription.billingCycle) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                "ANNUAL" -> calendar.add(Calendar.YEAR, 1)
                else -> calendar.add(Calendar.MONTH, 1)
            }

            val updated = subscription.copy(
                nextRenewalDate = calendar.timeInMillis
            )
            repository.update(updated)
        }
    }
}
