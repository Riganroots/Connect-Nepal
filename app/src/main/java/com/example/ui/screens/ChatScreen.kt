package com.example.ui.screens

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.MessageEntity
import com.example.ui.viewmodel.ConnectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    activityId: Int,
    viewModel: ConnectViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity by viewModel.getActivityStream(activityId).collectAsStateWithLifecycle(initialValue = null)
    val messages by viewModel.getMessagesStream(activityId).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Safety and Context States
    var showSafetyReminder by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showSharePlaceDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Inappropriate behavior") }
    var reportComments by remember { mutableStateOf("") }

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activity == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val act = activity!!

    // Dialog for Block Confirmation
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block ${act.organizerName}?") },
            text = { Text("Are you sure you want to block ${act.organizerName}? You will no longer see their activities or messages in nearby feeds.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.blockUser(act.organizerId)
                        Toast.makeText(context, "${act.organizerName} has been blocked successfully.", Toast.LENGTH_SHORT).show()
                        showBlockDialog = false
                        onNavigateBack() // Pop back safety
                    },
                    modifier = Modifier.testTag("confirm_block_btn")
                ) {
                    Text("Block", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Report
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report ${act.organizerName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a reason for reporting:")
                    val reasons = listOf("Spam", "Harassment", "Suspicious Activity", "Inappropriate behavior", "Other")
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = reportReason == reason,
                                onClick = { reportReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = reportComments,
                        onValueChange = { reportComments = it },
                        label = { Text("Additional comments (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("report_comments_input"),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reportUser(act.organizerId, reportReason + ": " + reportComments)
                        Toast.makeText(context, "Thank you. ${act.organizerName} has been reported.", Toast.LENGTH_LONG).show()
                        showReportDialog = false
                    },
                    modifier = Modifier.testTag("confirm_report_btn")
                ) {
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Share Public Place
    if (showSharePlaceDialog) {
        val safePlaces = listOf(
            "Himalayan Java Cafe, Baneshwor",
            "Fire and Ice Pizzeria, Thamel",
            "Patan Durbar Square, Lalitpur",
            "Boudhanath Stupa, Boudha",
            "Garden of Dreams, Thamel"
        )
        AlertDialog(
            onDismissRequest = { showSharePlaceDialog = false },
            title = { Text("Share Public Meeting Place") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Recommend a secure, well-populated public space for meeting up:")
                    safePlaces.forEach { place ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.sendChatMessage(act.id, "[SHARE_PLACE]: $place")
                                    showSharePlaceDialog = false
                                }
                                .testTag("share_place_$place")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(place, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSharePlaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = rememberAsyncImagePainter(act.coverImageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = act.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "${act.participantCount} members online",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.testTag("chat_menu_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Request to Connect", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                val name = currentUser?.name ?: "Someone"
                                viewModel.sendChatMessage(act.id, "[CONNECT_REQUEST]: $name")
                            },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            modifier = Modifier.testTag("menu_connect_request")
                        )
                        DropdownMenuItem(
                            text = { Text("Share Public Place", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                showSharePlaceDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.testTag("menu_share_place")
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Block Organizer", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showBlockDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.testTag("menu_block_user")
                        )
                        DropdownMenuItem(
                            text = { Text("Report Organizer", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.testTag("menu_report_user")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Meetup Safety Reminder Banner (Dismissible card)
            AnimatedVisibility(
                visible = showSafetyReminder,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("meetup_safety_reminder")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Safety Shield",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Meetup Safety First! 🛡️",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(
                                onClick = { showSafetyReminder = false },
                                modifier = Modifier.size(24.dp).testTag("dismiss_safety_btn")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "For a safe & positive experience, please observe these rules:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val tips = listOf(
                            "Coordinate and meet ONLY in busy, well-lit public spaces.",
                            "Always tell a friend or family member where you are going.",
                            "Do not share highly private personal or financial information.",
                            "Ensure your own secure transportation to and from the venue."
                        )
                        tips.forEach { tip ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info header on top
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "🛡️ Group Chat: This chat is used to coordinate meeting details for \"${act.title}\". Enjoy connecting and meeting new friends safely!",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                items(messages) { message ->
                    val isOwnMessage = currentUser != null && message.senderId == currentUser!!.id
                    InteractiveChatBubble(message = message, isOwnMessage = isOwnMessage)
                }
            }

            // Quick Shortcut Row for Connect / Share Place
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        val name = currentUser?.name ?: "Someone"
                        viewModel.sendChatMessage(act.id, "[CONNECT_REQUEST]: $name")
                    },
                    label = { Text("Request to Connect") },
                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("shortcut_connect_btn")
                )
                AssistChip(
                    onClick = { showSharePlaceDialog = true },
                    label = { Text("Share Safe Place") },
                    leadingIcon = { Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("shortcut_share_place_btn")
                )
            }

            // Input Bar
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val textToSend = messageText
                                viewModel.sendChatMessage(act.id, textToSend)
                                messageText = ""

                                // Trigger a simulated cool reply from other joined members after 2 seconds
                                coroutineScope.launch {
                                    delay(2000)
                                    val mockReplies = listOf(
                                        "That sounds great! See you there on time.",
                                        "Awesome, I'll be coming directly from Baneshwor.",
                                        "Let's meet exactly at the suggested public place.",
                                        "Can't wait! Stay safe everyone.",
                                        "Perfect. I am bringing a camera too."
                                    )
                                    viewModel.sendChatMessage(act.id, mockReplies.random())
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("send_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveChatBubble(message: MessageEntity, isOwnMessage: Boolean) {
    val isSystemMessage = message.senderName == "System" || message.senderName == "system" || (message.senderAvatar.isEmpty() && !isOwnMessage)

    if (isSystemMessage) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else if (message.text.startsWith("[CONNECT_REQUEST]:")) {
        // Special Connection Request Card
        val requesterName = message.text.removePrefix("[CONNECT_REQUEST]:").trim()
        var connectResult by remember { mutableStateOf<String?>(null) } // null, "accepted", "declined"
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("interactive_connect_request_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connection Request",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$requesterName would like to connect with you to plan activities together!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (connectResult == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    connectResult = "accepted"
                                    Toast.makeText(context, "You accepted $requesterName's connection request! 🤝", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("accept_connection_btn")
                            ) {
                                Text("Accept", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    connectResult = "declined"
                                    Toast.makeText(context, "Connection request declined.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("decline_connection_btn")
                            ) {
                                Text("Decline", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (connectResult == "accepted") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (connectResult == "accepted") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (connectResult == "accepted") "Connection Accepted! 🤝" else "Request Declined",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (connectResult == "accepted") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    } else if (message.text.startsWith("[SHARE_PLACE]:")) {
        // Special Share Safe Place Card
        val sharedPlaceName = message.text.removePrefix("[SHARE_PLACE]:").trim()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("interactive_share_place_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Safe Meeting Spot",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Suggested Public Meeting Place:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = sharedPlaceName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Let's coordinate to gather at this highly public and secure venue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isOwnMessage && message.senderAvatar.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(message.senderAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
            ) {
                if (!isOwnMessage) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isOwnMessage) 16.dp else 0.dp,
                                bottomEnd = if (isOwnMessage) 0.dp else 16.dp
                            )
                        )
                        .background(
                            if (isOwnMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 260.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
