package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val content: String,
    val timestamp: Long,
    val isOwn: Boolean = false,
    val replyCount: Int = 0,
    val tag: String? = null,
    val isDraft: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "replies")
data class Reply(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorName: String,
    val content: String,
    val timestamp: Long,
    val isOwn: Boolean = false
)

@Entity(tableName = "check_ins")
data class CheckIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long
)

@Entity(tableName = "encouragements")
data class Encouragement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val message: String,
    val isRead: Boolean = false
)
