package com.example.data

import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(private val subscriptionDao: SubscriptionDao) {
    val allSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    suspend fun insert(subscription: SubscriptionEntity) {
        subscriptionDao.insertSubscription(subscription)
    }

    suspend fun update(subscription: SubscriptionEntity) {
        subscriptionDao.updateSubscription(subscription)
    }

    suspend fun delete(subscription: SubscriptionEntity) {
        subscriptionDao.deleteSubscription(subscription)
    }

    suspend fun getById(id: Int): SubscriptionEntity? {
        return subscriptionDao.getSubscriptionById(id)
    }
}
