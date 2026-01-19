import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class User {
    long id;
    String name;
    Set<Long> following = new HashSet<>();
    List<Post> posts = new ArrayList<>();
    
    User(long id, String name){
        this.id = id;
        this.name = name;
    }
}

class Post {
     long id;
     long authorId;
     String content;
     Instant createdAt;
     Set<Long> likes = new HashSet<>();
     List<Comment> comments = new ArrayList<>();

    Post(long id, long authorId, String content) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = Instant.now();
    }
    
    void like(long userId){
        likes.add(userId);
    }
    
    void comment(Comment comment) {
        comments.add(comment);
    }
}

class Comment {
    long id;
    long userId;
    String text;
    Instant createdAt;
    
    Comment(long id,long userId, String text) {
        this.id = id;
        this.userId = userId;
        this.text = text;
        this.createdAt = Instant.now();
    }
}

class SocialMediaService {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, Post> posts = new ConcurrentHashMap<>();

    private final AtomicLong userIdGen = new AtomicLong(1000);
    private final AtomicLong postIdGen = new AtomicLong(2000);
    private final AtomicLong commentIdGen = new AtomicLong(3000);
    
    User createUser(String name) {
        long id = userIdGen.getAndIncrement();
        User user = new User(id,name);
        users.put(id, user);
        return user;
    }
    
    void follow(long followerId, long followeeId) {
        User follower = users.get(followerId);
        if(follower != null) {
            follower.following.add(followeeId);
        }
    }
    
    Post createPost(long userId, String content) {
        User user = users.get(userId);
        if (user == null) throw new RuntimeException("User not found");
        
        long postId = postIdGen.getAndIncrement();
        Post post = new Post(postId, userId, content);
        user.posts.add(post);
        posts.put(postId, post);
        return post;
    }
    
    void likePost(long userId, long postId) {
        Post post = posts.get(postId);
        if (post != null) {
            post.like(userId);
        }
    }

    void commentOnPost(long userId, long postId, String text) {
        Post post = posts.get(postId);
        if (post != null) {
            post.comment(new Comment(
                    commentIdGen.getAndIncrement(),
                    userId,
                    text
            ));
        }
    }

    /* -------- Feed -------- */
    List<Post> getFeed(long userId) {
        User user = users.get(userId);
        if (user == null) return Collections.emptyList();

        List<Post> feed = new ArrayList<>();
        for (Long followedUserId : user.following) {
            User followed = users.get(followedUserId);
            if (followed != null) {
                feed.addAll(followed.posts);
            }
        }

        // Latest posts first
        feed.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        return feed;
    }
}

public class Main
{
    public static void main(String[] args) {
        SocialMediaService service = new SocialMediaService();

        User alice = service.createUser("Alice");
        User bob = service.createUser("Bob");
        User charlie = service.createUser("Charlie");

        service.follow(alice.id, bob.id);
        service.follow(alice.id, charlie.id);

        Post p1 = service.createPost(bob.id, "Hello from Bob!");
        Post p2 = service.createPost(charlie.id, "Charlie here!");
        Post p3 = service.createPost(bob.id, "Another post by Bob");

        service.likePost(alice.id, p1.id);
        service.commentOnPost(alice.id, p1.id, "Nice post!");

        List<Post> feed = service.getFeed(alice.id);

        System.out.println("Alice's Feed:");
        for (Post post : feed) {
            System.out.println(
                    post.content +
                    " | Likes: " + post.likes.size() +
                    " | Comments: " + post.comments.size()
            );
        }
    }
}
