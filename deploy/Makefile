.PHONY: help check-env check-docker check-network infra init app deploy down restart clean logs status health rollback backup

# Конфигурация
TAG ?= latest
export TAG
COMPOSE_APP = docker compose -f docker-compose.yml
COMPOSE_INFRA = docker compose -f infra-compose.yml
COMPOSE_INIT = docker compose -f init-compose.yml
NETWORK_NAME = room-booking-network
ENV_FILE = .env

# Цвета для вывода
GREEN = \033[0;32m
RED = \033[0;31m
YELLOW = \033[1;33m
NC = \033[0m # No Color

# ============================================
# HELP
# ============================================
help: ## Показать все доступные команды
	@echo "$(GREEN)Доступные команды:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}'

# ============================================
# CHECKS
# ============================================

check-env: ## Проверить наличие .env файла
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "$(RED)[ERROR]$(NC) $(ENV_FILE) file not found!"; \
		echo "Create it from .env.example or manually"; \
		exit 1; \
	fi
	@echo "$(GREEN)[OK]$(NC) $(ENV_FILE) exists"

check-docker: ## Проверить что Docker запущен
	@if ! docker info > /dev/null 2>&1; then \
		echo "$(RED)[ERROR]$(NC) Docker is not running!"; \
		exit 1; \
	fi
	@echo "$(GREEN)[OK]$(NC) Docker is running"

check-network: ## Проверить что сеть существует
	@if ! docker network ls | grep -q $(NETWORK_NAME); then \
		echo "$(YELLOW)[WARN]$(NC) Network $(NETWORK_NAME) not found, creating..."; \
		docker network create $(NETWORK_NAME); \
		echo "$(GREEN)[OK]$(NC) Network created"; \
	else \
		echo "$(GREEN)[OK]$(NC) Network $(NETWORK_NAME) exists"; \
	fi

check-kafka: ## Проверить что Kafka готова
	@echo "Waiting for Kafka to be healthy..."
	@max_attempts=30; \
	attempt=1; \
	while [ $$attempt -le $$max_attempts ]; do \
		if docker compose -f infra-compose.yml exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then \
			echo "$(GREEN)[OK]$(NC) Kafka is ready"; \
			break; \
		fi; \
		echo "  Attempt $$attempt/$$max_attempts - waiting..."; \
		sleep 2; \
		attempt=$$((attempt + 1)); \
	done; \
	if [ $$attempt -gt $$max_attempts ]; then \
		echo "$(RED)[ERROR]$(NC) Kafka failed to start after $$max_attempts attempts"; \
		exit 1; \
	fi

check-postgres: ## Проверить что PostgreSQL готова
	@echo "Waiting for PostgreSQL to be healthy..."
	@max_attempts=30; \
	attempt=1; \
	while [ $$attempt -le $$max_attempts ]; do \
		if docker compose -f infra-compose.yml exec -T booking-db pg_isready -U postgres > /dev/null 2>&1; then \
			echo "$(GREEN)[OK]$(NC) PostgreSQL is ready"; \
			break; \
		fi; \
		echo "  Attempt $$attempt/$$max_attempts - waiting..."; \
		sleep 2; \
		attempt=$$((attempt + 1)); \
	done; \
	if [ $$attempt -gt $$max_attempts ]; then \
		echo "$(RED)[ERROR]$(NC) PostgreSQL failed to start after $$max_attempts attempts"; \
		exit 1; \
	fi

check-all: check-docker check-env check-network ## Запустить все проверки

# ============================================
# INFRASTRUCTURE
# ============================================

infra: check-all ## Поднять инфраструктуру (БД, Kafka, Redis, Prometheus)
	@echo "$(YELLOW)Starting infrastructure...$(NC)"
	$(COMPOSE_INFRA) up -d --wait
	@echo "$(GREEN)[OK]$(NC) Infrastructure started"
	@make check-postgres
	@make check-kafka
	@echo "$(GREEN)✅ Infrastructure is ready$(NC)"

infra-down: ## Остановить инфраструктуру
	@echo "$(YELLOW)Stopping infrastructure...$(NC)"
	$(COMPOSE_INFRA) down
	@echo "$(GREEN)[OK]$(NC) Infrastructure stopped"

infra-restart: infra-down infra ## Перезапустить инфраструктуру

# ============================================
# INITIALIZATION
# ============================================

init: check-kafka ## Запустить инициализацию (топики Kafka)
	@echo "$(YELLOW)Running initialization...$(NC)"
	$(COMPOSE_INIT) up -d
	@echo "Waiting for init to complete..."
	@sleep 10
	@if docker compose -f init-compose.yml ps -a | grep -q "kafka-init-topics.*Exited"; then \
		exit_code=$$(docker compose -f init-compose.yml ps -a | grep kafka-init-topics | awk '{print $$4}' | grep -o '[0-9]*'); \
		if [ "$$exit_code" = "0" ]; then \
			echo "$(GREEN)[OK]$(NC) Initialization completed successfully"; \
		else \
			echo "$(RED)[ERROR]$(NC) Initialization failed with exit code $$exit_code"; \
			docker compose -f init-compose.yml logs kafka-init-topics; \
		fi; \
	fi
	$(COMPOSE_INIT) rm -f || true
	@echo "$(GREEN)✅ Initialization done$(NC)"

# ============================================
# APPLICATION
# ============================================

app: check-all ## Запустить приложение
	@echo "$(YELLOW)Deploying app with TAG=$(TAG)...$(NC)"
	$(COMPOSE_APP) up -d
	@echo "Waiting for app to start..."
	@sleep 5
	@if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then \
		echo "$(GREEN)[OK]$(NC) App is healthy"; \
	else \
		echo "$(YELLOW)[WARN]$(NC) App might still be starting, check logs"; \
	fi
	@echo "$(GREEN)✅ App deployed with TAG=$(TAG)$(NC)"

app-down: ## Остановить приложение
	@echo "$(YELLOW)Stopping app...$(NC)"
	$(COMPOSE_APP) down
	@echo "$(GREEN)[OK]$(NC) App stopped"

app-restart: app-down app ## Перезапустить приложение

app-logs: ## Посмотреть логи приложения
	$(COMPOSE_APP) logs -f

# ============================================
# FULL DEPLOY
# ============================================

deploy: infra init app ## Полный деплой (инфраструктура + инициализация + приложение)
	@echo "$(GREEN)========================================$(NC)"
	@echo "$(GREEN)✅ Deploy completed with TAG=$(TAG)$(NC)"
	@echo "$(GREEN)========================================$(NC)"
	@make status

# ============================================
# ROLLBACK
# ============================================

rollback: ## Откатить приложение на предыдущую версию
	@read -p "Enter TAG to rollback: " rollback_tag; \
	if [ -z "$$rollback_tag" ]; then \
		echo "$(RED)[ERROR]$(NC) TAG is required"; \
		exit 1; \
	fi; \
	echo "$(YELLOW)Rolling back to TAG=$$rollback_tag...$(NC)"; \
	TAG=$$rollback_tag $(COMPOSE_APP) up -d; \
	echo "$(GREEN)[OK]$(NC) Rolled back to $$rollback_tag"

# ============================================
# MAINTENANCE
# ============================================

down: ## Остановить всё
	@echo "$(YELLOW)Stopping all services...$(NC)"
	-$(COMPOSE_APP) down
	-$(COMPOSE_INFRA) down
	-$(COMPOSE_INIT) down
	@echo "$(GREEN)[OK]$(NC) All services stopped"

clean: down ## Полная очистка (удалить контейнеры и volumes) ⚠️
	@echo "$(RED)⚠️  This will delete all data!$(NC)"
	@read -p "Are you sure? [y/N]: " confirm; \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		$(COMPOSE_APP) down -v; \
		$(COMPOSE_INFRA) down -v; \
		$(COMPOSE_INIT) down -v; \
		echo "$(GREEN)[OK]$(NC) All services and volumes removed"; \
	else \
		echo "Cancelled"; \
	fi

restart: down deploy ## Полный перезапуск всего

# ============================================
# MONITORING
# ============================================

status: ## Показать статус всех сервисов
	@echo "$(YELLOW)=== App Services ===$(NC)"
	@$(COMPOSE_APP) ps 2>/dev/null || echo "No app services running"
	@echo ""
	@echo "$(YELLOW)=== Infrastructure Services ===$(NC)"
	@$(COMPOSE_INFRA) ps 2>/dev/null || echo "No infrastructure services running"

logs: ## Посмотреть все логи
	@echo "$(YELLOW)Press Ctrl+C to exit$(NC)"
	$(COMPOSE_APP) logs -f &
	$(COMPOSE_INFRA) logs -f &
	wait

health: ## Проверить здоровье всех сервисов
	@echo "$(YELLOW)Checking health...$(NC)"
	@echo ""
	@echo "App:"
	@curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || echo "  $(RED)App is not responding$(NC)"
	@echo ""
	@echo "Prometheus:"
	@curl -s -o /dev/null -w "  HTTP Status: %{http_code}\n" http://localhost:9090/-/ready || echo "  $(RED)Prometheus is not responding$(NC)"
	@echo ""
	@echo "PostgreSQL:"
	@docker compose -f infra-compose.yml exec -T booking-db pg_isready -U postgres 2>/dev/null || echo "  $(RED)PostgreSQL is not responding$(NC)"
	@echo ""
	@echo "Kafka:"
	@docker compose -f infra-compose.yml exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "  $(GREEN)OK$(NC)" || echo "  $(RED)Kafka is not responding$(NC)"
	@echo ""
	@echo "Redis:"
	@docker compose -f infra-compose.yml exec -T booking-redis redis-cli ping 2>/dev/null || echo "  $(RED)Redis is not responding$(NC)"

# ============================================
# BACKUP
# ============================================

backup-db: ## Создать бэкап PostgreSQL
	@timestamp=$$(date +%Y%m%d_%H%M%S); \
	backup_dir="./backups"; \
	mkdir -p $$backup_dir; \
	echo "$(YELLOW)Creating database backup...$(NC)"; \
	docker compose -f infra-compose.yml exec -T booking-db pg_dump -U postgres bookingdb > $$backup_dir/backup_$$timestamp.sql; \
	echo "$(GREEN)[OK]$(NC) Backup saved to $$backup_dir/backup_$$timestamp.sql"

restore-db: ## Восстановить PostgreSQL из бэкапа
	@read -p "Enter backup file path: " backup_file; \
	if [ ! -f "$$backup_file" ]; then \
		echo "$(RED)[ERROR]$(NC) File not found: $$backup_file"; \
		exit 1; \
	fi; \
	echo "$(YELLOW)Restoring database from $$backup_file...$(NC)"; \
	docker compose -f infra-compose.yml exec -T booking-db psql -U postgres bookingdb < $$backup_file; \
	echo "$(GREEN)[OK]$(NC) Database restored"

# ============================================
# UTILS
# ============================================

shell-app: ## Зайти в контейнер приложения
	$(COMPOSE_APP) exec app /bin/sh

shell-db: ## Зайти в контейнер PostgreSQL
	$(COMPOSE_INFRA) exec booking-db psql -U postgres

shell-kafka: ## Зайти в контейнер Kafka
	$(COMPOSE_INFRA) exec kafka /bin/bash

shell-redis: ## Зайти в контейнер Redis
	$(COMPOSE_INFRA) exec booking-redis redis-cli

update-images: ## Обновить все Docker образы
	@echo "$(YELLOW)Pulling latest images...$(NC)"
	$(COMPOSE_APP) pull
	$(COMPOSE_INFRA) pull
	@echo "$(GREEN)[OK]$(NC) Images updated"

prune: ## Очистить неиспользуемые Docker ресурсы
	@echo "$(YELLOW)Cleaning up Docker...$(NC)"
	docker system prune -f
	@echo "$(GREEN)[OK]$(NC) Cleanup done"