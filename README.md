## Запуск приложения

1. Склонируйте репозиторий и перейдите в папку проекта

```bash
git clone https://github.com/Java-Masterskaya/Room_Booking_System.git
cd Room_Booking_System
```

Настройте переменные окружения в файле .env по примеру .env.example

```bash
cp .env .env
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

## Локальный запуск CI pipline

На текущий момент в CI pipeline включены следующие этапы:

- Юнит-тесты (для PR при пуше в develop)
- Юнит-тесты + сборка образов + запуск контейнеров + проверка здоровья
  (для PR в main или опционально если через Github UI)

Убедитесь, что у вас установлен Make, для этого выполните команду:

```bash
make --version
```

Если Make не установлен, следуйте инструкциям для вашей операционной системы:

- **Для Windows**: Установите [Chocolatey](https://chocolatey.org/install) и затем выполните `choco install make`.
- **Для macOS**: Установите [Homebrew](https://brew.sh/) и затем выполните `brew install make`.
- **Для Linux**: Используйте пакетный менеджер вашей дистрибуции, например, `sudo apt-get install make` для
  Debian/Ubuntu.

Для локального тестирования CI pipeline используйте команды:

- Для запуска всех этапов CI (тесты, сборка образов, запуск контейнеров, проверка здоровья):

```bash
make ci
```

- Для запуска только юнит-тестов:

```bash
make unit-test
```

- Для запуска для ручной проверки сборки образов и запуска контейнеров:

```bash
make ci-up
```

- Остановка контейнеров и удаление ресурсов после проверки:

```bash
make ci-down
```

## CD и развертывание на сервере

### Предварительные требования

1. **Установите Docker и Docker Compose на сервере:**
   ```bash
   docker --version      # Должно быть 24.0+
   docker compose version # Должно быть 2.0+
    ```

2. **Установить Make для удобного управления развертыванием
   (опционально, можно использовать команды Docker Compose напрямую):**

```bash
make --version  # Если нет: sudo apt install make
```

3. **Создайте GitHub Personal Access Token (classic) с правами read:packages:**
    - Перейдите: GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
    - Выдайте права: read:packages
    - Сохраните токен

### Первичная настройка сервера

1. **Создайте директорию для проекта:**

```bash
mkdir -p /opt/room-booking
cd /opt/room-booking
```

2. **Создайте Docker сеть:**

```bash
docker network create room-booking-network
```

3. **Скопируйте файлы деплоя на сервер. Структура должна быть такой:**

```text
/opt/room-booking
├── .env                    # Конфигурация окружения (создать из примера)
├── docker-compose.yml      # Docker Compose для приложения
├── infra-compose.yml       # Docker Compose для инфраструктуры
├── init-compose.yml        # Docker Compose для инициализации
├── Makefile                # Makefile для управления
├── config/
|   └── prometheus.yml      # Конфигурация Prometheus
└── scripts/
    └── create-topics.sh    # Скрипт создания топиков Kafka
```

4. **Настройте .env файл:**

```bash
# Пример .env для dev окружения
SPRING_PROFILES_ACTIVE=dev

POSTGRES_HOST_DEV=booking-db
POSTGRES_PORT=5432
POSTGRES_DB=bookingdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

REDIS_HOST_DEV=booking-redis
REDIS_PORT=6379

KAFKA_HOST_DEV=kafka
KAFKA_PORT_DEV=29092 
```

5. **Залогиньтесь в GitHub Container Registry:**

```bash
# Замените YOUR_USERNAME на ваш GitHub username
# Вставьте ваш Personal Access Token когда спросит пароль
docker login ghcr.io -u YOUR_USERNAME
```

### Развертывание

- Автоматически (через Make):

```bash
cd /opt/room-booking
make deploy TAG=latest
```

- Вручную (через Docker Compose):

```bash
cd /opt/room-booking

# 1. Запустить инфраструктуру
docker compose -p room-booking -f infra-compose.yml up -d --wait

# 2. Проверить что БД готова
docker compose -p room-booking -f infra-compose.yml exec -T booking-db pg_isready -U postgres

# 3. Проверить что Kafka готова
docker compose -p room-booking -f infra-compose.yml exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# 4. Запустить инициализацию (создание топиков)
docker compose -p room-booking -f init-compose.yml up -d

# 5. Запустить приложение
TAG=latest docker compose -p room-booking -f docker-compose.yml up -d

# 6. Проверить здоровье
curl http://localhost:8080/actuator/health
```

### Проверка статуса и логов

```bash
# Статус всех сервисов
make status

# Здоровье сервисов
make health

# Логи приложения
make app-logs

# Логи всех сервисов
make logs
```

### Полезные команды

```bash
make help          # Показать все доступные команды
make restart       # Полный перезапуск
make rollback      # Откатить на предыдущую версию
make backup-db     # Создать бэкап базы данных
make clean         # Полная очистка (удаляет все данные!)
make shell-app     # Зайти в контейнер приложения
make shell-db      # Подключиться к PostgreSQL
```

### Обновление на новую версию

```bash
# 1. Залогиниться в GHCR (если токен истёк)
docker login ghcr.io -u YOUR_USERNAME

# 2. Задеплоить новую версию
cd /opt/room-booking
make deploy TAG=develop-abc123def  # конкретный тег из CI/CD

# Или использовать latest
make deploy TAG=latest
```

### Устранение неполадок

```bash
# Проверить что все контейнеры запущены
docker ps -a --filter "network=room-booking-network"

# Посмотреть логи конкретного сервиса
docker compose -p room-booking -f docker-compose.yml logs app
docker compose -p room-booking -f infra-compose.yml logs booking-db

# Проверить переменные окружения в контейнере
docker exec room-booking-app-1 env | grep SPRING_PROFILES_ACTIVE

# Проверить доступность сервисов
docker exec room-booking-app-1 ping -c 1 booking-db
docker exec room-booking-app-1 nc -zv booking-db 5432
```

### Примечания

- CI/CD автоматически деплоит при merge в develop или main
- Образы хранятся в GitHub Container Registry: ghcr.io/java-masterskaya/room-booking-system
- Тег образа формируется как {ветка}-{SHA коммита} (например: develop-abc123def)
- latest тег всегда указывает на последний задеплоенный образ
- Для продакшена используйте конкретные теги, а не latest

## Документация и безопасность

Подробное описание архитектуры безопасности, валидации OpenAPI-контрактов, лимитов трафика и механизмов маскирования
конфиденциальных данных (защита логов от утечки токенов) находится в файле [SECURITY.md](./SECURITY.md).

## Примечание для разработчиков

## Инструкция по добавлению нового микросервиса

### Файлы, требующие изменений

| Файл                         | Назначение           | Что изменить                                                |
|------------------------------|----------------------|-------------------------------------------------------------|
| `.github/workflows/main.yml` | CI Pipeline          | Добавить проверку здоровья нового сервиса и верификацию JAR |
| `.github/workflows/cd.yml`   | CD Pipeline          | Добавить сборку и публикацию Docker образа                  |
| `deploy/docker-compose.yml`  | Описание сервисов    | Добавить конфигурацию нового сервиса                        |
| `deploy/.env`                | Переменные окружения | Добавить переменные для сервиса (опционально)               |

---

### Важные правила

- **Имена образов** должны соответствовать шаблону: `ghcr.io/java-masterskaya/{service-name}`
- **Порты** не должны конфликтовать (каждый сервис на своём порту)
- **Healthcheck** обязателен для каждого сервиса
- **Зависимости** (depends_on) указывайте явно
- **Ресурсы** настраивайте с учётом общей нагрузки на сервер
- **Service discovery** через Docker DNS (имена контейнеров = хосты)
- **Все сервисы** должны быть в сети room-booking-network

---

### Чек-лист при добавлении нового сервиса

```text
[ ] Создан Maven модуль с pom.xml
[ ] Создан Dockerfile в корне модуля
[ ] Добавлен healthcheck endpoint (Actuator)
[ ] Обновлён CI: health check + verify JARs
[ ] Обновлён CD: build + push образов
[ ] Обновлён deploy/docker-compose.yml
[ ] Обновлён deploy/.env (если нужны новые переменные)
[ ] Проверены порты на конфликты
[ ] Указаны корректные depends_on
[ ] Протестирован деплой на сервере
```

---

### Проверка после добавления

```bash
# 1. Проверить что образ собирается локально
docker build -f notification-service/Dockerfile -t test-notification notification-service

# 2. Проверить валидность docker-compose
docker compose -f deploy/docker-compose.yml config --quiet

# 3. Проверить конфликты портов
netstat -tlnp | grep -E "8080|8081|8082"

# 4. Проверить здоровье после деплоя
curl -s http://localhost:8080/actuator/health | jq .
curl -s http://localhost:8081/actuator/health | jq .
curl -s http://localhost:8082/actuator/health | jq .
```

