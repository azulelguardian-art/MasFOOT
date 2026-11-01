# Estado real del proyecto (2026-11-01)

## Cómo conseguir el APK
Este entorno no puede compilar Android (sin SDK y con los repositorios de Gradle bloqueados), así que el APK
se compila en la nube con GitHub Actions:

1. Crea un repositorio en GitHub y sube **todo el contenido de esta carpeta** (incluida `.github/`).
2. Abre la pestaña **Actions** → «Compilar APK de MasFOOT» (se lanza sola al subir; también con *Run workflow*).
3. Al terminar, en el resumen de la ejecución descarga el artefacto **MasFOOT-apk** (contiene `app-debug.apk`).
4. Cópialo al móvil y ábrelo (permite «instalar apps de origen desconocido»). Es un APK *debug*, válido para uso propio.

**Aviso importante:** el código Android NO ha pasado por ningún compilador. Es posible que la primera ejecución
falle por algún error de compilación. Copia el log rojo de Actions y se corrige.

Alternativa: abrir la carpeta `android/` con Android Studio y usar *Build > Build APK(s)*.

## Qué hace la app (escrito, sin compilar ni probar en dispositivo)
- Inicio con partidos del día (escudos, hora, competición, país, estado, marcador), orden cronológico,
  secciones En directo / Próximos / Finalizados, calendario (selector de fecha), buscador, filtros por competición.
- Deslizar para actualizar + actualización automática cada minuto (hoy). Caché local y modo sin conexión
  (muestra los últimos datos guardados avisando de ello).
- Filtros de emisión: todas / SOLO EMISIONES GRATUITAS / gratis con registro / suscripción; chips de idioma
  (solo los realmente disponibles); etiqueta de acceso en cada partido. Sin oferta: «NO HAY RETRANSMISIÓN LEGAL DISPONIBLE.».
- Ficha de partido: equipos, escudos, marcador, hora local, estadio, favoritos (partido/equipos/competición),
  botón SELECCIONAR IDIOMA, ofertas de emisión, VER DIRECTO (reproductor Media3 HLS/DASH, subtítulos, pantalla
  completa, reintentos de red) o ABRIR PLATAFORMA OFICIAL, sección CUOTAS SPORTIUM con APOSTAR EN SPORTIUM.
- Pestañas INICIO · DIRECTO (con contador) · BUSCAR · FAVORITOS · AJUSTES. Modo oscuro opcional.
- Ajustes: URL+token del backend, o clave propia de football-data.org (guardada solo en el dispositivo).
  Botón PROBAR REPRODUCTOR con un stream público de prueba.

## Dos modos de datos
- **Modo directo** (solo clave gratuita de football-data.org en Ajustes): partidos reales del día según tu plan,
  pero **ninguna retransmisión** (no hay catálogo).
- **Modo servidor** (backend desplegado + `BROADCAST_CATALOG_URL`): añade retransmisiones, pero solo las que
  entregue un catálogo oficial autorizado. Para `EMBEDDED_*`, `official_url` debe ser la URL del manifiesto que el
  proveedor autoriza a reproducir en apps.

## Probado: NADA de forma real
- Backend: 8 pruebas pytest escritas, no ejecutadas (no hay acceso a PyPI aquí).
- Android: XML y workflow validados como bien formados; Kotlin sin compilar.
- Sin ejecutar: Docker Compose, esquema SQL, llamadas reales a football-data.org, reproducción de vídeo.

## Diferencias con el plan original (decisiones para reducir riesgo de compilación)
- Un solo módulo Gradle `app` organizado por paquetes (`data`, `ui`), no 16 módulos.
- Sin Hilt/Room/Retrofit: inyección manual, DataStore para caché y favoritos, OkHttp + kotlinx.serialization.
- No hay notificaciones ni WorkManager todavía (no se añaden botones decorativos); la actualización es en primer plano.
- Eventos, estadísticas y alineaciones: la ficha indica que no están disponibles con la fuente actual.
- Cuotas Sportium: sin integración autorizada solo se muestra el aviso y el enlace oficial. No se inventan cuotas.
- Backend sin persistencia en PostgreSQL todavía (caché en memoria); adaptador API-Football y Sportmonks pendientes.

## Pendiente de autorización externa
- Catálogo de emisiones autorizado: sin él, todos los partidos salen sin retransmisión legal. No existe una API
  pública que diga dónde ver gratis cada partido en cada país.
- Cuotas Sportium: requiere acuerdo/API autorizada.
- Cobertura de ligas: la de tu plan de football-data.org (el gratuito no cubre todo el mundo).
- LICENSE: sustituir por el texto íntegro de Apache 2.0 antes de publicar.
