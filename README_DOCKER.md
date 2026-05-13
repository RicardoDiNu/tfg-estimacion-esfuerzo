# Ejecución con Docker

## 1. Preparar variables

Copia el fichero de ejemplo:

```bash
cp .env.example .env
```

Edita `.env` y cambia al menos `POSTGRES_PASSWORD`.

## 2. Levantar la aplicación

```bash
docker compose up --build
```

La aplicación quedará disponible en:

```text
http://localhost:8080
```

## 3. Parar la aplicación

```bash
docker compose down
```

## 4. Borrar también la base de datos

Solo si quieres resetear completamente los datos:

```bash
docker compose down -v
```

## 5. Usuarios iniciales

Con el código actual, al arrancar se crea automáticamente:

```text
usuario: admin
contraseña: admin123
```
