# Sistema para Estimación de Esfuerzos de Proyectos de Software

Aplicación web desarrollada como Trabajo de Fin de Grado del Grado en Ingeniería Informática del Software de la Universidad de Oviedo.

El sistema permite gestionar proyectos de estimación software, calcular su tamaño mediante distintas técnicas y convertir ese tamaño a esfuerzo y coste estimado.

## Ejecución rápida con Docker

La forma recomendada de ejecutar la aplicación es mediante Docker Compose.

Docker permite levantar la aplicación y la base de datos PostgreSQL sin instalar Java, Maven ni PostgreSQL manualmente en el equipo.

### Requisitos

Tener instalado:

- Docker Desktop
- Docker Compose

Comprobar instalación:

```bash
docker --version
docker compose version
```

### Pasos de ejecución

Desde la raíz del proyecto, crear el fichero `.env` a partir del ejemplo:

```bash
cp .env.example .env
```

En Windows PowerShell:

```powershell
copy .env.example .env
```

Editar el fichero `.env` si se desea cambiar la contraseña de la base de datos o el puerto de la aplicación:

```env
POSTGRES_DB=tfg_estimacion_esfuerzo
POSTGRES_USER=tfg_user
POSTGRES_PASSWORD=cambia_esta_password
APP_PORT=8080
```

Levantar la aplicación:

```bash
docker compose up --build
```

Cuando el arranque finalice, la aplicación estará disponible en:

```text
http://localhost:8080
```

Para detener la aplicación:

```bash
docker compose down
```

Para detener la aplicación y eliminar también la base de datos Docker:

```bash
docker compose down -v
```

> `docker compose down` conserva los datos de la base de datos.  
> `docker compose down -v` elimina la base de datos y todos sus datos.

## Datos iniciales

Al arrancar la aplicación se crea automáticamente un usuario administrador inicial:

```text
Usuario: admin
Contraseña: admin123
```

También se registra una función de transformación predefinida para Puntos Función basada en el modelo de Matson, Barrett y Mellichamp (1994):

```text
E = 585.7 + 15.12 × FP
```

## Funcionalidades principales

- Gestión de usuarios, roles y permisos.
- Gestión de proyectos de estimación.
- Asignación de trabajadores a proyectos.
- Estimación de tamaño mediante Puntos Función.
- Estimación de tamaño mediante Use Case Points.
- Conversión de tamaño a esfuerzo mediante Delphi.
- Conversión de tamaño a esfuerzo mediante funciones de transformación.
- Cálculo opcional de coste estimado.
- Importación y exportación mediante XML.
- Generación de informes.
- Internacionalización en español e inglés.

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Spring MVC
- Spring Security
- Thymeleaf
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Bootstrap
- Docker
- Docker Compose
- Selenium WebDriver

## Ejecución sin Docker

También puede ejecutarse directamente con Maven, siempre que exista una base de datos PostgreSQL disponible y estén configuradas las variables de conexión.

Variables principales:

```text
DB_URL
DB_USER
DB_PASSWORD
```

Ejecutar en Linux/macOS:

```bash
./mvnw spring-boot:run
```

Ejecutar en Windows:

```powershell
mvnw.cmd spring-boot:run
```

## Pruebas

Para ejecutar las pruebas:

```bash
./mvnw test
```

En Windows:

```powershell
mvnw.cmd test
```

## Estructura principal del proyecto

```text
src/
├── main/
│   ├── java/com/uniovi/estimacion/
│   │   ├── config/
│   │   ├── controllers/
│   │   ├── entities/
│   │   ├── repositories/
│   │   ├── services/
│   │   ├── validators/
│   │   └── web/
│   └── resources/
│       ├── static/
│       ├── templates/
│       └── application.properties
└── test/
```

## Autor

Ricardo Díaz Núñez

Trabajo de Fin de Grado  
Grado en Ingeniería Informática del Software  
Universidad de Oviedo  
Curso 2025/2026