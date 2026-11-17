package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FriendContact
import com.example.ui.components.getAvatarDrawableRes
import com.example.ui.components.hexToColor
import com.example.ui.theme.TechColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val friends by viewModel.friendsList.collectAsStateWithLifecycle()
    val availablePeers by viewModel.availablePeersToAdd.collectAsStateWithLifecycle()
    val activeChatFriend by viewModel.activeChatFriend.collectAsStateWithLifecycle()
    val messagesMap by viewModel.chatMessagesMap.collectAsStateWithLifecycle()

    val currentGroup = viewModel.normalizeRoleGroup(currentUser?.role)

    var messageInput by remember { mutableStateOf("") }
    var chatError by remember { mutableStateOf("") }
    var showAddPeerDialog by remember { mutableStateOf(false) }

    if (showAddPeerDialog) {
        AddGroupPeerDialog(
            groupName = currentGroup,
            peers = availablePeers,
            onDismiss = { showAddPeerDialog = false },
            onSelectPeer = { peer ->
                val res = viewModel.addPeerToChat(peer)
                if (res.first) {
                    showAddPeerDialog = false
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (activeChatFriend == null) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chat de $currentGroup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Solo puedes agregar y chatear con miembros de $currentGroup.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showAddPeerDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_chat_contact_button")
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Agregar miembro de $currentGroup",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Agregar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (friends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aún no tienes chats con $currentGroup",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { showAddPeerDialog = true }) {
                                Text("Agregar compañero de grupo")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(friends, key = { it.id }) { friend ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openChatWith(friend) }
                                    .testTag("chat_contact_${friend.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
                                        val borderColor = hexToColor(friend.avatarColorHex)
                                        val avatarRes = getAvatarDrawableRes(friend.avatarId)
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E1E1E))
                                                .border(width = 2.dp, color = borderColor, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = avatarRes),
                                                contentDescription = "Avatar de ${friend.name}",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        if (friend.isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color(0xFF34C759), CircleShape)
                                                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                                    .align(Alignment.BottomEnd)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = friend.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = TechColors.NeonCyan.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = viewModel.normalizeRoleGroup(friend.role),
                                                    color = TechColors.NeonCyan,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = friend.lastMessage,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }

                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val friend = activeChatFriend!!
            val messages = messagesMap[friend.id] ?: emptyList()

            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.closeChat() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TechColors.NeonCyan)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        val borderColor = hexToColor(friend.avatarColorHex)
                        val avatarRes = getAvatarDrawableRes(friend.avatarId)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E1E))
                                .border(width = 2.dp, color = borderColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "Avatar de ${friend.name}",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = friend.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (friend.isOnline) "En línea • ${viewModel.normalizeRoleGroup(friend.role)}" else viewModel.normalizeRoleGroup(friend.role),
                                color = TechColors.NeonCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed(), key = { it.id }) { msg ->
                        val isMe = msg.isFromMe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ),
                                color = if (isMe) TechColors.NeonCyan else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = msg.text,
                                        color = if (isMe) Color.Black else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.timestamp,
                                        color = if (isMe) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                if (chatError.isNotBlank()) {
                    Text(
                        text = chatError,
                        color = Color(0xFFFF3B30),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = {
                                messageInput = it
                                chatError = ""
                            },
                            placeholder = { Text("Escribe un mensaje respetuoso...") },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TechColors.NeonCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val result = viewModel.sendChatMessage(friend.id, messageInput)
                                if (result.first) {
                                    messageInput = ""
                                    chatError = ""
                                } else {
                                    chatError = result.second ?: "No permitido"
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(TechColors.NeonCyan, CircleShape)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddGroupPeerDialog(
    groupName: String,
    peers: List<FriendContact>,
    onDismiss: () -> Unit,
    onSelectPeer: (FriendContact) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPeers = remember(peers, searchQuery) {
        if (searchQuery.isBlank()) peers
        else peers.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = TechColors.TechCard),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(vertical = 8.dp)
                .testTag("add_group_peer_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Text(
                    text = "Agregar al Chat ($groupName)",
                    color = TechColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Solo se muestran integrantes de tu grupo ($groupName).",
                    color = TechColors.NeonCyan,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre...", color = TechColors.TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TechColors.NeonCyan) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredPeers.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (peers.isEmpty()) {
                                "Ya agregaste a todos los integrantes disponibles de $groupName."
                            } else {
                                "No se encontraron coincidencias en $groupName."
                            },
                            color = TechColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredPeers, key = { it.id }) { peer ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TechColors.DeepCharcoal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPeer(peer) }
                                    .testTag("peer_candidate_${peer.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val borderColor = hexToColor(peer.avatarColorHex)
                                    val avatarRes = getAvatarDrawableRes(peer.avatarId)
                                    Image(
                                        painter = painterResource(id = avatarRes),
                                        contentDescription = "Avatar de ${peer.name}",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, borderColor, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peer.name,
                                            color = TechColors.TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = peer.role,
                                            color = TechColors.NeonCyan,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.AddCircleOutline,
                                        contentDescription = "Agregar a ${peer.name}",
                                        tint = TechColors.NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar", color = TechColors.TextSecondary)
                }
            }
        }
    }
}
