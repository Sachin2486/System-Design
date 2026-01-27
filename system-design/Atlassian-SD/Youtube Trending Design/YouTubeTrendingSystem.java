import java.util.*;
import java.util.stream.Collectors;

class Video {
    private String id;
    private String title;
    private String description;
    private int viewCount;
    private List<String> tags;

    public Video(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.viewCount = 0;
        this.tags = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public List<String> getTags() {
        return tags;
    }

    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    @Override
    public String toString() {
        return "Video{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", viewCount=" + viewCount +
                ", tags=" + tags +
                '}';
    }
}

class Tag {
    private String name;
    private int usageCount;

    public Tag(String name) {
        this.name = name;
        this.usageCount = 0;
    }

    public String getName() {
        return name;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", usageCount=" + usageCount +
                '}';
    }
}

class VideoService {
    private List<Video> videos;
    private Map<String, Tag> tags;

    public VideoService() {
        videos = new ArrayList<>();
        tags = new HashMap<>();
    }

    public void addVideo(Video video) {
        videos.add(video);
    }

    public void addTagToVideo(String videoId, String tagName) {
        Video video = findVideoById(videoId);
        if (video != null) {
            video.addTag(tagName);
            tags.computeIfAbsent(tagName, Tag::new).incrementUsageCount();
        }
    }

    public void incrementViewCount(String videoId) {
        Video video = findVideoById(videoId);
        if (video != null) {
            video.incrementViewCount();
        }
    }

    public List<Video> getTrendingVideos(int limit) {
        return videos.stream()
                .sorted(Comparator.comparingInt(Video::getViewCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Video findVideoById(String videoId) {
        return videos.stream()
                .filter(video -> video.getId().equals(videoId))
                .findFirst()
                .orElse(null);
    }

    public void printTagsUsage() {
        System.out.println("Tags usage:");
        tags.values().forEach(System.out::println);
    }
}

public class YouTubeTrendingSystem {
    public static void main(String[] args) {
        VideoService videoService = new VideoService();

        // Adding videos
        Video video1 = new Video("1", "Learn Java", "A tutorial on Java programming.");
        Video video2 = new Video("2", "Spring Framework", "Understanding Spring Framework.");
        Video video3 = new Video("3", "Java Streams", "Exploring Streams in Java.");

        videoService.addVideo(video1);
        videoService.addVideo(video2);
        videoService.addVideo(video3);

        // Simulate views
        videoService.incrementViewCount("1");
        videoService.incrementViewCount("1");
        videoService.incrementViewCount("2");
        videoService.incrementViewCount("3");
        videoService.incrementViewCount("3");
        videoService.incrementViewCount("3");

        // Adding tags
        videoService.addTagToVideo("1", "Java");
        videoService.addTagToVideo("1", "Programming");
        videoService.addTagToVideo("2", "Spring");
        videoService.addTagToVideo("3", "Java");
        videoService.addTagToVideo("3", "Streams");

        // Get trending videos
        List<Video> trendingVideos = videoService.getTrendingVideos(2);
        System.out.println("Trending Videos:");
        trendingVideos.forEach(System.out::println);

        // Print tag usage
        videoService.printTagsUsage();
    }
}
