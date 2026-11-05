package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TechColors

@Composable
fun TechTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    placeholder: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = if (isError) Color(0xFFFF3B30) else TechColors.TextSecondary) },
        placeholder = if (placeholder != null) { { Text(placeholder, color = TechColors.TextSecondary.copy(alpha = 0.5f)) } } else null,
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (isError) Color(0xFFFF3B30) else TechColors.NeonCyan) },
        trailingIcon = trailingIcon,
        isError = isError,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = TechColors.TechCard,
            focusedContainerColor = TechColors.TechCard,
            disabledContainerColor = TechColors.TechCard.copy(alpha = 0.6f),
            unfocusedBorderColor = if (isError) Color(0xFFFF3B30) else Color.Transparent,
            focusedBorderColor = if (isError) Color(0xFFFF3B30) else TechColors.NeonCyan,
            cursorColor = TechColors.NeonCyan,
            focusedTextColor = TechColors.TextPrimary,
            unfocusedTextColor = TechColors.TextPrimary,
            disabledTextColor = TechColors.TextSecondary
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun GlowingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TechColors.NeonCyan
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
    }
}
