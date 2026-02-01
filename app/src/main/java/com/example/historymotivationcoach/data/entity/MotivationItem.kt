package com.example.historymotivationcoach.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motivation_items")
data class MotivationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "quote")
    val quote: String,
    
    @ColumnInfo(name = "author")
    val author: String,
    
    @ColumnInfo(name = "context")
    val context: String? = null,
    
    @ColumnInfo(name = "image_uri")
    val imageUri: String,
    
    @ColumnInfo(name = "themes")
    val themes: List<String>,
    
    @ColumnInfo(name = "source_name")
    val sourceName: String,
    
    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,
    
    @ColumnInfo(name = "license")
    val license: String
)
