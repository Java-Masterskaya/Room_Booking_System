.PHONY: ci unit-test integration-test ci-up ci-down ci-clean

# Unit-тесты (аналог джоба "unit-tests")
unit-test:
	mvn clean test

# Интеграционные тесты локально
integration-test:
	mvn clean package
	docker compose up -d --build --wait --timeout 300
	curl --fail http://localhost:8080/actuator/health || (docker compose logs && exit 1)
	@echo "Integration tests OK"

# Запустить стек для ручной проверки
ci-up:
	mvn clean package
	docker compose up -d --build --wait --timeout 300

# Остановить стек
ci-down:
	docker compose down -v

# Полный прогон как в CI
ci: integration-test ci-down