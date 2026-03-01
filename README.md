# 🔍 Leads Scout

> Plataforma fullstack para la gestión y seguimiento de leads/clientes potenciales, compuesta por una API REST en Java (Spring Boot) y un frontend moderno en TypeScript.

---

## 📋 Tabla de contenidos

- [Descripción](#descripción)
- [Arquitectura](#arquitectura)
- [Stack tecnológico](#stack-tecnológico)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos previos](#requisitos-previos)
- [Instalación y puesta en marcha](#instalación-y-puesta-en-marcha)
- [Uso](#uso)
- [API Reference](#api-reference)
- [Variables de entorno](#variables-de-entorno)
- [Contribución](#contribución)
- [Licencia](#licencia)

---

## 📖 Descripción

**Leads Scout** es una aplicación web diseñada para que equipos de ventas y marketing puedan capturar, clasificar y hacer seguimiento de leads (clientes potenciales). Permite centralizar la información de contacto, asignar estados al pipeline de ventas y consultar el histórico de interacciones.

---

## 🏗️ Arquitectura

```
Leads-scout/
├── clientes-scout/          # Backend — API REST (Java / Spring Boot)
└── clientes-scout-front/    # Frontend — SPA (TypeScript / Angular o React)
```

La arquitectura sigue el patrón **cliente–servidor desacoplado**: el frontend consume la API REST del backend a través de HTTP/JSON. Ambas capas pueden desplegarse y escalar de forma independiente.

```
┌─────────────────────┐       HTTP/REST       ┌──────────────────────────┐
│  clientes-scout-    │ ◄──────────────────► │    clientes-scout         │
│  front (TypeScript) │       JSON            │    (Spring Boot / Java)   │
└─────────────────────┘                       └──────────┬───────────────┘
                                                          │
                                                    ┌─────▼──────┐
                                                    │  Base de   │
                                                    │   datos    │
                                                    └────────────┘
```

---

## 🛠️ Stack tecnológico

| Capa       | Tecnología                         |
|------------|------------------------------------|
| Backend    | Java 17+, Spring Boot, Maven       |
| Frontend   | TypeScript, HTML5, CSS3            |
| Base datos | (PostgreSQL / MySQL / H2 — según config) |
| Build      | Maven (backend), npm (frontend)    |

---

## 📁 Estructura del proyecto

```
Leads-scout/
├── .gitignore
├── clientes-scout/                  # Backend
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/example/clientesscout/
│       │   │       ├── controller/
│       │   │       ├── service/
│       │   │       ├── repository/
│       │   │       └── model/
│       │   └── resources/
│       │       └── application.properties
│       └── test/
└── clientes-scout-front/            # Frontend
    ├── package.json
    ├── tsconfig.json
    └── src/
```

---

## ✅ Requisitos previos

- **Java 17** o superior
- **Maven 3.8+**
- **Node.js 18+** y **npm 9+**
- Base de datos compatible (PostgreSQL recomendado)

---

## 🚀 Instalación y puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/macros05/Leads-scout.git
cd Leads-scout
```

### 2. Configurar la base de datos

Copia y edita el archivo de configuración del backend:

```bash
cp clientes-scout/src/main/resources/application.properties \
   clientes-scout/src/main/resources/application-local.properties
```

Edita `application-local.properties` con tus credenciales (ver sección [Variables de entorno](#variables-de-entorno)).

### 3. Levantar el backend

```bash
cd clientes-scout
mvn spring-boot:run
```

La API quedará disponible en `http://localhost:8080`.

### 4. Levantar el frontend

```bash
cd clientes-scout-front
npm install
npm start
```

La aplicación web estará disponible en `http://localhost:4200` (o el puerto configurado).

---

## 💻 Uso

1. Accede al frontend en tu navegador.
2. Registra e inicia sesión (si la autenticación está habilitada).
3. Crea, edita y gestiona leads desde el panel principal.
4. Filtra y busca leads por estado, fecha o responsable.

---

## 📡 API Reference

| Método | Endpoint            | Descripción                   |
|--------|---------------------|-------------------------------|
| GET    | `/api/clientes`     | Listar todos los leads        |
| GET    | `/api/clientes/{id}`| Obtener un lead por ID        |
| POST   | `/api/clientes`     | Crear un nuevo lead           |
| PUT    | `/api/clientes/{id}`| Actualizar un lead existente  |
| DELETE | `/api/clientes/{id}`| Eliminar un lead              |

> ⚠️ Los endpoints exactos pueden variar. Consulta la documentación Swagger/OpenAPI en `http://localhost:8080/swagger-ui.html` si está habilitada.

---

## 🔐 Variables de entorno

| Variable                   | Descripción                         | Ejemplo                          |
|----------------------------|-------------------------------------|----------------------------------|
| `SPRING_DATASOURCE_URL`    | URL de conexión a la base de datos  | `jdbc:postgresql://localhost/leads` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos       | `postgres`                       |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos    | `secreto`                        |
| `SERVER_PORT`              | Puerto del servidor backend         | `8080`                           |

> 🚨 **Nunca** incluyas credenciales reales en el código fuente ni en el repositorio.

---

## 🤝 Contribución

1. Haz un fork del repositorio.
2. Crea una rama para tu feature: `git checkout -b feature/nueva-funcionalidad`
3. Haz commit de tus cambios: `git commit -m 'feat: añadir nueva funcionalidad'`
4. Sube la rama: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request.

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Consulta el archivo `LICENSE` para más detalles.

---

*Desarrollado por [macros05](https://github.com/macros05)*
