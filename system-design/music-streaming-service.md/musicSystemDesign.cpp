#include <bits/stdc++.h>
#include <iostream>
#include <mutex>
using namespace std;

enum class PlayState {
    STOP,
    PLAYING,
    PAUSED
};

// ------------------ SONG ------------------
class Song {
public:
    string songId, title, artist, album;
    int duration; //in seconds

    Song(string id, string t, string a, string alb, int d)
        : songId(id), title(t), artist(a), album(alb), duration(d) {}
};

// ------------------ USER ------------------
class User {
public:
    string userId, name, password;
    unordered_set<string> likedSongs;
    vector<string> listeningHistory;

    User(string id, string n, string p) : userId(id), name(n), password(p) {}

    bool authenticate(const string& pwd) {
        return password == pwd;  // FIXED: == instead of =
    }

    void likeSong(const string& songId) {
        likedSongs.insert(songId);
    }

    void logHistory(const string& songId) {
        listeningHistory.push_back(songId);
    }
};

// ------------------ PLAYLIST ------------------
class Playlist {
public:
    string playlistId, name, ownerId;
    vector<string> songIds;

    Playlist(string id, string n, string owner) : playlistId(id), name(n), ownerId(owner) {}

    void addSong(const string& songId) {
        songIds.push_back(songId);
    }

    void removeSong(const string& songId) {
        songIds.erase(remove(songIds.begin(), songIds.end(), songId), songIds.end());
    }
};

// ------------------ MUSIC PLAYER ------------------
class MusicPlayer {
    PlayState state;
    int currentTime; // seconds
    Song* currentSong;

public:
    MusicPlayer() : state(PlayState::STOP), currentTime(0), currentSong(nullptr) {}

    void play(Song* song) {
        currentSong = song;
        state = PlayState::PLAYING;
        currentTime = 0;
        cout << "Playing: " << song->title << " by " << song->artist << endl;
    }

    void pause() {
        if (state == PlayState::PLAYING) {
            state = PlayState::PAUSED;
            cout << "Paused." << endl;
        }
    }

    void resume() {
        if (state == PlayState::PAUSED) {
            state = PlayState::PLAYING;
            cout << "Resumed." << endl;
        }
    }

    void seek(int seconds) {
        if (currentSong && seconds < currentSong->duration) {
            currentTime = seconds;
            cout << "Seeked to: " << currentTime << " seconds" << endl;
        }
    }

    void stop() {
        state = PlayState::STOP;
        currentTime = 0;
        currentSong = nullptr;
        cout << "Stopped." << endl;
    }
};

// ------------------ MUSIC SERVICE ------------------
class MusicService {
    unordered_map<string, Song*> songs;
    unordered_map<string, User*> users;
    unordered_map<string, Playlist*> playlists;
    mutex mtx;

public:
    void addSong(Song* song) {
        lock_guard<mutex> lock(mtx);
        songs[song->songId] = song;
    }

    void addUser(User* user) {
        lock_guard<mutex> lock(mtx);
        users[user->userId] = user;
    }

    User* authenticate(const string& userId, const string& pwd) {
        lock_guard<mutex> lock(mtx);
        if (users.count(userId) && users[userId]->authenticate(pwd))
            return users[userId];
        return nullptr;
    }

    Song* searchSongByTitle(const string& title) {
        for (auto& pair : songs)
            if (pair.second->title == title)
                return pair.second;
        return nullptr;
    }

    Playlist* createPlaylist(const string& name, const string& ownerId) {
        string pid = "PL" + to_string(playlists.size() + 1);
        Playlist* pl = new Playlist(pid, name, ownerId);
        playlists[pid] = pl;
        return pl;
    }

    void recommendSongs(User* user) {
        cout << "Recommended songs for " << user->name << ": ";
        for (auto& pair : songs) {
            if (user->likedSongs.count(pair.first) == 0)
                cout << pair.second->title << " ";
        }
        cout << endl;
    }

    Song* getSongById(const string& id) {
        return songs.count(id) ? songs[id] : nullptr;
    }

    Playlist* getPlaylistById(const string& id) {
        return playlists.count(id) ? playlists[id] : nullptr;
    }
};

// ------------------ MAIN ------------------
int main() {
    MusicService service;
    MusicPlayer player;

    // Setup
    service.addSong(new Song("S1", "Closer", "Chainsmokers", "Memories", 240));
    service.addSong(new Song("S2", "Shape of You", "Ed Sheeran", "Divide", 260));

    service.addUser(new User("U1", "Sachin", "1234"));

    // Auth
    User* u = service.authenticate("U1", "1234");
    if (!u) {
        cout << "Auth failed.\n";
        return 1;
    }

    // Song Search and Play
    Song* song = service.searchSongByTitle("Closer");
    if (song) {
        player.play(song);
        u->logHistory(song->songId);
        u->likeSong(song->songId);
    }

    // Seek and pause
    player.seek(120);
    player.pause();
    player.resume();

    // Playlist
    Playlist* pl = service.createPlaylist("Workout", u->userId);
    pl->addSong(song->songId);
    cout << "Playlist created: " << pl->name << endl;

    // Recommendations
    service.recommendSongs(u);

    return 0;
}
