package httpapi

import (
	"log/slog"
	"net/http"

	"github.com/go-chi/chi/v5"
)

type healthResponse struct {
	Status string `json:"status"`
}

func NewRouter(logger *slog.Logger) http.Handler {
	if logger == nil {
		logger = slog.Default()
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

	return router
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, healthResponse{Status: "ok"})
}
