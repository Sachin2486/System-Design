import java.util.*;

// ------------------ Core Entities ------------------
class User {
	private final int id;
	private String username;
	private String email;
	private int reputation;

	public User(int id,String username,String email) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.reputation = 0;
	}

	public int getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public int getReputation() {
		return reputation;
	}

	public void increaseReputation(int points) {
		reputation += points;
	}

	@Override
	public String toString() {
		return "User{" + "id=" + id + ", username='" + username + '\'' +
		       ", email='" + email + '\'' + ", reputation=" + reputation + '}';
	}
}

class Comment {
	private final int id;
	private final String content;
	private final User author;
	private final Date creationDate;

	public Comment(int id, String content, User author) {
		this.id = id;
		this.content = content;
		this.author = author;
		this.creationDate = new Date();
	}

	@Override
	public String toString() {
		return "Comment{" + "id=" + id + ", content='" + content + '\'' +
		       ", author=" + author.getUsername() + ", date=" + creationDate + '}';
	}
}

class Tag {
	private final int id;
	private final String  name;

	public Tag(int id,String name) {
		this.id = id;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "#" + name;
	}
}

enum VoteType { UPVOTE, DOWNVOTE }

class Vote {
    private final User voter;
    private final VoteType type;
    
    public Vote(User voter, VoteType type) {
        this.voter = voter;
        this.type = type;
    }
    
    public VoteType getType() {
        return type;
    }
}

// ------------------ Question & Answer ------------------
class Answer {
    private final int id;
    private final String content;
    private final User author;
    private final Question question;
    private final Date creationDate;
    private final List<Comment> comments = new ArrayList<>();
    private final List<Vote> votes = new ArrayList<>();

    public Answer(int id, String content, User author, Question question) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.question = question;
        this.creationDate = new Date();
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void addVote(Vote vote) {
        votes.add(vote);
        if (vote.getType() == VoteType.UPVOTE) {
            author.increaseReputation(10);
        } else {
            author.increaseReputation(-2);
        }
    }

    @Override
    public String toString() {
        return "Answer{" + "id=" + id + ", content='" + content + '\'' +
                ", author=" + author.getUsername() + ", votes=" + votes.size() +
                ", comments=" + comments.size() + '}';
    }
}

class Question {
    private final int id;
    private final String title;
    private final String content;
    private final User author;
    private final Date creationDate;
    private final List<Answer> answers = new ArrayList<>();
    private final List<Comment> comments = new ArrayList<>();
    private final List<Tag> tags = new ArrayList<>();
    private final List<Vote> votes = new ArrayList<>();

    public Question(int id, String title, String content, User author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.creationDate = new Date();
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void addVote(Vote vote) {
        votes.add(vote);
        if (vote.getType() == VoteType.UPVOTE) {
            author.increaseReputation(5);
        } else {
            author.increaseReputation(-1);
        }
    }

    public boolean hasTag(String tagName) {
        return tags.stream().anyMatch(t -> t.getName().equalsIgnoreCase(tagName));
    }

    public boolean containsKeyword(String keyword) {
        return title.toLowerCase().contains(keyword.toLowerCase()) ||
                content.toLowerCase().contains(keyword.toLowerCase());
    }

    public User getAuthor() { return author; }
    public String getTitle() { return title; }

    @Override
    public String toString() {
        return "Question{" + "id=" + id + ", title='" + title + '\'' +
                ", author=" + author.getUsername() + ", tags=" + tags +
                ", votes=" + votes.size() + ", answers=" + answers.size() + '}';
    }
}

// ------------------ Main System ------------------

class StackOverflow {
    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Question> questions = new HashMap<>();
    private final Map<Integer, Answer> answers = new HashMap<>();
    private final Map<Integer, Tag> tags = new HashMap<>();

    private int userIdCounter = 1;
    private int questionIdCounter = 1;
    private int answerIdCounter = 1;
    private int commentIdCounter = 1;
    private int tagIdCounter = 1;

    // User
    public User createUser(String username, String email) {
        User user = new User(userIdCounter++, username, email);
        users.put(user.getId(), user);
        return user;
    }

    // Question
    public Question postQuestion(User author, String title, String content, List<String> tagNames) {
        Question q = new Question(questionIdCounter++, title, content, author);
        for (String tagName : tagNames) {
            Tag tag = tags.values().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(tagName))
                    .findFirst()
                    .orElseGet(() -> {
                        Tag newTag = new Tag(tagIdCounter++, tagName);
                        tags.put(newTag.hashCode(), newTag);
                        return newTag;
                    });
            q.addTag(tag);
        }
        questions.put(q.hashCode(), q);
        return q;
    }

    // Answer
    public Answer postAnswer(User author, Question question, String content) {
        Answer a = new Answer(answerIdCounter++, content, author, question);
        question.addAnswer(a);
        answers.put(a.hashCode(), a);
        return a;
    }

    // Comment
    public Comment addCommentToQuestion(User author, Question question, String content) {
        Comment c = new Comment(commentIdCounter++, content, author);
        question.addComment(c);
        return c;
    }

    public Comment addCommentToAnswer(User author, Answer answer, String content) {
        Comment c = new Comment(commentIdCounter++, content, author);
        answer.addComment(c);
        return c;
    }

    // Voting
    public void voteQuestion(User voter, Question question, VoteType type) {
        question.addVote(new Vote(voter, type));
    }

    public void voteAnswer(User voter, Answer answer, VoteType type) {
        answer.addVote(new Vote(voter, type));
    }

    // Searching
    public List<Question> searchByKeyword(String keyword) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions.values()) {
            if (q.containsKeyword(keyword)) result.add(q);
        }
        return result;
    }

    public List<Question> searchByTag(String tagName) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions.values()) {
            if (q.hasTag(tagName)) result.add(q);
        }
        return result;
    }

    public List<Question> searchByUser(User user) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions.values()) {
            if (q.getAuthor().equals(user)) result.add(q);
        }
        return result;
    }
}

// ------------------ Demo ------------------

public class StackOverflowDemo {
    public static void main(String[] args) {
        StackOverflow so = new StackOverflow();

        User alice = so.createUser("Alice", "alice@mail.com");
        User bob = so.createUser("Bob", "bob@mail.com");

        Question q1 = so.postQuestion(alice, "What is polymorphism in Java?",
                "Can someone explain with example?", Arrays.asList("Java", "OOP"));

        Answer a1 = so.postAnswer(bob, q1, "Polymorphism allows objects to take many forms...");
        so.voteQuestion(bob, q1, VoteType.UPVOTE);
        so.voteAnswer(alice, a1, VoteType.UPVOTE);

        so.addCommentToQuestion(bob, q1, "Good question!");
        so.addCommentToAnswer(alice, a1, "Nice explanation!");

        System.out.println("Search by keyword 'Java': " + so.searchByKeyword("Java"));
        System.out.println("Search by tag 'OOP': " + so.searchByTag("OOP"));
        System.out.println("Questions by Alice: " + so.searchByUser(alice));

        System.out.println("Alice reputation: " + alice.getReputation());
        System.out.println("Bob reputation: " + bob.getReputation());
    }
}