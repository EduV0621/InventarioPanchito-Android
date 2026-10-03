package com.panchito.inventario.domain.model

/**
 * Usuario autenticado mediante Firebase Authentication (Fase 4).
 * Modelo de dominio puro: ni el ViewModel ni la UI conocen FirebaseUser directamente.
 */
data class Usuario(
    val uid: String,
    val correo: String?
)
