package com.example.historymotivationcoach.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "delivery_history",
    foreignKeys = [
        ForeignKey(
            entity = MotivationItem::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("item_id"),
        Index("date_key"),
        Index("shown_at"),
        // Composite index for optimizing date-based queries with time sorting
        // This improves performance for getHistoryByDate and getMotivationsWithHistoryByDate
        Index(value = ["date_key", "shown_at"], name = "index_date_time")
    ]
)
data class DeliveryHistory(
    @PrimaryKey(autoGenerate = true)
    val historyId: Long = 0,
    
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    
    @ColumnInfo(name = "shown_at")
    val shownAt: Long, // Unix timestamp
    
    @ColumnInfo(name = "date_key")
    val dateKey: String, // YYYY-MM-DD format
    
    @ColumnInfo(name = "notification_id")
    val notificationId: Int,
    
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: DeliveryStatus
)

enum class DeliveryStatus {
    DELIVERED,
    OPENED,
    DISMISSED
}
