package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.Encouragement
import com.example.data.KizuViewModel
import com.example.data.Post
import com.example.data.Reply
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: KizuViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDark) {
                KizuApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KizuApp(viewModel: KizuViewModel) {
    val authorName by viewModel.authorName.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val selectedPostId by viewModel.selectedPostId.collectAsStateWithLifecycle()

    var showNameModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var titleTapCount by remember { mutableIntStateOf(0) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    var currentScreen by remember { mutableStateOf("board") }

    // Dialog for custom pen name
    if (showNameModal) {
        NameModal(
            currentName = authorName,
            onDismiss = { showNameModal = false },
            onSave = { newName ->
                viewModel.setDisplayName(newName)
                showNameModal = false
            }
        )
    }

    // Modal for Room settings (hidden, revealed in top bar click x5)
    if (showSettingsModal) {
        SettingsModal(
            viewModel = viewModel,
            onDismiss = { showSettingsModal = false },
            onViewEmbers = { 
                currentScreen = "embers"
                showSettingsModal = false
            }
        )
    }

    // AlertDialog to verify card long-press deletion of own trauma story
    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete story", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this trauma story? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    postToDelete?.let { viewModel.deletePost(it.id) }
                    postToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.secondary
        )
    }

    var showQuickPost by remember { mutableStateOf(false) }

    if (showQuickPost) {
        QuickPostDialog(
            onDismiss = { showQuickPost = false },
            onPost = { content ->
                viewModel.createPost(content, tag = null, isDraft = false)
                showQuickPost = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            if (selectedPostId == null) {
                FloatingActionButton(
                    onClick = { showQuickPost = true },
                    containerColor = Color(0xFFFFB300), // Amber
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "I Need to Be Seen")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Elegant Header reflecting Immersive UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tapping 5 times reveals Settings modal
                Column(
                    modifier = Modifier
                        .testTag("settings_trigger")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            titleTapCount++
                            if (titleTapCount >= 5) {
                                showSettingsModal = true
                                titleTapCount = 0
                            }
                        }
                ) {
                    Text(
                        text = "Kizu",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp, // tracking-tight
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "ANONYMOUS SPACE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 2.sp, // tracking-[0.2em]
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)
                        )
                    )
                }

                // Current pen name styled exactly like the button design in header
                Box(
                    modifier = Modifier
                        .testTag("change_name_trigger")
                        .clickable { showNameModal = true }
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // Body navigation logic
            if (currentScreen == "board") {
                if (selectedPostId == null) {
                    Column(modifier = Modifier.weight(1f)) {
                        BoardView(
                            viewModel = viewModel,
                            posts = posts,
                            onPostLongClick = { post ->
                                if (post.isOwn) {
                                    postToDelete = post
                                }
                            },
                            onViewDrafts = { currentScreen = "drafts" }
                        )
                    }
                    BoardFooter(remainingSpots = (100 - posts.size).coerceAtLeast(0))
                } else {
                    val selectedPost by viewModel.selectedPost.collectAsStateWithLifecycle()
                    val replies by viewModel.selectedPostReplies.collectAsStateWithLifecycle()
    
                    PostDetailView(
                        selectedPost = selectedPost,
                        replies = replies,
                        viewModel = viewModel,
                        onBackClick = { viewModel.selectPost(null) },
                        onPostLongClick = { post ->
                            if (post.isOwn) {
                                postToDelete = post
                            }
                        }
                    )
                }
            } else if (currentScreen == "drafts") {
                DraftsView(
                    viewModel = viewModel,
                    onBackClick = { currentScreen = "board" },
                    onEditDraft = { draft -> 
                        // Simplified: we'll just publish the draft for now or delete it
                    }
                )
            } else if (currentScreen == "embers") {
                EmbersView(
                    viewModel = viewModel,
                    onBackClick = { currentScreen = "board" }
                )
            }
        }
    }
}

@Composable
fun BoardFooter(remainingSpots: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surface, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Data remains local • $remainingSpots spots remaining".uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.20f)
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BoardView(
    viewModel: KizuViewModel,
    posts: List<Post>,
    onPostLongClick: (Post) -> Unit,
    onViewDrafts: () -> Unit
) {
    var postText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var isDraft by remember { mutableStateOf(false) }
    val availableTags = listOf("#abuse", "#grief", "#suicide", "#illness", "#identity", "#healing")
    
    var showTagDropdown by remember { mutableStateOf(false) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    
    val currentFilterTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (drafts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewDrafts() }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "You have ${drafts.size} unsent letter${if(drafts.size > 1) "s" else ""}.",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Post Creation Section Pinned at Top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = postText,
                onValueChange = { if (it.length <= 500) postText = it },
                placeholder = {
                    Text(
                        text = "Write your post here... (share something, ask something, say something)",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .testTag("post_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                maxLines = 6,
                shape = RoundedCornerShape(16.dp)
            )
            
            // Post options row (Tags, Draft toggle)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag selector
                Box {
                    Text(
                        text = selectedTag ?: "Add Tag +",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selectedTag != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clickable { showTagDropdown = true }
                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    
                    DropdownMenu(
                        expanded = showTagDropdown,
                        onDismissRequest = { showTagDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { selectedTag = null; showTagDropdown = false }
                        )
                        availableTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = { selectedTag = tag; showTagDropdown = false }
                            )
                        }
                    }
                }
                
                // Draft toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Unsent Letter",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isDraft,
                        onCheckedChange = { isDraft = it },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${postText.length} / 500",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = if (postText.length > 450) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    }
                )

                val hasContent = postText.isNotBlank() && postText.length <= 500
                OutlinedButton(
                    onClick = {
                        if (hasContent) {
                            viewModel.createPost(postText, selectedTag, isDraft)
                            postText = ""
                            selectedTag = null
                            isDraft = false
                        }
                    },
                    enabled = hasContent,
                    border = BorderStroke(
                        1.dp,
                        if (hasContent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("post_button")
                ) {
                    Text(text = if (isDraft) "Save Draft" else "Post", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }

        // Section Title: Recent Stories with fine line and filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT STORIES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface, thickness = 1.dp, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showFilterDropdown = true }
                ) {
                    Text(
                        text = currentFilterTag ?: "Filter",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Stories") },
                        onClick = { viewModel.setTagFilter(null); showFilterDropdown = false }
                    )
                    availableTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = { viewModel.setTagFilter(tag); showFilterDropdown = false }
                        )
                    }
                }
            }
        }

        // Scrollable Bulletin Board
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "The room is still. Speak when you are ready.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onCardClick = { viewModel.selectPost(post.id) },
                        onLongClick = { onPostLongClick(post) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val postAuthor = if (post.isOwn) "${post.authorName} (You)" else post.authorName

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        if (post.tag != null) {
            Text(
                text = post.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp)
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = onCardClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = postAuthor,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Text(
                    text = formatTime(post.timestamp).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.80f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Bottom actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REPLY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.clickable { onCardClick() }
                )

                Spacer(modifier = Modifier.width(16.dp))

                val responsesText = when (post.replyCount) {
                    0 -> "NO RESPONSES YET"
                    1 -> "1 RESPONSE"
                    else -> "${post.replyCount} RESPONSES"
                }

                Text(
                    text = responsesText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    )
                )
            }
        }
    }
    }
}

@Composable
fun PostDetailView(
    selectedPost: Post?,
    replies: List<Reply>,
    viewModel: KizuViewModel,
    onBackClick: () -> Unit,
    onPostLongClick: (Post) -> Unit
) {
    if (selectedPost == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Story not found.",
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "← BACK TO BOARD",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBackClick() }
            )
        }
        return
    }

    var replyText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back Link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            Text(
                text = "← BACK TO BOARD",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            // Header Post Content
            item {
                PostCard(
                    post = selectedPost,
                    onCardClick = { /* No-op: already inside details */ },
                    onLongClick = { onPostLongClick(selectedPost) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESPONSES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surface, thickness = 1.dp, modifier = Modifier.weight(1f))
                }
            }

            // Replies scroll
            if (replies.isEmpty()) {
                item {
                    Text(
                        text = "The room is quiet. Offer a response when you are ready.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 24.dp)
                    )
                }
            } else {
                items(replies, key = { it.id }) { reply ->
                    ReplyItem(reply = reply)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        onClick = { viewModel.addCheckIn() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Heart", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I Am Still Here (Private Check-in)")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.passItForward(selectedPost.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pass It Forward (Send anonymous encouragement)")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Reply input bar fixed at screen bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surface, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = replyText,
                onValueChange = { if (it.length <= 500) replyText = it },
                placeholder = {
                    Text(
                        text = "Write a response...",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .testTag("reply_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                maxLines = 4,
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${replyText.length} / 500",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = if (replyText.length > 450) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    }
                )

                val hasReply = replyText.isNotBlank() && replyText.length <= 500
                OutlinedButton(
                    onClick = {
                        if (hasReply) {
                            viewModel.createReply(selectedPost.id, replyText)
                            replyText = ""
                        }
                    },
                    enabled = hasReply,
                    border = BorderStroke(
                        1.dp,
                        if (hasReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("send_reply_button")
                ) {
                    Text(text = "Send", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ReplyItem(reply: Reply) {
    val authorText = if (reply.isOwn) "${reply.authorName} (You)" else reply.authorName

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.03f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = authorText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (reply.isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )

                Text(
                    text = formatTime(reply.timestamp).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reply.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.80f)
                )
            )
        }
    }
}

@Composable
fun NameModal(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textState by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Edit Pen Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "This display name is stored locally and will be shown beside your stories and responses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = textState,
                    onValueChange = { if (it.length <= 30) textState = it },
                    singleLine = true,
                    placeholder = { Text("Enter display name...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "[Cancel]",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    TextButton(
                        onClick = { onSave(textState) }
                    ) {
                        Text(
                            text = "[Save]",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsModal(
    viewModel: KizuViewModel,
    onDismiss: () -> Unit,
    onViewEmbers: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    var showConfirmDelete by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Room Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Local offline storage preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (showConfirmDelete) {
                    // Quick inside dialogue block for database clear confirmation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Are you absolutely sure you want to erase all posts and responses from this device? This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showConfirmDelete = false }) {
                                Text("[Undo]", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            TextButton(onClick = {
                                viewModel.clearAllData()
                                showConfirmDelete = false
                                onDismiss()
                            }) {
                                Text("[Erase]", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Standard settings items
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Core setting 1: Theme Mood Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleDarkMode() }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aesthetic mood",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkMode) "[Dim Room]" else "[Light Room]",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.testTag("theme_toggle_button")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))

                        // The Embers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewEmbers() }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "The Embers (Archive)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "[View]",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Core setting 2: Export own stories file
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        val data = viewModel.compileOwnPostsText()
                                        if (data.isNotEmpty()) {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "My Kizu Posts")
                                                putExtra(Intent.EXTRA_TEXT, data)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(
                                                    intent,
                                                    "Export My Posts"
                                                )
                                            )
                                            onDismiss()
                                        } else {
                                            exportStatusMessage = "No stories authored yet."
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Export my posts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (exportStatusMessage.isNotEmpty()) {
                                    Text(
                                        text = exportStatusMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = "[Share .txt]",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.testTag("export_stories_button")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))

                        // Core setting 3: Erase all data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showConfirmDelete = true }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Erase all elements",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "[Clear Board]",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.testTag("clear_board_button")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "[Close]",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(date)
        }
    }
}

@Composable
fun QuickPostDialog(
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "I NEED TO BE SEEN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFFFFB300),
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            text = "Say what you need to say. No one will judge you here.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { if (content.isNotBlank()) onPost(content) },
                        enabled = content.isNotBlank(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFB300),
                            disabledContentColor = Color(0xFFFFB300).copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = if (content.isNotBlank()) 1f else 0.3f))
                    ) {
                        Text("Release")
                    }
                }
            }
        }
    }
}

@Composable
fun DraftsView(
    viewModel: KizuViewModel,
    onBackClick: () -> Unit,
    onEditDraft: (Post) -> Unit
) {
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "← BACK TO BOARD",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBackClick() }
            )
        }
        
        Text("Unsent Letters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        
        if (drafts.isEmpty()) {
            Text("You have no unsent letters.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        } else {
            LazyColumn {
                items(drafts) { draft ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEditDraft(draft) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(draft.content, style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { viewModel.publishDraft(draft) }) {
                                    Text("Send")
                                }
                                TextButton(onClick = { viewModel.deletePost(draft.id) }) {
                                    Text("Discard", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmbersView(
    viewModel: KizuViewModel,
    onBackClick: () -> Unit
) {
    val embers by viewModel.archivedPosts.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "← BACK TO BOARD",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBackClick() }
            )
        }
        
        Text("The Embers", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        
        if (embers.isEmpty()) {
            Text("No past stories stored here.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        } else {
            LazyColumn {
                items(embers) { ember ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(formatTime(ember.timestamp), style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(ember.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
