#include <iostream>
#include <vector>
#include <string>
#include <map>
#include <set>
#include <algorithm>
#include <mutex>

using namespace std;

class Song {
private:
    string title;
    string artist;
    string album;
    int duration; // in seconds

public:
    Song(string title, string artist, string album, int duration)
        : title(move(title)), artist(move(artist)), album(move(album)), duration(duration) {}

    const string& getTitle() const { return title; }
    const string& getArtist() const { return artist; }
    const string& getAlbum() const { return album; }
    int getDuration() const { return duration; }
};

class Playlist {
private:
    string name;
    vector<Song> songs;

public:
    Playlist() = default;
    explicit Playlist(string name) : name(move(name)) {}

    void addSong(const Song& song) {
        songs.push_back(song);
    }

    void removeSong(const string& title) {
        songs.erase(remove_if(songs.begin(), songs.end(),
            [&title](const Song& song) { return song.getTitle() == title; }), songs.end());
    }

    void display() const {
        cout << "Playlist: " << name << endl;
        for (const auto& song : songs) {
            cout << song.getTitle() << " - " << song.getArtist() << " (" << song.getAlbum() << ")" << endl;
        }
    }

    const vector<Song>& getSongs() const {
        return songs;
    }
};

class User {
private:
    string username;
    string password;
    map<string, Playlist> playlists;

public:
    User() = default;
    User(string username, string password) : username(move(username)), password(move(password)) {}

    bool authenticate(const string& inputPassword) const {
        return password == inputPassword;
    }

    void createPlaylist(const string& playlistName) {
        playlists.emplace(playlistName, Playlist(playlistName));
    }

    void deletePlaylist(const string& playlistName) {
        playlists.erase(playlistName);
    }

    const map<string, Playlist>& getPlaylists() const {
        return playlists;
    }

    map<string, Playlist>& getPlaylists() {
        return playlists;
    }

    const string& getUsername() const {
        return username;
    }
};

class MusicLibrary {
private:
    vector<Song> songs;

public:
    void addSong(const Song& song) {
        songs.push_back(song);
    }

    vector<Song> search(const string& keyword) const {
        vector<Song> results;
        for (const auto& song : songs) {
            if (song.getTitle().find(keyword) != string::npos ||
                song.getArtist().find(keyword) != string::npos ||
                song.getAlbum().find(keyword) != string::npos) {
                results.push_back(song);
            }
        }
        return results;
    }

    const vector<Song>& getSongs() const {
        return songs;
    }
};

class MusicPlayer {
private:
    const Song* currentSong;
    int currentTime; // in seconds
    bool isPlaying;

public:
    MusicPlayer() : currentSong(nullptr), currentTime(0), isPlaying(false) {}

    void play(const Song& song) {
        currentSong = &song;
        currentTime = 0;
        isPlaying = true;
        cout << "Playing: " << song.getTitle() << " by " << song.getArtist() << endl;
    }

    void pause() {
        if (isPlaying && currentSong) {
            isPlaying = false;
            cout << "Paused: " << currentSong->getTitle() << endl;
        }
    }

    void skip() {
        if (currentSong) {
            cout << "Skipped: " << currentSong->getTitle() << endl;
            currentSong = nullptr;
            isPlaying = false;
        }
    }

    void seek(int time) {
        if (currentSong && time >= 0 && time < currentSong->getDuration()) {
            currentTime = time;
            cout << "Seeked to " << time << " seconds in " << currentSong->getTitle() << endl;
        }
    }
};

class RecommendationSystem {
public:
    vector<Song> recommendSongs(const MusicLibrary& library) const {
        vector<Song> recommendations;
        const auto& songs = library.getSongs();
        if (!songs.empty()) {
            recommendations.push_back(songs[0]); 
        }
        return recommendations;
    }

    vector<Playlist> recommendPlaylists(const map<string, Playlist>& playlists) const {
        vector<Playlist> recommendations;
        if (!playlists.empty()) {
            recommendations.push_back(playlists.begin()->second); // Simple recommendation for demonstration purpose
        }
        return recommendations;
    }
};

class StreamingService {
private:
    map<string, User> users;
    MusicLibrary library;
    MusicPlayer player;
    RecommendationSystem recommendationSystem;
    mutable mutex serviceMutex;

public:
    void addUser(const User& user) {
        lock_guard<mutex> lock(serviceMutex);
        users[user.getUsername()] = user;
    }

    void addSongToLibrary(const Song& song) {
        lock_guard<mutex> lock(serviceMutex);
        library.addSong(song);
    }

    void createPlaylist(const string& username, const string& playlistName) {
        lock_guard<mutex> lock(serviceMutex);
        auto it = users.find(username);
        if (it != users.end()) {
            it->second.createPlaylist(playlistName);
        }
    }

    void addSongToPlaylist(const string& username, const string& playlistName, const Song& song) {
        lock_guard<mutex> lock(serviceMutex);
        auto userIt = users.find(username);
        if (userIt != users.end()) {
            auto& playlists = userIt->second.getPlaylists();
            auto playlistIt = playlists.find(playlistName);
            if (playlistIt != playlists.end()) {
                playlistIt->second.addSong(song);
            }
        }
    }

    void playSong(const string& songTitle) {
        lock_guard<mutex> lock(serviceMutex);
        auto searchResults = library.search(songTitle);
        if (!searchResults.empty()) {
            player.play(searchResults[0]);
        }
    }

    void pauseSong() {
        lock_guard<mutex> lock(serviceMutex);
        player.pause();
    }

    void skipSong() {
        lock_guard<mutex> lock(serviceMutex);
        player.skip();
    }

    void seekSong(int time) {
        lock_guard<mutex> lock(serviceMutex);
        player.seek(time);
    }

    vector<Song> searchMusic(const string& keyword) const {
        lock_guard<mutex> lock(serviceMutex);
        return library.search(keyword);
    }

    vector<Song> getRecommendations() const {
        lock_guard<mutex> lock(serviceMutex);
        return recommendationSystem.recommendSongs(library);
    }

   vector<Playlist> getPlaylistRecommendations() const {
    lock_guard<mutex> lock(serviceMutex);
    vector<Playlist> recommendations;
    for (const auto& [_, user] : users) {
        const auto& playlists = user.getPlaylists();
        for (const auto& [__, playlist] : playlists) {
            recommendations.push_back(playlist);
        }
    }
    return recommendations;
    }
};

int main() {
    StreamingService service;

    // Adding users
    service.addUser(User("alice", "password123"));
    service.addUser(User("bob", "password456"));

    cout << "Users added: alice, bob\n\n";

    // Adding songs to the library
    service.addSongToLibrary(Song("Song A", "Artist 1", "Album X", 180));
    service.addSongToLibrary(Song("Song B", "Artist 2", "Album Y", 200));

    cout << "Songs added to library: Song A, Song B\n\n";

    // Creating playlists
    service.createPlaylist("alice", "Alice's Favorites");
    cout << "Playlist created: Alice's Favorites\n";

    service.addSongToPlaylist("alice", "Alice's Favorites", Song("Song A", "Artist 1", "Album X", 180));
    cout << "Song A added to Alice's Favorites\n\n";

    // Playing a song
    cout << "Attempting to play Song A:\n";
    service.playSong("Song A");

    // Pausing the song
    cout << "\nPausing the song:\n";
    service.pauseSong();

    // Seeking in the song
    cout << "\nSeeking to 60 seconds:\n";
    service.seekSong(60);

    // Getting recommendations
    cout << "\nGetting song recommendations:\n";
    auto recommendations = service.getRecommendations();
    if (!recommendations.empty()) {
        cout << "Recommended Song: " << recommendations[0].getTitle() << " by " << recommendations[0].getArtist() << endl;
    } else {
        cout << "No recommendations available.\n";
    }

    // Getting playlist recommendations
    cout << "\nGetting playlist recommendations:\n";
    auto playlistRecommendations = service.getPlaylistRecommendations();
    if (!playlistRecommendations.empty()) {
        cout << "Recommended Playlist: " << playlistRecommendations[0].getSongs()[0].getTitle() << endl;
    } else {
        cout << "No playlist recommendations available.\n";
    }

    return 0;
}