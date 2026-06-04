package com.hp.hp_omnipad.data.remote.model

/*
    Added default values because if not added firestore parsing might fail
 */
data class CategoryDto(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val icon: String = "",
    val order: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)