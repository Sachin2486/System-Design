import java.time.LocalDateTime;
import java.util.*;

/* ================= TWEET ================= */

class Tweet {

    String tweetId;
    String content;
    User author;
    LocalDateTime createdAt;

    Tweet(String content, User author) {
        this.tweetId = UUID.randomUUID().toString();
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return author.username + " : " + content;
    }
}

/* ================= USER ================= */

class User {

    String userId;
    String username;

    // followers of this user
    Set<User> followers = new HashSet<>();

    // users whom this user follows
    Set<User> following = new HashSet<>();

    // tweets posted by user
    List<Tweet> tweets = new ArrayList<>();

    User(String username) {
        this.userId = UUID.randomUUID().toString();
        this.username = username;
    }

    void follow(User user) {

        if (user == null || user == this) {
            return;
        }

        following.add(user);
        user.followers.add(this);
    }

    void unfollow(User user) {

        if (user == null || user == this) {
            return;
        }

        following.remove(user);
        user.followers.remove(this);
    }

    void addTweet(Tweet tweet) {
        tweets.add(tweet);
    }
}

/* ================= FEED SERVICE ================= */

class FeedService {

    private static final int FEED_LIMIT = 10;

    /*
        Generate latest feed using Max Heap

        Why PriorityQueue?
        -> Efficient latest tweet retrieval
        -> Tweets sorted by timestamp
    */

    List<Tweet> getNewsFeed(User user) {

        PriorityQueue<Tweet> maxHeap =
                new PriorityQueue<>(
                        (a, b) ->
                                b.createdAt.compareTo(a.createdAt)
                );

        // add own tweets
        maxHeap.addAll(user.tweets);

        // add following users tweets
        for (User followedUser : user.following) {
            maxHeap.addAll(followedUser.tweets);
        }

        List<Tweet> feed = new ArrayList<>();

        int count = 0;

        while (!maxHeap.isEmpty() && count < FEED_LIMIT) {
            feed.add(maxHeap.poll());
            count++;
        }

        return feed;
    }
}

/* ================= TWITTER SERVICE ================= */

class TwitterService {

    Map<String, User> users = new HashMap<>();

    FeedService feedService = new FeedService();

    // register user
    User registerUser(String username) {

        if (users.containsKey(username)) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User(username);

        users.put(username, user);

        return user;
    }

    // post tweet
    Tweet postTweet(User user, String content) {

        Tweet tweet = new Tweet(content, user);

        user.addTweet(tweet);

        return tweet;
    }

    // follow user
    void follow(User follower, User followee) {
        follower.follow(followee);
    }

    // unfollow user
    void unfollow(User follower, User followee) {
        follower.unfollow(followee);
    }

    // get feed
    List<Tweet> getFeed(User user) {
        return feedService.getNewsFeed(user);
    }
}

/* ================= DRIVER ================= */

public class Main {

    public static void main(String[] args) {

        TwitterService twitter = new TwitterService();

        // create users
        User sachin = twitter.registerUser("Sachin");
        User virat = twitter.registerUser("Virat");
        User rohit = twitter.registerUser("Rohit");

        // post tweets
        twitter.postTweet(virat, "Hello from Virat");
        twitter.postTweet(rohit, "Rohit tweet");
        twitter.postTweet(virat, "Excited for IPL");

        // follow users
        twitter.follow(sachin, virat);
        twitter.follow(sachin, rohit);

        // get feed
        List<Tweet> feed = twitter.getFeed(sachin);

        System.out.println("===== SACHIN FEED =====");

        for (Tweet tweet : feed) {
            System.out.println(tweet);
        }

        // unfollow
        twitter.unfollow(sachin, rohit);

        System.out.println("\n===== AFTER UNFOLLOW =====");

        feed = twitter.getFeed(sachin);

        for (Tweet tweet : feed) {
            System.out.println(tweet);
        }
    }
}