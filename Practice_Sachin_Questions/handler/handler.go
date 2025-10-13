package handler

import (
	"net/http"

	"github.com/sachin/practice-questions/websocket"
)

func HomePageHandler(w http.ResponseWriter, r *http.Request) {
	http.ServeFile(w, r, "frontend/index.html")
}

func WebSocketHandler(last10Lines []string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		websocket.WebSocketHandler(w, r, last10Lines)
	}
}
