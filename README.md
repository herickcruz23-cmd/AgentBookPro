# AgentBookPro 🦷

> Asistente IA en Android (Kotlin + Jetpack Compose) que agenda citas dentales conversando, las guarda en Supabase y notifica al dueño por Telegram con el costo total.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material-3-blueviolet)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## ✨ Features

- 💬 **Chat IA conversacional** con Groq (Llama 3.3 70B) — gratis, sin tarjeta
- 🗓️ **Agenda citas automáticamente** detectando datos en lenguaje natural
- 💾 **Persistencia en Supabase** (PostgreSQL) con REST API
- 📲 **Notificación a Telegram** con todos los datos del paciente + costo total
- 💰 **Cálculo automático de costos** según servicio (limpieza, ortodoncia, etc.)
- 📊 **Estadísticas en vivo**: total de citas y citas del día
- 🎨 **UI premium**: tema oscuro con paleta neón verde/rosa
- 🛡️ **Manejo robusto de errores**: retry automático, fallback en Telegram, mensajes claros

## 🏗️ Stack

- **Kotlin 2.0** + **Jetpack Compose** (BOM 2024.09.02)
- **Material 3** + tema oscuro custom
- **Arquitectura MVVM + Repository Pattern**
- **Retrofit 2.11** + **OkHttp 4.12**
- **kotlinx.serialization**
- **Coroutines** + **Flow**
- **Groq API** (compatible OpenAI) / **Supabase REST** / **Telegram Bot API**

## 📸 Screenshots

_(Agrega aquí capturas de la app)_

---

## 🚀 Setup

### 1. Clonar el repo

```bash
git clone https://github.com/herickcruz23-cmd/AgentBookPro.git
cd AgentBookPro
```

### 2. Configurar las claves API

Tienes **dos opciones**:

#### Opción A — `keys.properties` (recomendado)

Crea un archivo `keys.properties` en la **raíz** del proyecto (al lado de `settings.gradle.kts`):

```properties
API_KEY=gsk_tu_clave_de_groq
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu_anon_key
TELEGRAM_BOT_TOKEN=tu_token_del_bot
TELEGRAM_CHAT_ID=tu_chat_id
```

#### Opción B — Hardcodear en `AppConfig.kt`

Edita `app/src/main/java/com/agentbook/pro/AppConfig.kt` y rellena las constantes `HARDCODED_*`.

### 3. Conseguir las claves

- **Groq** (IA, gratis): https://console.groq.com/keys
- **Supabase** (BD): https://supabase.com → nuevo proyecto → Settings → API
  - Ejecuta el SQL de `supabase.sql` en el SQL Editor
- **Telegram Bot**:
  - Abre @BotFather en Telegram → `/newbot` → te da el TOKEN
  - Manda `/start` a tu bot
  - Para el CHAT_ID: abre `https://api.telegram.org/bot<TOKEN>/getUpdates` y copia el `chat.id`
  - O usa @userinfobot para obtener tu ID directo

### 4. Compilar y ejecutar

Abre el proyecto en **Android Studio Hedgehog** o superior.

```
Build → Clean Project
Build → Rebuild Project
Run ▶
```

---

## 🗄️ Setup de Supabase

Ejecuta este SQL en el **SQL Editor** de tu proyecto Supabase:

```sql
create table if not exists public.appointments (
    id bigserial primary key,
    nombre text not null,
    telefono text not null,
    fecha date not null,
    hora time not null,
    servicio text not null,
    costo numeric(10,2),
    created_at timestamptz not null default now()
);

alter table public.appointments enable row level security;

create policy "anon_insert" on public.appointments
    for insert to anon with check (true);

create policy "anon_select" on public.appointments
    for select to anon using (true);
```

El SQL completo (idempotente, con migración) está en `supabase.sql`.

---

## 📁 Estructura del proyecto

```
app/src/main/java/com/agentbook/pro/
├── MainActivity.kt              Entry point
├── AppConfig.kt                 ⭐ Configuración central
├── AgentBookApp.kt              Application class
├── data/
│   ├── api/                     Clientes Retrofit (OpenAI, Supabase, Telegram)
│   ├── model/                   DTOs serializables + ServicePricing
│   └── repository/Repository.kt ⭐ Lógica de datos completa
└── ui/
    ├── viewmodel/ChatViewModel.kt
    ├── screens/MainScreen.kt
    ├── components/              ChatBubble, ChatInputBar, StatCard
    └── theme/                   Color, Theme, Type
```

## 🔄 Flujo

```
Usuario escribe → ChatViewModel → Repository.chat() → Groq API
                                                          ↓
                                              Modelo emite <BOOK>...</BOOK>
                                                          ↓
                                            extractBookingBlock()
                                                          ↓
                                         ┌─────────────────┴─────────────────┐
                                         ↓                                   ↓
                            Repository.createAppointment()    Repository.sendTelegramNotification()
                                  Supabase REST                       Telegram Bot API
```

## 🎨 Diseño

- Paleta **neón** sobre fondo oscuro (`#05060A`)
- Verde neón `#00FFA3` y rosa neón `#FF3DAA` como acentos
- Burbujas de chat con gradiente
- Animaciones de scroll automático

## 🛡️ Manejo de errores

| Componente | Estrategia |
|------------|-----------|
| Groq | Mensaje claro según código HTTP (401/429) |
| Supabase | 3 reintentos × 2s (free tier despierta lento) |
| Telegram | Fallback HTML → texto plano si parseo falla |
| App | `CoroutineExceptionHandler` global, errores en chat |

## 📄 Manual técnico

Documentación técnica completa con todos los archivos y diagramas en `MANUAL_TECNICO_AgentBook.md` (33 páginas).

## 📜 License

MIT
