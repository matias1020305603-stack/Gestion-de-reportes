package com.example.ui.model

import androidx.annotation.DrawableRes

data class AvatarItem(
    val id: Int,
    val nombreRol: String,
    @DrawableRes val imagenRes: Int
)
