import java.util.*;
import java.util.concurrent.*;

// ---------------------- ENTITY CLASSES ----------------------

class User {
    private final int userID;
    private final String name;
    private final String email;
    private final String password;
    private final List<Playlist> playlists;
    private final Map<Integer,Integer> listeningHistory; //SongID -> Count
    
    public User(int userID, String name, String email, String password) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.password = password;
        this.playlists = new ArrayList<>();
        this.listeningHistory = new ConcurrentHashMap<>();
    }
    
    public int getUserId() { return userID; }
    public String getName() { return name; }

    public boolean authenticate(String password) {
        return this.password.equals(password);
    }

    public void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
    }

    public List<Playlist> getPlaylists() { return playlists; }

    public void addToListeningHistory(int songId) {
        listeningHistory.put(songId, listeningHistory.getOrDefault(songId, 0) + 1);
    }

    public Map<Integer, Integer> getListeningHistory() { return listeningHistory; }
}

class Artist {
    private final int artistId;
    private final String name;
    
    public Artist(int artistId, String name) {
        this.artistId = artistId;
        this.name = name;
    }
    
    public int getArtistId() {
        return artistId;
    }
    
    public String getName() {
        return name;
    }
}

class Album {
    private final int albumId;
    private final String title;
    private final Artist artist;
    private final List<Song> songs;
    
    public Album(int albumId, String title, Artist artist){
        this.albumId = albumId;
        this.title = title;
        this.artist = artist;
        this.songs = new ArrayList<>();
    }
    
    public void addSong(Song song) {
        songs.add(song);
    }
    
    public List<Song> getSongs() {
        return songs;
    }
    
    public Artist getArtist() {
        return artist;
    }
    
    public String getTitle() {
        return title;
    }
}

class Song {
    private final int songId;
    private final String title;
    private final Artist artist;
    private final Album album;
    private final int duration; // in seconds
    
    public Song(int songId, String title, Artist artist, Album album, int duration) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
    }
    
    public int getSongId() { return songId; }
    public String getTitle() { return title; }
    public Artist getArtist() { return artist; }
    public Album getAlbum() { return album; }
    public int getDuration() { return duration; }
}

class Playlist {
    private final int playlistId;
    private final String name;
    private final List<Song> songs;

    public Playlist(int playlistId, String name) {
        this.playlistId = playlistId;
        this.name = name;
        this.songs = new CopyOnWriteArrayList<>();
    }

    public void addSong(Song song) {
        songs.add(song);
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }

    public List<Song> getSongs() { return songs; }

    public String getName() { return name; }
}

class AuthService {
    private final Map<String, User> loggedInUsers = new ConcurrentHashMap<>();

    public boolean login(User user, String password) {
        if (user.authenticate(password)) {
            loggedInUsers.put(user.getName(), user);
            System.out.println(user.getName() + " logged in successfully.");
            return true;
        }
        System.out.println("Invalid credentials for " + user.getName());
        return false;
    }

    public void logout(User user) {
        loggedInUsers.remove(user.getName());
        System.out.println(user.getName() + " logged out successfully.");
    }
}

class MusicLibrary {
    private final Map<Integer, Song> songs = new ConcurrentHashMap<>();
    private final Map<Integer, Artist> artists = new ConcurrentHashMap<>();
    private final Map<Integer, Album> albums = new ConcurrentHashMap<>();

    public void addArtist(Artist artist) {
        artists.put(artist.getArtistId(), artist);
    }

    public void addAlbum(Album album) {
        albums.put(album.getArtist().getArtistId(), album);
    }

    public void addSong(Song song) {
        songs.put(song.getSongId(), song);
    }

    public Song getSong(int id) {
        return songs.get(id);
    }

    public List<Song> searchSong(String keyword) {
        List<Song> result = new ArrayList<>();
        for (Song song : songs.values()) {
            if (song.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                song.getArtist().getName().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(song);
            }
        }
        return result;
    }

    public Collection<Song> getAllSongs() {
        return songs.values();
    }
}

class Player {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<User, Song> userCurrentSong = new ConcurrentHashMap<>();

    public void playSong(User user, Song song) {
        userCurrentSong.put(user, song);
        executor.submit(() -> {
            try {
                System.out.println(user.getName() + " started playing: " + song.getTitle());
                for (int i = 1; i <= song.getDuration() / 5; i++) {
                    Thread.sleep(500);
                }
                System.out.println(user.getName() + " finished playing: " + song.getTitle());
                user.addToListeningHistory(song.getSongId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void stopSong(User user) {
        Song song = userCurrentSong.remove(user);
        if (song != null) {
            System.out.println(user.getName() + " stopped the song: " + song.getTitle());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}

class RecommendationService {
    private final MusicLibrary library;

    public RecommendationService(MusicLibrary library) {
        this.library = library;
    }

    public List<Song> recommendSongs(User user) {
        Map<Integer, Integer> history = user.getListeningHistory();
        if (history.isEmpty()) {
            return new ArrayList<>(library.getAllSongs());
        }

        List<Integer> topSongs = new ArrayList<>(history.keySet());
        Collections.shuffle(topSongs);

        List<Song> recommendations = new ArrayList<>();
        for (Integer id : topSongs) {
            Song s = library.getSong(id);
            if (s != null) recommendations.add(s);
        }
        return recommendations;
    }
}

// ---------------------- MAIN CLASS ----------------------

public class MusicStreamingSystem {
    public static void main(String[] args) throws InterruptedException {
        // Initialize core managers
        MusicLibrary library = new MusicLibrary();
        AuthService authService = new AuthService();
        Player player = new Player();
        RecommendationService recommender = new RecommendationService(library);

        // Artists, Albums, and Songs
        Artist artist1 = new Artist(1, "The Synths");
        Artist artist2 = new Artist(2, "Acoustic Duo");
        library.addArtist(artist1);
        library.addArtist(artist2);

        Album album1 = new Album(1, "Neon Nights", artist1);
        Album album2 = new Album(2, "Morning Coffee", artist2);

        Song s1 = new Song(1, "Midnight Drive", artist1, album1, 15);
        Song s2 = new Song(2, "Electric Dreams", artist1, album1, 20);
        Song s3 = new Song(3, "Sunrise", artist2, album2, 15);
        Song s4 = new Song(4, "Calm Breeze", artist2, album2, 20);

        library.addSong(s1);
        library.addSong(s2);
        library.addSong(s3);
        library.addSong(s4);

        album1.addSong(s1);
        album1.addSong(s2);
        album2.addSong(s3);
        album2.addSong(s4);

        // Users
        User u1 = new User(1, "Alice", "alice@mail.com", "123");
        User u2 = new User(2, "Bob", "bob@mail.com", "456");

        // Login
        authService.login(u1, "123");
        authService.login(u2, "456");

        // Playlist creation
        Playlist chill = new Playlist(1, "Chill Vibes");
        chill.addSong(s3);
        chill.addSong(s4);
        u1.addPlaylist(chill);

        // Play songs concurrently
        player.playSong(u1, s1);
        player.playSong(u2, s2);

        Thread.sleep(3000);

        // Stop one song
        player.stopSong(u2);

        // Recommend songs
        List<Song> recs = recommender.recommendSongs(u1);
        System.out.println("\nRecommended for " + u1.getName() + ":");
        for (Song s : recs) System.out.println(" - " + s.getTitle());

        player.shutdown();
        authService.logout(u1);
        authService.logout(u2);
    }
}

