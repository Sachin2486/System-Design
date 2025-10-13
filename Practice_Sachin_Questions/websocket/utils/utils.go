package utils

import (
	"fmt"
	"strings"
	"sync"

	"github.com/gorilla/websocket"
)

var (
	connectedClients = make(map[*websocket.Conn]bool)
	mu               = sync.Mutex{}
)

func addClient(conn *websocket.Conn) {
	mu.Lock()
	defer mu.Unlock()
	fmt.Println("Adding new client connection: ", conn.RemoteAddr())
	connectedClients[conn] = true
}

func removeClient(conn *websocket.Conn) {
	mu.Lock()
	defer mu.Unlock()
	fmt.Println("Removing client connection: ", conn.RemoteAddr())
	delete(connectedClients, conn)
}

func BroadcastMessageToConnectedClients(latestData []string) {
	for client := range connectedClients {
		strData := strings.Join(latestData, "\n")
		err := client.WriteMessage(websocket.TextMessage, []byte(strData))
		if err != nil {
			removeClient(client)
			_ = client.Close()
		}
	}
}

func BroadcastMessageToNewClient(client *websocket.Conn, logs []string) {
	logData := strings.Join(logs, "\n")
	err := client.WriteMessage(websocket.TextMessage, []byte(logData))
	if err != nil {
		fmt.Println("Error broadcasting message to new client: ", err)
		closeErr := client.Close()
		if closeErr != nil {
			return
		}
	} else {
		addClient(client)
	}
}
