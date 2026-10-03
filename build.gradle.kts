// Archivo de build a nivel de proyecto. No agregar dependencias aqui.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // Fase 4: procesa google-services.json (ver seccion "Configuracion pendiente" del informe).
    id("com.google.gms.google-services") version "4.4.2" apply false
}
