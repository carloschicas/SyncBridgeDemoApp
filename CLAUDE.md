# CLAUDE.md — SyncBridgeDemoApp

Instrucciones para Claude Code al trabajar en este repositorio.

## Qué es este proyecto

Aplicaciones de demostración para **SyncBridge**, una librería que gestiona sincronización offline-first en apps móviles (Android/iOS). El repositorio contiene tres proyectos independientes: el backend del servidor, la app Android y la app iOS.

## Estructura del repositorio

```
SyncBridgeDemoApp/
├── docs/                               # Documentación de planificación e implementación
│   ├── DESIGN.md                       # Sistema de diseño visual (leer antes de tocar UI)
│   ├── planificacion_demo_backend.md
│   ├── planificacion_demo_android.md
│   ├── planificacion_demo_ios.md
│   └── implementacion_backend.md       # Checklist de progreso del backend
│
├── syncbridge-demo-backend/            # Servidor Node.js + Express + PostgreSQL
│   └── CLAUDE.md                       # Instrucciones específicas del backend
│
└── syncbridge-demo-android/            # App Android Kotlin + Jetpack Compose
    └── CLAUDE.md                       # Instrucciones específicas de Android
```

## Reglas globales

- Cada sub-proyecto tiene su propio `CLAUDE.md` con instrucciones específicas — leerlo antes de trabajar en ese directorio.
- La documentación en `docs/` es la fuente de verdad para la planificación. No tomar decisiones de arquitectura que contradigan esos documentos sin revisar primero.
- Los checklists de progreso (`implementacion_*.md`) deben mantenerse actualizados: marcar `[ ]` → `[x]` conforme se completen tareas.
