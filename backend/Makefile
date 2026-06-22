.PHONY: fmt generate check-generated lint lint-api test run migrate k6-smoke

fmt:
	gofmt -w ./cmd ./internal

generate:
	go generate ./internal/http/openapi

check-generated: generate
	git diff --exit-code -- internal/http/openapi

lint:
	go vet ./...

lint-api:
	npm --prefix ../01-analysis/api run lint

test:
	go test ./...

run:
	go run ./cmd/api

migrate:
	@echo "migrations are not implemented yet"

k6-smoke:
	@echo "k6 smoke test is not implemented yet"
