package com.example.data

import kotlinx.coroutines.flow.Flow

class KizuRepository(private val database: KizuDatabase) {
    private val postDao = database.postDao()
    private val replyDao = database.replyDao()
    private val checkInDao = database.checkInDao()
    private val encouragementDao = database.encouragementDao()

    val allPosts: Flow<List<Post>> = postDao.getAllPosts()
    val drafts: Flow<List<Post>> = postDao.getDrafts()
    val archivedPosts: Flow<List<Post>> = postDao.getArchivedPosts()
    val checkInCount: Flow<Int> = checkInDao.getCheckInCount()
    val unreadEncouragements: Flow<List<Encouragement>> = encouragementDao.getUnreadEncouragementsFlow()

    fun getPostsByTag(tag: String?): Flow<List<Post>> {
        return if (tag == null) postDao.getAllPosts() else postDao.getPostsByTag(tag)
    }

    fun getPostById(postId: Long): Flow<Post?> {
        return postDao.getPostById(postId)
    }

    fun getRepliesForPost(postId: Long): Flow<List<Reply>> {
        return replyDao.getRepliesForPost(postId)
    }

    suspend fun createPost(authorName: String, content: String, isOwn: Boolean, tag: String?, isDraft: Boolean): Long {
        val post = Post(
            authorName = authorName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isOwn = isOwn,
            replyCount = 0,
            tag = tag,
            isDraft = isDraft
        )
        return postDao.insertPostAndArchive(post)
    }

    suspend fun publishDraft(post: Post) {
        postDao.insertPostAndArchive(post.copy(isDraft = false, timestamp = System.currentTimeMillis()))
    }

    suspend fun createReply(postId: Long, authorName: String, content: String, isOwn: Boolean) {
        val reply = Reply(
            postId = postId,
            authorName = authorName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isOwn = isOwn
        )
        replyDao.insertReply(reply)
        val post = postDao.getPostByIdSuspend(postId)
        if (post != null) {
            postDao.updatePost(post.copy(replyCount = post.replyCount + 1))
        }
    }

    suspend fun deletePost(postId: Long) {
        postDao.archivePostById(postId)
    }
    
    suspend fun permanentlyDeletePost(postId: Long) {
        postDao.deletePostById(postId)
        replyDao.deleteRepliesForPost(postId)
    }

    suspend fun clearAll() {
        postDao.clearAllPosts()
        replyDao.clearAllReplies()
    }

    suspend fun getOwnPosts(): List<Post> {
        return postDao.getOwnPosts()
    }

    suspend fun addCheckIn() {
        checkInDao.insertCheckIn(CheckIn(timestamp = System.currentTimeMillis()))
    }

    suspend fun addEncouragement(postId: Long, message: String) {
        val post = postDao.getPostByIdSuspend(postId)
        if (post != null && post.isOwn) {
            encouragementDao.insertEncouragement(Encouragement(postId = postId, message = message))
        }
    }

    suspend fun markEncouragementRead(id: Long) {
        encouragementDao.markAsRead(id)
    }
}
