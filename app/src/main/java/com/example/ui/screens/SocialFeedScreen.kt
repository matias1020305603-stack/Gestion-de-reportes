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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SocialPost
import com.example.ui.components.getAvatarDrawableRes
import com.example.ui.components.hexToColor
import com.example.ui.theme.TechColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SocialFeedScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val socialPosts by viewModel.socialPosts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val socialImageModeration by viewModel.socialImageModeration.collectAsStateWithLifecycle()

    var postContent by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAddUrlField by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Active comment text per post ID
    var commentInputs by remember { mutableStateOf(mapOf<String, String>()) }

    val (launchCamera, launchGallery) = com.example.ui.components.rememberCameraAndGalleryLaunchers(
        onImageCapturedOrPicked = { uri ->
            errorMessage = ""
            selectedImageUri = uri
            imageUrlInput = ""
            showAddUrlField = false
            viewModel.validateSocialImage(uri)
        },
        onPermissionDenied = { msg ->
            errorMessage = msg
        }
    )

    if (showImageSourceDialog) {
        com.example.ui.components.ImageSourceChooserDialog(
            onDismiss = { showImageSourceDialog = false },
            onOpenCamera = launchCamera,
            onOpenGallery = launchGallery,
            onOpenPrivacyTerms = { showPrivacyDialog = true }
        )
    }

    if (showPrivacyDialog) {
        com.example.ui.components.PrivacyPolicyPdfDialog(onDismiss = { showPrivacyDialog = false })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Tarjeta superior para crear nueva publicación (Estilo Twitter / X)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val borderColor = currentUser?.avatarColorHex?.let { hexToColor(it) } ?: Color(0xFF00FFFF)
                        val avatarRes = getAvatarDrawableRes(currentUser?.avatarId ?: 1)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E1E))
                                .border(width = 2.dp, color = borderColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.fullName ?: "Usuario CSR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Muro Comunitario (Estudiantes, Profesores, Serv. Gen.)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = postContent,
                        onValueChange = {
                            postContent = it
                            errorMessage = ""
                        },
                        placeholder = { Text("Comenta sobre infraestructura, daños o notas del colegio...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = TechColors.NeonCyan
                        )
                    )

                    if (selectedImageUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (socialImageModeration !is com.example.util.ImageModerationResult.Rejected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                coil.compose.AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Vista previa de imagen seleccionada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        selectedImageUri = null
                                        viewModel.resetSocialImageModeration()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar imagen", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        com.example.ui.components.ImageModerationStatusBanner(state = socialImageModeration)
                    } else if (showAddUrlField) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = imageUrlInput,
                            onValueChange = { imageUrlInput = it },
                            placeholder = { Text("URL de imagen adjunta (ej. https://picsum.photos/400)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF3B30),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val canPublishWithImage = selectedImageUri == null ||
                        socialImageModeration is com.example.util.ImageModerationResult.Approved

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { showImageSourceDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("social_add_image_button")
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = "Cámara o Galería",
                                    tint = TechColors.NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Foto", fontSize = 12.sp)
                            }
                            IconButton(onClick = { showAddUrlField = !showAddUrlField }) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = "Pegar enlace de imagen",
                                    tint = if (showAddUrlField) TechColors.NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            enabled = canPublishWithImage,
                            onClick = {
                                val approvedLocalUri = if (socialImageModeration is com.example.util.ImageModerationResult.Approved) {
                                    selectedImageUri?.toString()
                                } else null
                                val result = viewModel.createSocialPost(
                                    content = postContent,
                                    imageUrl = imageUrlInput.ifBlank { null },
                                    localImageUriString = approvedLocalUri
                                )
                                if (result.first) {
                                    postContent = ""
                                    imageUrlInput = ""
                                    selectedImageUri = null
                                    showAddUrlField = false
                                    errorMessage = ""
                                } else {
                                    errorMessage = result.second ?: "Error al publicar"
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonCyan)
                        ) {
                            Text("PUBLICAR", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Muro Social Vertical (Lista de publicaciones tipo Twitter)
        items(socialPosts) { post ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Encabezado de la publicación
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val borderColor = hexToColor(post.avatarColorHex)
                        val avatarRes = getAvatarDrawableRes(post.avatarId)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E1E))
                                .border(width = 2.dp, color = borderColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TechColors.NeonCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = post.authorRole,
                                        color = TechColors.NeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(text = post.timestamp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contenido del post
                    Text(
                        text = post.content,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Imagen opcional adjunta
                    if (!post.imageUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = post.imageUrl,
                                contentDescription = "Evidencia",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Botones de interacción (Me gusta, Comentarios)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.toggleLikePost(post.id) }
                        ) {
                            Icon(
                                imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Me gusta",
                                tint = if (post.isLikedByMe) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${post.likesCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "Comentarios",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${post.comments.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Lista de comentarios
                    if (post.comments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            post.comments.forEach { comment ->
                                Row(verticalAlignment = Alignment.Top) {
                                    val commentBorder = hexToColor(comment.avatarColorHex)
                                    val commentAvatarRes = getAvatarDrawableRes(comment.avatarId)

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E1E1E))
                                            .border(width = 1.dp, color = commentBorder, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = commentAvatarRes),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = comment.authorName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "(${comment.authorRole})", color = TechColors.NeonCyan, fontSize = 10.sp)
                                        }
                                        Text(text = comment.text, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Campo para agregar un comentario moderado
                    Spacer(modifier = Modifier.height(10.dp))
                    var currentCommentInput by remember(post.id) { mutableStateOf(commentInputs[post.id] ?: "") }
                    var commentError by remember(post.id) { mutableStateOf("") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentCommentInput,
                            onValueChange = {
                                currentCommentInput = it
                                commentInputs = commentInputs + (post.id to it)
                                commentError = ""
                            },
                            placeholder = { Text("Escribe un comentario respetuoso...") },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val result = viewModel.addCommentToPost(post.id, currentCommentInput)
                                if (result.first) {
                                    commentInputs = commentInputs + (post.id to "")
                                    currentCommentInput = ""
                                    commentError = ""
                                } else {
                                    commentError = result.second ?: "Error"
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = TechColors.NeonCyan)
                        }
                    }

                    if (commentError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = commentError, color = Color(0xFFFF3B30), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
