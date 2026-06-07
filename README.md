Запуск приложения

1. Склонируйте репозиторий и перейдите в папку проекта
```bash
git clone https://github.com/Java-Masterskaya/Room_Booking_System.git
cd Room_Booking_System
```
Настройте переменные окружения в файле .env по примеру .env.example
```bash
cp .env.example .env.example
```

Откройте `.env` в редакторе и замените плейсхолдеры на реальные значения:

```bash
POSTGRES_DB=bookingdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=ваш-безопасный-пароль
KAFKA_CLUSTER_ID=уникальный-id-кластера
```
2. Вариант "Local" (Разработка в IDE/ CLI)
Используется, когда Postgres, Redis, Kafka, Prometheus UI запущены в Docker, а код запускается локально в IDEA

Шаг 1. Запуск БД, Redis, Kafka, Prometheus UI в Docker:

```bash
docker-compose up -d booking-db redis kafka kafka-init-topics prometheus
```

Шаг 2. Запуск сервиса 

- Из командной строки
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- С помощью графического интерфейса IDE. 
В Edit Configurations создайте конфигурацию Local, в поле VM Options введите -Dspring.profiles.active=local

3. Вариант "Dev" запуск через Docker-compose:
собираем jar файлы сервисов
```bash
mvn clean package -DskipTests
```
собираем образы для всех сервисов
```bash
docker-compose build
```

4. Запустите приложение с помощью команды:

```bash
docker-compose up -d
```
или если вы хотите дождаться, пока все сервисы будут готовы, используйте:
```bash
docker-compose up -d --wait
```

5. Проверка статуса приложения:

```bash
# Статус всех сервисов (должны быть "healthy")
docker compose ps

# Просмотр логов
docker compose logs -f

# Логи конкретного сервиса
docker compose logs -f booking-service
```
6. После запуска приложения сервисы будут доступны по следующим URL:
- Booking Service: http://localhost:8080
- Prometheus UI: http://localhost:9090
- Health Check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

7. Для остановки приложения используйте команду:

```bash
docker-compose down
```
Останавливает все контейнеры, но сохраняет данные в volumes. При следующем запуске все данные будут на месте.
Для остановки и удаления контейнеров, сетей и томов используйте команду:

```bash
docker-compose down -v
```
## Security: JWT Authentication & Authorization

Реализован модуль безопасности, обеспечивающий:

- **JWT-аутентификацию** - защита API с помощью JSON Web Tokens

## Эндпоинты аутентификации

### Регистрация пользователя:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
    "email": "user@example.com",
    "name": "Иван Иванов",
    "password": "Password123"
}

Ответ (201 Created):

json
{
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "email": "user@example.com",
    "role": "USER"
}
```

### Авторизация:
```http
POST /api/v1/auth/authenticate
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "Password123"
}

Ответ (200 OK):

json
{
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "email": "user@example.com",
    "role": "USER"
}
```
## Использование JWT токена
Добавляйте токен в заголовок каждого защищенного запроса:

```text
Authorization: Bearer <your-jwt-token>
```
Для получения текущего аутентифицированного пользователя в любом сервисе:
```java
@Service
public class ExampleService {

    private final UserRepository userRepository;
    
    // Использовать этот метод в своем сервисе
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
    }
    
    // Использование в методах
    public void exampleMethod() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();      // ID пользователя
        String email = currentUser.getEmail();   // Email
        Role role = currentUser.getRole();       // Роль (USER/ADMIN)
    }
}
```
**Важно:** не передавать userId в запросе
