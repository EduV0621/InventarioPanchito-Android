# Inventario Panchito — Prototipo inicial (Sprint 1)

Este es el prototipo técnico pedido en el **primer avance**: proyecto de Android
Studio configurado con Kotlin + Jetpack Compose, arquitectura **MVVM en capas**
(UI → ViewModel → Repository → Data Source) con **Room** y **DataStore** ya
definidos (sin lógica de negocio todavía), y **wireframes navegables** de las
4 pantallas principales: **Login → Dashboard → Catálogo → Movimientos**.

No usa Figma: los wireframes están hechos directamente en Jetpack Compose,
como componentes reales del proyecto. Esto es mejor para tu entrega porque:
- Es la misma evidencia técnica que pide el informe ("el esqueleto del
  proyecto compila y navega entre pantallas").
- Lo que hagas ahora en Compose lo sigues usando y ampliando en los
  Sprints 2, 3 y 4 (no hay que "traducir" nada de Figma a código).

## Estado actual: Fase 1 (MVVM + persistencia local con Room)

Catalogo de productos con **CRUD local** funcionando de punta a punta:

```
UI (Compose) -> ViewModel -> Repository -> LocalDataSource -> Room (DAO)
```

- **Registrar** producto: Catalogo -> boton "+" (formulario con validaciones).
- **Ver** productos: Catalogo (lista) y Detalle (tocar un producto).
- **Editar**: Detalle -> icono lapiz.
- **Eliminar**: Detalle -> icono papelera (con confirmacion). Si el producto ya tiene
  movimientos asociados se **desactiva** en lugar de borrarse (regla de negocio).
- Los datos persisten en `inventario_panchito.db`. Al crear la base por primera vez se
  insertan las categorias iniciales (Abarrotes, Lacteos, Panaderia, Limpieza, Bebidas, Otros).
- Las dependencias se arman a mano en `di/AppContainer.kt` (sin Hilt/Koin).

Siguen con datos de ejemplo (fuera de esta fase): Login, Registro de empleado y Movimientos.
Firebase, Retrofit y Google Maps siguen sin implementarse (solo las dependencias ya declaradas).

## 1. Cómo abrirlo en Android Studio (Panda | 2025.3.2 RC1)

1. Descomprime el archivo `InventarioPanchito.zip`.
2. Abre Android Studio → **File → Open...** → selecciona la carpeta
   `InventarioPanchito` (la que contiene `settings.gradle.kts`).
3. Como el proyecto no trae el binario del Gradle Wrapper (`gradlew`,
   `gradle-wrapper.jar`), Android Studio te va a mostrar un aviso del estilo
   *"Gradle wrapper is missing"* o similar. Dale a **"Create Gradle Wrapper"**
   (o **Try Again** tras sincronizar) — Android Studio lo genera solo.
   Si en tu versión no aparece esa opción, abre una terminal dentro de la
   carpeta del proyecto y ejecuta `gradle wrapper` (requiere tener Gradle
   instalado una sola vez) y vuelve a sincronizar.
4. Espera el **Gradle Sync**. La primera vez puede tardar porque descarga
   dependencias (Compose, Room, Retrofit, Firebase BoM, Maps).
5. Corre la app (▶) en un emulador o dispositivo con **Android 7.0 (API 24)**
   o superior (ese es el `minSdk` definido, según la restricción de
   dispositivos disponibles del equipo).

## 2. Qué vas a ver al ejecutar

- **Login**: pantalla con correo/contraseña. Demo: `admin@panchito.pe` +
  cualquier contraseña → navega al Dashboard.
- **Dashboard**: resumen de productos, stock bajo y agotados (datos de
  ejemplo), con accesos a Catálogo y Movimientos.
- **Catálogo**: lista de productos con indicador visual de stock
  (verde/ámbar/rojo).
- **Movimientos**: lista de entradas/salidas con su estado de sincronización
  (ícono de nube).

Todo navega con **Navigation Compose** (`NavGraph.kt`), que es justamente la
evidencia que pide el informe para el Sprint 1.

## 3. Estructura del proyecto (arquitectura MVVM)

```
app/src/main/java/com/panchito/inventario/
├── navigation/          → Screen.kt, NavGraph.kt (las 4 rutas del wireframe)
├── ui/theme/            → Color, Theme, Type (Material 3)
├── ui/components/       → UiState.kt (Loading/Success/Error/Empty/Offline/Syncing)
├── ui/screens/          → login/, dashboard/, catalogo/, movimientos/
│                          (cada uno con su Screen.kt + ViewModel.kt)
├── domain/model/        → Producto, Empleado, Movimiento, Rol (modelos puros)
├── data/local/entity/   → Entidades Room (tablas, sin lógica de negocio aún)
├── data/local/dao/      → DAOs de Room (consultas base, con TODOs marcados)
├── data/local/          → AppDatabase.kt
├── data/local/datastore/→ UserPreferencesManager.kt (sesión/rol)
├── data/local/datasource/→ LocalDataSource (unica capa que usa los DAO)
├── data/mapper/         → Entity <-> modelo de dominio
├── data/repository/     → Interfaces + implementaciones (Producto/Categoria con Room; Empleado/Movimiento aun "Fake")
└── di/                  → AppContainer (inyeccion manual de dependencias)
```

`FakeEmpleadoRepository` y `FakeMovimientoRepository` son lo que falta por
**reemplazar** por implementaciones reales (Producto y Categoria ya usan Room). La UI y los ViewModel ya están escritos para trabajar contra la
**interfaz** del repositorio, no contra la implementación — así el cambio no
rompe las pantallas.

## 4. Qué queda para los siguientes sprints (ya marcado con `// TODO`)

- **Sprint 2**: lógica real en los DAO/Repository (Room), autenticación local
  (PB01), registro de empleados (PB03) y productos (PB06).
- **Sprint 3**: registro de entradas/salidas con validación de stock
  (PB09/PB10), alertas reales (PB13), consumo de la API REST con Retrofit
  (PB14) y sincronización offline→online (PB15).
- **Sprint 4**: migración a Firebase Authentication/Firestore/Storage (PB16)
  y el mapa con Google Maps API (PB17).

## 5. Evidencia para tu informe

Para la sección "Evidencia a incluir" (prototipo inicial) puedes capturar:
- Pantalla de Android Studio con el árbol de carpetas (`data`, `domain`,
  `ui`, `navigation`) abierto.
- Capturas de la app navegando Login → Dashboard → Catálogo → Movimientos.
- El `build.gradle.kts` (app) mostrando las dependencias del stack
  (Kotlin, Compose, Room, DataStore, Retrofit, Firebase, Maps) ya declaradas.

## 6. Fase 4: Firebase Authentication (Email/Password)

Se agrego autenticacion de usuarios con Firebase Authentication, integrada a la arquitectura
existente sin tocar Room, Retrofit ni las pantallas de Catalogo/Movimientos:

```
UI (LoginScreen) -> AuthViewModel -> AuthRepository -> AuthDataSource -> Firebase
```

- **Login** (`ui/screens/login/`): correo + contrasena contra Firebase Authentication.
- **Registro publico: eliminado en la Fase 5.** El Login ya no ofrece "Crear cuenta". Las cuentas
  de empleados las registra el Administrador desde el Panel (`RegistroEmpleadoScreen`, aun sin
  funcionalidad completa: roles y activar/desactivar empleados son de una fase posterior).
- **Sesion**: `NavGraph` decide la pantalla inicial (Login o Dashboard) leyendo
  `AuthRepository.usuarioActual()`, que a su vez lee `FirebaseAuth.currentUser` de forma local
  (sin red). Por eso la sesion persiste offline tras cerrar y volver a abrir la app.
- **Logout**: boton en el Dashboard -> `FirebaseAuth.signOut()` (`AuthRepository.cerrarSesion()`).
- Ninguna contrasena se guarda en Room, DataStore ni en archivos: Firebase Authentication es la
  unica fuente de verdad de la autenticacion.
- Pendiente de configuracion manual (no incluido en el codigo, ver instrucciones del chat):
  crear/conectar el proyecto en Firebase Console, agregar `app/google-services.json` y activar
  el metodo de acceso Email/Password.
- Fuera de alcance de esta fase (a proposito): Firestore, Storage, Google Maps, Google Sign-In,
  roles complejos y sincronizacion Room <-> Firestore.

## 7. Fase 5: Cloud Firestore integrado al inventario real

Tres fuentes de datos, cada una con un rol distinto (todas detras de `ProductoRepository`):

```
UI (Compose) -> ViewModel -> ProductoRepository
                                 |-- Room (ProductoLocalDataSource)      inventario REAL, base local (la UI lee de aqui)
                                 |-- Firestore (ProductoFirestoreDataSource)  copia en la nube del inventario real
                                 '-- Retrofit (ProductoRemoteDataSource) demo tecnica de API REST (DummyJSON)
```

- **Inventario real** = pantalla "Catalogo de productos". Registrar / editar / eliminar escriben
  primero en **Room** y despues en **Firestore**, en `users/{uid}/products/{productId}` (`uid` =
  `FirebaseAuth.currentUser.uid`; el `productId` es el mismo id que Room). No existe la coleccion
  global `products`.
- Si la nube falla (sin Internet, error del servicio), el cambio **queda guardado en Room** y la app
  avisa al usuario ("no se guardo en la nube"). No hay cola de pendientes ni reintentos todavia.
- Sin usuario autenticado no se puede registrar/editar/eliminar: "Debes iniciar sesion para
  gestionar el inventario."
- **Catalogo API (demo Retrofit)** sigue siendo independiente: sus productos (DummyJSON) no aparecen
  en el catalogo real ni se guardan en Room ni en Firestore.
- Reglas de seguridad: `firestore.rules` (cada usuario solo lee/escribe `users/{su-uid}/...`).
  Hay que publicarlas en Firebase Console (ver abajo).
- Se elimino la pantalla separada "Inventario en la nube (Firestore)" para no tener dos inventarios.

**NO incluido a proposito** (fases posteriores): sincronizacion Room <-> Firestore (lectura de la
nube hacia Room, cola offline, WorkManager, conflictos), Firebase Storage, Google Maps, roles.

### Configuracion manual en Firebase Console
1. **Authentication -> Sign-in method**: Email/Password habilitado.
2. **Authentication -> Users -> Add user**: crear a mano la cuenta del administrador (ya no hay
   registro desde la app).
3. **Firestore Database -> Create database** (modo produccion).
4. **Firestore Database -> Rules**: pegar el contenido de `firestore.rules` y **Publicar**.
5. Verificar en Firestore Database -> Data la ruta `users/{uid}/products/{id}`.
