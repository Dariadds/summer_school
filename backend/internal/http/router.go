package httpapi

import (
	"log/slog"
	"net/http"

	authapi "summer-school-2026/backend/internal/http/openapi/auth"
	profileapi "summer-school-2026/backend/internal/http/openapi/profile"

	"github.com/go-chi/chi/v5"
)

type healthResponse struct {
	Status string `json:"status"`
}

type RouterOptions struct {
	Auth    authapi.ServerInterface
	Profile profileapi.ServerInterface
}

func NewRouter(logger *slog.Logger, options ...RouterOptions) http.Handler {
	if logger == nil {
		logger = slog.Default()
	}
	var opts RouterOptions
	if len(options) > 0 {
		opts = options[0]
	}

	router := chi.NewRouter()
	router.Use(requestIDMiddleware)
	router.Use(recoverMiddleware(logger))
	router.Use(accessLogMiddleware(logger))
	router.Use(jsonContentTypeMiddleware)
	router.NotFound(func(w http.ResponseWriter, r *http.Request) {
		WriteError(w, http.StatusNotFound, CodeNotFound, "Запрашиваемый ресурс не найден.", nil)
	})
	router.MethodNotAllowed(func(w http.ResponseWriter, r *http.Request) {
		WriteError(w, http.StatusNotFound, CodeNotFound, "Запрашиваемый ресурс не найден.", nil)
	})
	router.Get("/healthz", healthHandler)
	router.Get("/readyz", healthHandler)
	if opts.Auth != nil {
		authapi.HandlerFromMux(opts.Auth, router)
	}
	if opts.Profile != nil {
		profileapi.HandlerFromMux(opts.Profile, router)
	}

	return router
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, healthResponse{Status: "ok"})
}
