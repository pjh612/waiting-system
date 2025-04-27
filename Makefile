.PHONY: run

run:
	@echo "Running ./gradlew buildDockerImage"
	@./gradlew buildDockerImage
	@echo "Running docker-compose up -d"
	@docker-compose up -d