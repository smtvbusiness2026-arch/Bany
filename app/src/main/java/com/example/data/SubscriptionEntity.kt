package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val currency: String = "SAR",
    val billingCycle: String, // "DAILY", "WEEKLY", "MONTHLY", "ANNUAL"
    val nextRenewalDate: Long, // Timestamp of next renewal
    val bankName: String, // e.g. "الراجحي", "الأهلي", "Visa"
    val category: String, // e.g. "ترفيه", "خدمات", "صحة"
    val isAutoRenew: Boolean = true,
    val alertDaysBefore: Int = 3,
    val notes: String = ""
)
