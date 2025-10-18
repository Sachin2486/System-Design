import java.util.*;
import java.util.concurrent.*;

// -------------------- ENUMS --------------------
enum VoteType { UPVOTE, DOWNVOTE; }

// -------------------- USER --------------------
class User {
    private String username;
    private int reputation;
    private List<Question> questions;
    private List<Answer> answers;

    public User(String username) {
        this.username = username;
        this.reputation = 0;
        this.questions = new ArrayList<>();
        this.answers = new ArrayList<>();
    }

    public String getUsername() { return username; }
    public int getReputation() { return reputation; }

    public void increaseReputation(int points) { reputation += points; }
    public void decreaseReputation(int points) { reputation = Math.max(0, reputation - points); }

    public void addQuestion(Question q) { questions.add(q); }
    public void addAnswer(Answer a) { answers.add(a); }

    @Override
    public String toString() {
        return username + " (Reputation: " + reputation + ")";
    }
}

// -------------------- BASE POST --------------------
abstract class Post {
    protected String content;
    protected User author;
    protected int votes;
    protected List<Comment> comments;

    public Post(String content, User author) {
        this.content = content;
        this.author = author;
        this.votes = 0;
        this.comments = new ArrayList<>();
    }

    public synchronized void vote(VoteType type) {
        if (type == VoteType.UPVOTE) {
            votes++;
            author.increaseReputation(10);
        } else {
            votes--;
            author.decreaseReputation(2);
        }
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public String getContent() { return content; }
    public User getAuthor() { return author; }
    public int getVotes() { return votes; }
    public List<Comment> getComments() { return comments; }

    @Override
    public String toString() {
        return content + " (by " + author.getUsername() + ", Votes: " + votes + ")";
    }
}

// -------------------- QUESTION --------------------
class Question extends Post {
    private List<String> tags;
    private List<Answer> answers;

    public Question(String content, User author, List<String> tags) {
        super(content, author);
        this.tags = tags;
        this.answers = new ArrayList<>();
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public List<Answer> getAnswers() { return answers; }
    public List<String> getTags() { return tags; }
}

// -------------------- ANSWER --------------------
class Answer extends Post {
    private Question question;

    public Answer(String content, User author, Question question) {
        super(content, author);
        this.question = question;
    }

    public Question getQuestion() { return question; }
}

// -------------------- COMMENT --------------------
class Comment {
    private String content;
    private User author;

    public Comment(String content, User author) {
        this.content = content;
        this.author = author;
    }

    @Override
    public String toString() {
        return author.getUsername() + ": " + content;
    }
}

// -------------------- SEARCH STRATEGY INTERFACE --------------------
interface SearchStrategy {
    List<Question> search(List<Question> allQuestions, String query);
}

// Search by keyword
class KeywordSearch implements SearchStrategy {
    @Override
    public List<Question> search(List<Question> allQuestions, String keyword) {
        List<Question> result = new ArrayList<>();
        for (Question q : allQuestions) {
            if (q.getContent().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(q);
            }
        }
        return result;
    }
}

// Search by tag
class TagSearch implements SearchStrategy {
    @Override
    public List<Question> search(List<Question> allQuestions, String tag) {
        List<Question> result = new ArrayList<>();
        for (Question q : allQuestions) {
            if (q.getTags().contains(tag.toLowerCase())) {
                result.add(q);
            }
        }
        return result;
    }
}

// Search by username
class UserSearch implements SearchStrategy {
    @Override
    public List<Question> search(List<Question> allQuestions, String username) {
        List<Question> result = new ArrayList<>();
        for (Question q : allQuestions) {
            if (q.getAuthor().getUsername().equalsIgnoreCase(username)) {
                result.add(q);
            }
        }
        return result;
    }
}

// -------------------- Q&A PLATFORM (Singleton) --------------------
class QnAPlatform {
    private static QnAPlatform instance;
    private List<Question> allQuestions;
    private Map<String, User> users;
    private ExecutorService executor;

    private QnAPlatform() {
        this.allQuestions = Collections.synchronizedList(new ArrayList<>());
        this.users = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(5);
    }

    public static synchronized QnAPlatform getInstance() {
        if (instance == null) instance = new QnAPlatform();
        return instance;
    }

    public User registerUser(String username) {
        User user = new User(username);
        users.put(username, user);
        return user;
    }

    public Question postQuestion(User user, String content, List<String> tags) {
        Question q = new Question(content, user, tags);
        allQuestions.add(q);
        user.addQuestion(q);
        System.out.println("🟢 " + user.getUsername() + " posted: " + q.getContent());
        return q;
    }

    public void postAnswer(User user, Question question, String content) {
        Answer a = new Answer(content, user, question);
        question.addAnswer(a);
        user.addAnswer(a);
        System.out.println("🟠 " + user.getUsername() + " answered: " + content);
    }

    public List<Question> searchQuestions(SearchStrategy strategy, String query) {
        return strategy.search(allQuestions, query);
    }

    public void vote(Post post, VoteType type) {
        executor.execute(() -> post.vote(type)); // simulate concurrent votes
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void showAllQuestions() {
        System.out.println("\n📋 All Questions:");
        for (Question q : allQuestions) {
            System.out.println("- " + q);
        }
    }
}

// -------------------- MAIN CLASS --------------------
public class QnAApp {
    public static void main(String[] args) throws InterruptedException {
        QnAPlatform platform = QnAPlatform.getInstance();

        User alice = platform.registerUser("Alice");
        User bob = platform.registerUser("Bob");
        User charlie = platform.registerUser("Charlie");

        Question q1 = platform.postQuestion(alice, "How to implement Singleton in Java?", Arrays.asList("java", "design-patterns"));
        Question q2 = platform.postQuestion(bob, "What is multithreading?", Arrays.asList("java", "threads"));

        platform.postAnswer(charlie, q1, "Use a private constructor and static getInstance() method.");
        platform.vote(q1, VoteType.UPVOTE);
        platform.vote(q1, VoteType.UPVOTE);
        platform.vote(q1, VoteType.DOWNVOTE);

        Thread.sleep(500); // allow threads to finish
        platform.showAllQuestions();

        // Search examples
        System.out.println("\n🔍 Search by tag 'java':");
        List<Question> javaQs = platform.searchQuestions(new TagSearch(), "java");
        javaQs.forEach(q -> System.out.println("→ " + q.getContent()));

        System.out.println("\n👤 User reputations:");
        System.out.println(alice);
        System.out.println(bob);
        System.out.println(charlie);

        platform.shutdown();
    }
}
