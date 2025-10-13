package main

import (
	"fmt"
	"log"
	"net/http"

	"github.com/fsnotify/fsnotify"
	"github.com/gorilla/mux"
	"github.com/sachin/practice-questions/filereader"
	"github.com/sachin/practice-questions/handler"
	"github.com/sachin/practice-questions/websocket/utils"
)

const LogPath = "storage/logs.txt"

func main() {
	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		log.Fatal(err)
	}
	defer watcher.Close()
	fileReader := filereader.NewFileReader(LogPath)

	go func() {
		for {
			select {
			case event, ok := <-watcher.Events:
				if !ok {
					return
				}
				if event.Has(fsnotify.Write) {
					newData, err := fileReader.ReadNewLines()
					if err != nil {
						fmt.Println("Watcher error: ", err)
					}
					utils.BroadcastMessageToConnectedClients(newData)
				}
			case err, ok := <-watcher.Errors:
				if !ok {
					return
				}
				fmt.Println("Watcher error: ", err)
			}
		}
	}()

	err = watcher.Add(LogPath)
	if err != nil {
		log.Fatal(err)
	}
	last10Lines, err := fileReader.ReadLast10Lines()
	if err != nil {
		fmt.Println(err)
	}

	router := mux.NewRouter()
	router.HandleFunc("/logs", handler.HomePageHandler)
	router.HandleFunc("/ws", handler.WebSocketHandler(last10Lines))
	fmt.Println("Server started at port :8081")
	log.Fatal(http.ListenAndServe(":8081", router))
}
