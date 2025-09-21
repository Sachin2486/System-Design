import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// --- Domain classes ---
enum Role { USER, ADMIN }

class User {
    private final String id;
    private final String username;
    private final Role role;
    // simple listening history (song id -> play count)
    private final Map<String, Integer> listeningHistory = new ConcurrentHashMap<>();

    public User(String id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
    public String getId() { return id; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public Map<String, Integer> getListeningHistory() { return listeningHistory; }

    public void recordPlay(String songId) {
        listeningHistory.merge(songId, 1, Integer::sum);
    }
    public String toString() { return username + "(" + id + ")"; }
}

class Artist {
    final String id;
    final String name;
    Artist(String id, String name) { this.id = id; this.name = name; }
}

class Album {
    final String id;
    final String title;
    final String artistId;
    Album(String id, String title, String artistId) { this.id = id; this.title = title; this.artistId = artistId; }
}

class Song {
    final String id;
    final String title;
    final String albumId;
    final String artistId;
    final int durationSeconds; // for seek simulation
    // metadata for recommendation
    final AtomicInteger globalPlayCount = new AtomicInteger(0);

    Song(String id, String title, String albumId, String artistId, int durationSeconds) {
        this.id = id; this.title = title; this.albumId = albumId; this.artistId = artistId; this.durationSeconds = durationSeconds;
    }
}

// Playlist
class Playlist {
    final String id;
    final String ownerId;
    final String name;
    final List<String> songIds = Collections.synchronizedList(new ArrayList<>());

    Playlist(String id, String ownerId, String name) { this.id = id; this.ownerId = ownerId; this.name = name; }
}

// Playback session (represents a user playing a song)
class PlaybackSession {
    final String sessionId;
    final String userId;
    volatile String songId;
    volatile int currentSecond;
    volatile boolean playing = false;
    final Object lock = new Object();

    PlaybackSession(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.songId = null;
        this.currentSecond = 0;
    }
}

// --- Services ---

// Authentication (very basic)
class AuthService {
    // in-memory credentials for demo: username -> password
    private final Map<String, String> creds = new ConcurrentHashMap<>();
    private final Map<String, User> tokens = new ConcurrentHashMap<>(); // token -> user
    private final Map<String, User> users = new ConcurrentHashMap<>(); // username -> user

    public AuthService() {
        // bootstrap
    }
    public void registerUser(String username, String password, Role role) {
        String id = UUID.randomUUID().toString();
        User u = new User(id, username, role);
        creds.put(username, password);
        users.put(username, u);
    }
    public Optional<String> login(String username, String password) {
        String p = creds.get(username);
        if (p != null && p.equals(password)) {
            String token = UUID.randomUUID().toString();
            tokens.put(token, users.get(username));
            return Optional.of(token);
        }
        return Optional.empty();
    }
    public Optional<User> getUserByToken(String token) {
        return Optional.ofNullable(tokens.get(token));
    }
}

// Catalog service: search & browse
class CatalogService {
    private final Map<String, Song> songs = new ConcurrentHashMap<>();
    private final Map<String, Album> albums = new ConcurrentHashMap<>();
    private final Map<String, Artist> artists = new ConcurrentHashMap<>();

    public void addArtist(Artist a) { artists.put(a.id, a); }
    public void addAlbum(Album al) { albums.put(al.id, al); }
    public void addSong(Song s) { songs.put(s.id, s); }

    public List<Song> searchSongs(String q) {
        String lower = q.toLowerCase();
        List<Song> res = new ArrayList<>();
        for (Song s : songs.values()) {
            if (s.title.toLowerCase().contains(lower)) res.add(s);
        }
        return res;
    }
    public List<Album> searchAlbums(String q) {
        String lower = q.toLowerCase();
        List<Album> res = new ArrayList<>();
        for (Album a : albums.values()) {
            if (a.title.toLowerCase().contains(lower)) res.add(a);
        }
        return res;
    }
    public List<Artist> searchArtists(String q) {
        String lower = q.toLowerCase();
        List<Artist> res = new ArrayList<>();
        for (Artist a : artists.values()) {
            if (a.name.toLowerCase().contains(lower)) res.add(a);
        }
        return res;
    }
    public Optional<Song> getSong(String id) { return Optional.ofNullable(songs.get(id)); }
    public Collection<Song> listAllSongs() { return songs.values(); }
}

// PlaylistService
class PlaylistService {
    private final Map<String, Playlist> playlists = new ConcurrentHashMap<>();
    public Playlist createPlaylist(String ownerId, String name) {
        Playlist p = new Playlist(UUID.randomUUID().toString(), ownerId, name);
        playlists.put(p.id, p);
        return p;
    }
    public boolean addSongToPlaylist(String playlistId, String songId) {
        Playlist p = playlists.get(playlistId);
        if (p == null) return false;
        p.songIds.add(songId);
        return true;
    }
    public List<Playlist> getPlaylistsForUser(String userId) {
        List<Playlist> out = new ArrayList<>();
        for (Playlist p : playlists.values()) if (p.ownerId.equals(userId)) out.add(p);
        return out;
    }
}

// Recommendation: simple top-global + user-history based
class RecommendationService {
    private final CatalogService catalog;
    public RecommendationService(CatalogService catalog) { this.catalog = catalog; }

    public List<Song> recommendFor(User user, int limit) {
        // combine user's top listened songs + global top
        Map<String, Integer> hist = user.getListeningHistory();
        List<Song> topFromUser = new ArrayList<>();
        if (!hist.isEmpty()) {
            List<String> sortedByUser = new ArrayList<>(hist.keySet());
            sortedByUser.sort((a,b) -> hist.get(b) - hist.get(a));
            for (String id : sortedByUser) {
                catalog.getSong(id).ifPresent(topFromUser::add);
                if (topFromUser.size() >= limit/2) break;
            }
        }
        // global top
        List<Song> all = new ArrayList<>(catalog.listAllSongs());
        all.sort((a,b) -> b.globalPlayCount.get() - a.globalPlayCount.get());
        List<Song> result = new ArrayList<>(topFromUser);
        for (Song s : all) {
            if (result.size() >= limit) break;
            if (!result.contains(s)) result.add(s);
        }
        return result;
    }
}

// StreamingService simulates concurrent stream; in real world this is a CDN + chunked TCP/HTTP
class StreamingService {
    private final ExecutorService streamPool = Executors.newCachedThreadPool();
    // simple bandwidth limiter for demo (max concurrent streams)
    private final Semaphore concurrentStreams;
    public StreamingService(int maxConcurrent) {
        this.concurrentStreams = new Semaphore(maxConcurrent);
    }

    public void streamSong(User user, Song song, PlaybackSession session, Runnable onFinish) {
        if (!concurrentStreams.tryAcquire()) {
            System.out.println(user + " — service busy, please try later.");
            return;
        }
        streamPool.submit(() -> {
            try {
                System.out.println("START streaming " + song.title + " to " + user);
                session.songId = song.id;
                session.currentSecond = 0;
                session.playing = true;
                // simulate streaming by advancing seconds unless paused
                while (session.currentSecond < song.durationSeconds) {
                    synchronized(session.lock) {
                        if (!session.playing) {
                            session.lock.wait(); // wait until resumed
                        }
                    }
                    // simulate chunk playback 1 second per loop
                    Thread.sleep(200); // fast-forward simulation: 200ms per second of song
                    session.currentSecond++;
                    // optionally: print progress
                    if (session.currentSecond % 5 == 0 || session.currentSecond == song.durationSeconds)
                        System.out.println(user + " playing " + song.title + " [" + session.currentSecond + "/" + song.durationSeconds + "s]");
                }
                System.out.println("FINISHED streaming " + song.title + " to " + user);
                onFinish.run();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                concurrentStreams.release();
            }
        });
    }

    public void shutdown() {
        streamPool.shutdownNow();
    }
}

// PlayerService: handles play/pause/seek/skip per-user
class PlayerService {
    private final AuthService auth;
    private final CatalogService catalog;
    private final StreamingService streamingService;
    private final RecommendationService recService;
    // sessionId -> session
    private final Map<String, PlaybackSession> sessions = new ConcurrentHashMap<>();

    public PlayerService(AuthService auth, CatalogService catalog, StreamingService streamingService, RecommendationService recService) {
        this.auth = auth; this.catalog = catalog; this.streamingService = streamingService; this.recService = recService;
    }

    public PlaybackSession createSessionForUser(User user) {
        PlaybackSession s = new PlaybackSession(UUID.randomUUID().toString(), user.getId());
        sessions.put(s.sessionId, s);
        return s;
    }

    // start playing a song (creates or reuses a session)
    public void play(User user, PlaybackSession session, String songId) {
        catalog.getSong(songId).ifPresentOrElse(song -> {
            // increment counters
            song.globalPlayCount.incrementAndGet();
            user.recordPlay(songId);
            Runnable onFinish = () -> { /* post-play actions can be added here */ };
            streamingService.streamSong(user, song, session, onFinish);
        }, () -> System.out.println("Song not found: " + songId));
    }

    public void pause(PlaybackSession session) {
        synchronized(session.lock) {
            if (session.playing) {
                session.playing = false;
                System.out.println("Paused session " + session.sessionId + " at " + session.currentSecond + "s");
            }
        }
    }

    public void resume(PlaybackSession session) {
        synchronized(session.lock) {
            if (!session.playing) {
                session.playing = true;
                session.lock.notifyAll();
                System.out.println("Resumed session " + session.sessionId + " at " + session.currentSecond + "s");
            }
        }
    }

    public void seek(PlaybackSession session, int second) {
        synchronized(session.lock) {
            session.currentSecond = second;
            System.out.println("Seeked session " + session.sessionId + " to " + second + "s");
            if (!session.playing) {
                // don't auto-resume
            } else {
                session.lock.notifyAll();
            }
        }
    }
}

// --- Demo main: populate catalog, register users, simulate concurrent streaming ---
public class MusicStreamingDemo {
    public static void main(String[] args) throws Exception {
        // Services
        AuthService auth = new AuthService();
        CatalogService catalog = new CatalogService();
        PlaylistService playlists = new PlaylistService();
        RecommendationService recs = new RecommendationService(catalog);
        StreamingService streaming = new StreamingService(3); // allow 3 concurrent streams for demo
        PlayerService player = new PlayerService(auth, catalog, streaming, recs);

        // Bootstrap users
        auth.registerUser("alice", "pass", Role.USER);
        auth.registerUser("bob", "pass", Role.USER);
        String aliceToken = auth.login("alice", "pass").orElseThrow();
        String bobToken = auth.login("bob", "pass").orElseThrow();
        User alice = auth.getUserByToken(aliceToken).get();
        User bob = auth.getUserByToken(bobToken).get();

        // Bootstrap catalog
        Artist ar1 = new Artist("art1", "The Waves");
        catalog.addArtist(ar1);
        Album al1 = new Album("alb1", "Ocean Sounds", ar1.id);
        catalog.addAlbum(al1);
        Song s1 = new Song("s1", "Blue Dawn", al1.id, ar1.id, 20);
        Song s2 = new Song("s2", "High Tide", al1.id, ar1.id, 15);
        Song s3 = new Song("s3", "Moonlight", al1.id, ar1.id, 25);
        Song s4 = new Song("s4", "Drift", al1.id, ar1.id, 10);
        catalog.addSong(s1); catalog.addSong(s2); catalog.addSong(s3); catalog.addSong(s4);

        // Browse / search
        System.out.println("Search 'Blue': " + catalog.searchSongs("Blue"));

        // Playlist create & manage
        Playlist p = playlists.createPlaylist(alice.getId(), "Morning Vibes");
        playlists.addSongToPlaylist(p.id, "s1");
        playlists.addSongToPlaylist(p.id, "s3");
        System.out.println("Alice playlists: " + playlists.getPlaylistsForUser(alice.getId()).size());

        // Create playback sessions
        PlaybackSession sa = player.createSessionForUser(alice);
        PlaybackSession sb = player.createSessionForUser(bob);

        // Simulate concurrent play: Alice plays s1, Bob plays s2
        System.out.println("\n--- Simulate streaming ---");
        player.play(alice, sa, "s1");
        player.play(bob, sb, "s2");

        // After a bit, pause/resume/seek
        Thread.sleep(700); // wait a bit (simulation time)
        player.pause(sa);
        Thread.sleep(300);
        player.seek(sa, 10);
        player.resume(sa);

        // Play more concurrently (exceed concurrent limit to see limit behavior)
        User charlie = new User(UUID.randomUUID().toString(), "charlie", Role.USER);
        PlaybackSession sc = player.createSessionForUser(charlie);
        PlaybackSession sd = player.createSessionForUser(charlie); // same charlie multiple sessions
        player.play(charlie, sc, "s3");
        player.play(charlie, sd, "s4"); // may hit concurrent streaming limit

        // Let simulation run for a short time
        Thread.sleep(4000);

        // Recommendations for Alice
        List<Song> recForAlice = recs.recommendFor(alice, 5);
        System.out.println("\nRecommendations for Alice:");
        for (Song s : recForAlice) System.out.println("  - " + s.title + " (plays=" + s.globalPlayCount + ")");

        // Shutdown
        streaming.shutdown();
        System.out.println("Demo finished.");
    }
}
