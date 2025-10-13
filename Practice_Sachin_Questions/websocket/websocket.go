package websocket

import (
	"fmt"
	"net/http"

	"github.com/gorilla/websocket"
	"github.com/sachin/practice-questions/websocket/utils"
)

var (
	wsUpgrader = websocket.Upgrader{
		ReadBufferSize:  1024,
		WriteBufferSize: 1024,
		CheckOrigin: func(r *http.Request) bool {
			return r.Host == "localhost:8081"
		},
	}
)

func WebSocketHandler(w http.ResponseWriter, r *http.Request, last10Lines []string) {
	conn, err := wsUpgrader.Upgrade(w, r, nil)
	if err != nil {
		fmt.Println("Error upgrading to websocket connection: ", err)
		return
	}
	utils.BroadcastMessageToNewClient(conn, last10Lines)
	go handleWebSocket(conn)
}

func handleWebSocket(conn *websocket.Conn) {
	defer func() {
		if err := conn.Close(); err != nil {
			fmt.Println("Error closing websocket connection:", err)
		}
	}()

	// keep connection open
	select {}
}
