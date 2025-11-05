import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// ---------------------- ENUMS ----------------------
enum VoteType {
    UPVOTE, DOWNVOTE
}

// ---------------------- TAG ----------------------
class Tag {
    private final String name;

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

// ---------------------- USER ----------------------
class User {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private final int id;
    private final String username;
    private int reputation;

    public User(String username) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.username = username;
        this.reputation = 0;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getReputation() { return reputation; }

    public void updateReputation(int delta) {
        this.reputation += delta;
    }

    @Override
    public String toString() {
        return username + " (Reputation: " + reputation + ")";
    }
}

// ---------------------- COMMENT ----------------------
class Comment {
    private final User author;
    private final String content;
    private final Date createdAt;

    public Comment(User author, String content) {
        this.author = author;
        this.content = content;
        this.createdAt = new Date();
    }

    public String getContent() { return content; }
    public User getAuthor() { return author; }

    @Override
    public String toString() {
        return author.getUsername() + ": " + content;
    }
}

// ---------------------- ANSWER ----------------------
class Answer {
    private final int id;
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private final User author;
    private final String content;
    private int votes;
    private final List<Comment> comments;

    public Answer(User author, String content) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.author = author;
        this.content = content;
        this.votes = 0;
        this.comments = new ArrayList<>();
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void vote(VoteType type) {
        if (type == VoteType.UPVOTE) votes++;
        else votes--;
    }

    public int getVotes() { return votes; }
    public User getAuthor() { return author; }

    @Override
    public String toString() {
        return "Answer by " + author.getUsername() + " (" + votes + " votes): " + content;
    }
}

// ---------------------- QUESTION ----------------------
class Question {
    private final int id;
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private final User author;
    private final String title;
    private final String content;
    private final List<Tag> tags;
    private final List<Answer> answers;
    private final List<Comment> comments;
    private int votes;

    public Question(User author, String title, String content, List<Tag> tags) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.author = author;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.answers = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.votes = 0;
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void vote(VoteType type) {
        if (type == VoteType.UPVOTE) votes++;
        else votes--;
    }

    public List<Tag> getTags() { return tags; }
    public User getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getVotes() { return votes; }

    @Override
    public String toString() {
        return "[" + title + "] by " + author.getUsername() + " (" + votes + " votes)\n" + content;
    }
}

// ---------------------- REPUTATION MANAGER ----------------------
class ReputationManager {
    public static void handleVote(User user, VoteType type, boolean isQuestion) {
        int delta = 0;
        if (type == VoteType.UPVOTE) delta = isQuestion ? 10 : 5;
        else delta = -2;
        user.updateReputation(delta);
    }
}

// ---------------------- SEARCH SERVICE ----------------------
class SearchService {
    public List<Question> searchByKeyword(List<Question> questions, String keyword) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions) {
            if (q.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                q.getContent().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(q);
            }
        }
        return result;
    }

    public List<Question> searchByTag(List<Question> questions, String tagName) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions) {
            for (Tag t : q.getTags()) {
                if (t.getName().equalsIgnoreCase(tagName)) {
                    result.add(q);
                    break;
                }
            }
        }
        return result;
    }

    public List<Question> searchByUser(List<Question> questions, String username) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions) {
            if (q.getAuthor().getUsername().equalsIgnoreCase(username)) {
                result.add(q);
            }
        }
        return result;
    }
}

// ---------------------- MAIN QA SYSTEM ----------------------
class QASystem {
    private final List<User> users = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final SearchService searchService = new SearchService();

    public User registerUser(String username) {
        User user = new User(username);
        users.add(user);
        return user;
    }

    public Question postQuestion(User user, String title, String content, List<Tag> tags) {
        Question q = new Question(user, title, content, tags);
        questions.add(q);
        return q;
    }

    public Answer postAnswer(User user, Question question, String content) {
        Answer a = new Answer(user, content);
        question.addAnswer(a);
        return a;
    }

    public void voteQuestion(Question question, VoteType type) {
        question.vote(type);
        ReputationManager.handleVote(question.getAuthor(), type, true);
    }

    public void voteAnswer(Answer answer, VoteType type) {
        answer.vote(type);
        ReputationManager.handleVote(answer.getAuthor(), type, false);
    }

    public List<Question> searchByKeyword(String keyword) {
        return searchService.searchByKeyword(questions, keyword);
    }

    public List<Question> searchByTag(String tag) {
        return searchService.searchByTag(questions, tag);
    }

    public List<Question> searchByUser(String username) {
        return searchService.searchByUser(questions, username);
    }

    public void printAllQuestions() {
        for (Question q : questions) System.out.println(q);
    }
}

// ---------------------- DEMO ----------------------
public class QADemo {
    public static void main(String[] args) {
        QASystem qa = new QASystem();

        User alice = qa.registerUser("Alice");
        User bob = qa.registerUser("Bob");

        Question q1 = qa.postQuestion(alice, "What is Java?", "Can someone explain Java in simple terms?",
                Arrays.asList(new Tag("programming"), new Tag("java")));
        Question q2 = qa.postQuestion(bob, "Difference between ArrayList and LinkedList?",
                "Which one should I use when?", Arrays.asList(new Tag("collections"), new Tag("java")));

        Answer a1 = qa.postAnswer(bob, q1, "Java is a high-level OOP language.");
        Answer a2 = qa.postAnswer(alice, q2, "ArrayList is backed by an array; LinkedList by nodes.");

        qa.voteQuestion(q1, VoteType.UPVOTE);
        qa.voteAnswer(a1, VoteType.UPVOTE);
        qa.voteAnswer(a2, VoteType.DOWNVOTE);

        System.out.println("\n--- All Questions ---");
        qa.printAllQuestions();

        System.out.println("\n--- Search by Tag 'java' ---");
        for (Question q : qa.searchByTag("java")) System.out.println(q);

        System.out.println("\n--- User Reputation ---");
        System.out.println(alice);
        System.out.println(bob);
    }
}
