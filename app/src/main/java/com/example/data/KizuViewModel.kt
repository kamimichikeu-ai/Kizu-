package com.example.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KizuViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("kizu_prefs", Context.MODE_PRIVATE)

    val database: KizuDatabase by lazy {
        Room.databaseBuilder(
            application,
            KizuDatabase::class.java,
            "kizu_database"
        ).fallbackToDestructiveMigration().build()
    }

    val repository: KizuRepository by lazy {
        KizuRepository(database)
    }

    private val _authorName = MutableStateFlow(sharedPrefs.getString("display_name", "Anonymous") ?: "Anonymous")
    val authorName: StateFlow<String> = _authorName.asStateFlow()

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedPostId = MutableStateFlow<Long?>(null)
    val selectedPostId: StateFlow<Long?> = _selectedPostId.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val posts: StateFlow<List<Post>> by lazy {
        _selectedTag.flatMapLatest { tag ->
            repository.getPostsByTag(tag)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val drafts: StateFlow<List<Post>> by lazy {
        repository.drafts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val archivedPosts: StateFlow<List<Post>> by lazy {
        repository.archivedPosts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val checkInCount: StateFlow<Int> by lazy {
        repository.checkInCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    }

    val unreadEncouragements: StateFlow<List<Encouragement>> by lazy {
        repository.unreadEncouragements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedPost: StateFlow<Post?> by lazy {
        _selectedPostId.flatMapLatest { id ->
            if (id != null) {
                repository.getPostById(id)
            } else {
                flowOf(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedPostReplies: StateFlow<List<Reply>> by lazy {
        _selectedPostId.flatMapLatest { id ->
            if (id != null) {
                repository.getRepliesForPost(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setDisplayName(newName: String) {
        val trimmed = newName.trim()
        val finalName = if (trimmed.isEmpty()) "Anonymous" else trimmed
        _authorName.value = finalName
        sharedPrefs.edit().putString("display_name", finalName).apply()
    }

    fun toggleDarkMode() {
        val nextVal = !_isDarkMode.value
        _isDarkMode.value = nextVal
        sharedPrefs.edit().putBoolean("dark_mode", nextVal).apply()
    }

    fun selectPost(postId: Long?) {
        _selectedPostId.value = postId
    }

    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
    }

    fun createPost(content: String, tag: String?, isDraft: Boolean) {
        viewModelScope.launch {
            repository.createPost(
                authorName = _authorName.value,
                content = content,
                isOwn = true,
                tag = tag,
                isDraft = isDraft
            )
        }
    }
    
    fun publishDraft(post: Post) {
        viewModelScope.launch {
            repository.publishDraft(post)
        }
    }

    fun createReply(postId: Long, content: String) {
        viewModelScope.launch {
            repository.createReply(
                postId = postId,
                authorName = _authorName.value,
                content = content,
                isOwn = true
            )
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            repository.deletePost(postId)
            if (_selectedPostId.value == postId) {
                _selectedPostId.value = null
            }
        }
    }
    
    fun permanentlyDeletePost(postId: Long) {
        viewModelScope.launch {
            repository.permanentlyDeletePost(postId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            _selectedPostId.value = null
        }
    }

    fun addCheckIn() {
        viewModelScope.launch {
            repository.addCheckIn()
        }
    }

    fun passItForward(postId: Long) {
        viewModelScope.launch {
            val messages = listOf(
                "Someone read your post. You are not alone.",
                "Your words reached someone. Keep going.",
                "You were heard. That is enough."
            )
            val msg = messages.random()
            repository.addEncouragement(postId, msg)
        }
    }

    fun markEncouragementRead(id: Long) {
        viewModelScope.launch {
            repository.markEncouragementRead(id)
        }
    }

    suspend fun compileOwnPostsText(): String {
        val ownPosts = repository.getOwnPosts()
        if (ownPosts.isEmpty()) return ""

        val sb = java.lang.StringBuilder()
        sb.append("Kizu - My Anonymous Stories & Thoughts\n")
        sb.append("======================================\n\n")
        ownPosts.forEachIndexed { index, post ->
            val dateStr = java.text.DateFormat.getDateTimeInstance()
                .format(java.util.Date(post.timestamp))
            sb.append("Post #${index + 1}\n")
            sb.append("Author: ${post.authorName} (You)\n")
            if (post.tag != null) sb.append("Tag: ${post.tag}\n")
            sb.append("Timestamp: $dateStr\n")
            sb.append("Content:\n")
            sb.append(post.content).append("\n")
            sb.append("--------------------------------------\n\n")
        }
        return sb.toString()
    }
}
