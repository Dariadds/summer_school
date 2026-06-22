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
	go run github.com/pressly/goose/v3/cmd/goose@v3.24.1 -dir migrations postgres "$${DATABASE_URL:-postgres://volna:volna@localhost:5432/volna?sslmode=disable}" up

k6-smoke:
	@echo "k6 smoke test is not implemented yet"
