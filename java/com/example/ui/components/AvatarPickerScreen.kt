package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.model.AvatarItem

// Paleta de colores para personalizar el círculo (Tecnológicos y Mantenimiento)
val ListaColoresBorde = listOf(
    Color(0xFF00FFFF), // Cian Neón
    Color(0xFFFF00FF), // Magenta Neón
    Color(0xFF0056D2), // Azul CSR
    Color(0xFF34C759), // Verde Esmeralda (Solucionado)
    Color(0xFFFFCC00), // Amarillo Ámbar (En Proceso)
    Color(0xFFFF3B30), // Rojo Neón (Crítico)
    Color(0xFF9D00FF), // Púrpura Tech
    Color(0xFFFFFFFF)  // Blanco Minimalista
)

// Helper para convertir Color a Hex String para almacenar
fun colorToHex(color: Color): String {
    val a = (color.alpha * 255).toInt()
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}

fun hexToColor(hex: String, defaultColor: Color = Color(0xFF00FFFF)): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0x00000000FF000000L)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        defaultColor
    }
}

fun getAvatarDrawableRes(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_1
        2 -> R.drawable.avatar_2
        3 -> R.drawable.avatar_3
        4 -> R.drawable.avatar_4
        5 -> R.drawable.avatar_5
        6 -> R.drawable.avatar_6
        7 -> R.drawable.avatar_7
        8 -> R.drawable.avatar_8
        9 -> R.drawable.avatar_9
        10 -> R.drawable.avatar_10
        11 -> R.drawable.avatar_11
        12 -> R.drawable.avatar_12
        13 -> R.drawable.avatar_13
        14 -> R.drawable.avatar_14
        15 -> R.drawable.avatar_15
        16 -> R.drawable.avatar_16
        17 -> R.drawable.avatar_17
        18 -> R.drawable.avatar_18
        19 -> R.drawable.avatar_19
        20 -> R.drawable.avatar_20
        else -> R.drawable.avatar_1
    }
}

@Composable
fun AvatarPickerScreen(
    initialAvatarId: Int = 1,
    initialColorHex: String = "#00FFFF",
    onDismiss: () -> Unit = {},
    onAvatarGuardado: (avatarId: Int, colorHex: String) -> Unit
) {
    // 1. LISTA DE LOS 20 AVATARES DE TECNOLOGÍA Y SERVICIOS GENERALES
    val listaAvatares = remember {
        listOf(
            AvatarItem(1, "Especialista IT", R.drawable.avatar_1),
            AvatarItem(2, "Mantenimiento Redes", R.drawable.avatar_2),
            AvatarItem(3, "Analista de Datos", R.drawable.avatar_3),
            AvatarItem(4, "Técnico Eléctrico", R.drawable.avatar_4),
            AvatarItem(5, "Soporte Técnico", R.drawable.avatar_5),
            AvatarItem(6, "Operario Fontanería", R.drawable.avatar_6),
            AvatarItem(7, "Técnico Climatización", R.drawable.avatar_7),
            AvatarItem(8, "Supervisora Servicios", R.drawable.avatar_8),
            AvatarItem(9, "Mantenimiento General", R.drawable.avatar_9),
            AvatarItem(10, "Ingeniera de Sistemas", R.drawable.avatar_10),
            AvatarItem(11, "Administrador Servidores", R.drawable.avatar_11),
            AvatarItem(12, "Seguridad Digital", R.drawable.avatar_12),
            AvatarItem(13, "Coordinador Infraestructura", R.drawable.avatar_13),
            AvatarItem(14, "Mantenimiento Pintura", R.drawable.avatar_14),
            AvatarItem(15, "Droide de Limpieza", R.drawable.avatar_15),
            AvatarItem(16, "Mantenimiento Estructuras", R.drawable.avatar_16),
            AvatarItem(17, "Técnico Electrónico", R.drawable.avatar_17),
            AvatarItem(18, "Operario de Seguridad", R.drawable.avatar_18),
            AvatarItem(19, "Impresión 3D / Taller", R.drawable.avatar_19),
            AvatarItem(20, "Técnico Control de Acceso", R.drawable.avatar_20)
        )
    }

    // Estados para la selección activa
    var avatarSeleccionado by remember {
        mutableStateOf(listaAvatares.find { it.id == initialAvatarId } ?: listaAvatares[0])
    }
    var colorBordeSeleccionado by remember {
        mutableStateOf(hexToColor(initialColorHex, ListaColoresBorde[0]))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Fondo oscuro 2026
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera con botón de retroceso
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PERSONALIZA TU AVATAR CSR",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. VISTA PREVIA DEL AVATAR SELECCIONADO CON CÍRCULO PERSONALIZABLE
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                // Borde circular personalizable (Código de cambio dinámico)
                .border(width = 4.dp, color = colorBordeSeleccionado, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = avatarSeleccionado.imagenRes),
                contentDescription = avatarSeleccionado.nombreRol,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = avatarSeleccionado.nombreRol,
            color = colorBordeSeleccionado,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SECTOR PARA CAMBIAR EL COLOR DEL CÍRCULO
        Text(
            text = "Color del círculo:",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ListaColoresBorde.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { colorBordeSeleccionado = color }
                        .border(
                            width = if (colorBordeSeleccionado == color) 3.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. GRILLA PEREZOSA (LAZYGRID) CON LOS 20 AVATARES
        Text(
            text = "Selecciona tu foto de perfil (20 disponibles):",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4), // 4 columnas de avatares
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listaAvatares) { avatar ->
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                        .clickable { avatarSeleccionado = avatar }
                        // Resalta con el color seleccionado si está activo
                        .border(
                            width = if (avatarSeleccionado.id == avatar.id) 3.dp else 1.dp,
                            color = if (avatarSeleccionado.id == avatar.id) colorBordeSeleccionado else Color.DarkGray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = avatar.imagenRes),
                        contentDescription = avatar.nombreRol,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BOTÓN GUARDAR SELECCIÓN
        Button(
            onClick = {
                onAvatarGuardado(avatarSeleccionado.id, colorToHex(colorBordeSeleccionado))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorBordeSeleccionado)
        ) {
            Text(
                text = "GUARDAR FOTO DE PERFIL",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
