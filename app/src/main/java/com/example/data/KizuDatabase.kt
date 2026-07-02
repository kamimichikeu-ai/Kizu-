package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE isDraft = 0 AND isArchived = 0 ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE isDraft = 0 AND isArchived = 0 AND tag = :tag ORDER BY timestamp DESC")
    fun getPostsByTag(tag: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE isDraft = 1 ORDER BY timestamp DESC")
    fun getDrafts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE isArchived = 1 AND isOwn = 1 ORDER BY timestamp DESC")
    fun getArchivedPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :id")
    fun getPostById(id: Long): Flow<Post?>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostByIdSuspend(id: Long): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post): Long

    @Update
    suspend fun updatePost(post: Post)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePostById(id: Long)
    
    @Query("UPDATE posts SET isArchived = 1 WHERE id = :id")
    suspend fun archivePostById(id: Long)

    @Query("UPDATE posts SET isArchived = 1 WHERE isDraft = 0 AND isArchived = 0 AND id NOT IN (SELECT id FROM posts WHERE isDraft = 0 AND isArchived = 0 ORDER BY timestamp DESC LIMIT 100)")
    suspend fun archiveOldPosts()

    @Query("DELETE FROM replies WHERE postId NOT IN (SELECT id FROM posts)")
    suspend fun pruneOrphanedReplies()

    @Transaction
    suspend fun insertPostAndArchive(post: Post): Long {
        val id = insertPost(post)
        if (!post.isDraft && !post.isArchived) {
            archiveOldPosts()
        }
        return id
    }

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    @Query("SELECT * FROM posts WHERE isOwn = 1 ORDER BY timestamp DESC")
    suspend fun getOwnPosts(): List<Post>
}

@Dao
interface ReplyDao {
    @Query("SELECT * FROM replies WHERE postId = :postId ORDER BY timestamp ASC")
    fun getRepliesForPost(postId: Long): Flow<List<Reply>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(reply: Reply): Long

    @Query("DELETE FROM replies WHERE postId = :postId")
    suspend fun deleteRepliesForPost(postId: Long)

    @Query("DELETE FROM replies")
    suspend fun clearAllReplies()
}

@Dao
interface CheckInDao {
    @Insert
    suspend fun insertCheckIn(checkIn: CheckIn)
    
    @Query("SELECT COUNT(*) FROM check_ins")
    fun getCheckInCount(): Flow<Int>
}

@Dao
interface EncouragementDao {
    @Insert
    suspend fun insertEncouragement(encouragement: Encouragement)

    @Query("SELECT * FROM encouragements WHERE isRead = 0")
    suspend fun getUnreadEncouragements(): List<Encouragement>

    @Query("UPDATE encouragements SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("SELECT * FROM encouragements WHERE isRead = 0")
    fun getUnreadEncouragementsFlow(): Flow<List<Encouragement>>
}

@Database(entities = [Post::class, Reply::class, CheckIn::class, Encouragement::class], version = 2, exportSchema = false)
abstract class KizuDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun replyDao(): ReplyDao
    abstract fun checkInDao(): CheckInDao
    abstract fun encouragementDao(): EncouragementDao
}
