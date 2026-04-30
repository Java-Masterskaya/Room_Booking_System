Запуск приложения

1. Склонируйте репозиторий и перейдите в папку проекта
```bash
git clone https://github.com/Java-Masterskaya/Room_Booking_System.git
cd Room_Booking_System
```
2. Настройте переменные окружения в файле .env по примеру .env.example
```bash
cp .env.example .env
```

Откройте `.env` в редакторе и замените плейсхолдеры на реальные значения:

```bash
POSTGRES_DB=bookingdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=ваш-безопасный-пароль
KAFKA_CLUSTER_ID=уникальный-id-кластера
```

3. Запустите команду для сборки проекта:
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
docker-compose up -d -wait
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

6. Для остановки приложения используйте команду:

```bash
docker-compose down
```
Останавливает все контейнеры, но сохраняет данные в volumes. При следующем запуске все данные будут на месте.
Для остановки и удаления контейнеров, сетей и томов используйте команду:

```bash
docker-compose down -v
```
