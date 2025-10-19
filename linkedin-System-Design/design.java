import java.util.*;
import java.util.concurrent.*;

// -------------------- ENUMS --------------------
enum RequestStatus { PENDING, ACCEPTED, REJECTED; }

// -------------------- PROFILE --------------------
class Profile {
    private String name;
    private String email;
    private String headline;
    private String summary;
    private List<String> experience;
    private List<String> education;
    private List<String> skills;
    private String profilePicture;

    public Profile(String name, String email) {
        this.name = name;
        this.email = email;
        this.experience = new ArrayList<>();
        this.education = new ArrayList<>();
        this.skills = new ArrayList<>();
    }

    public void updateHeadline(String headline) { this.headline = headline; }
    public void addExperience(String exp) { experience.add(exp); }
    public void addEducation(String edu) { education.add(edu); }
    public void addSkill(String skill) { skills.add(skill); }

    @Override
    public String toString() {
        return name + " (" + headline + ") | " + String.join(", ", skills);
    }
}

// -------------------- USER --------------------
class User {
    protected String username;
    protected String password;
    protected Profile profile;
    protected List<User> connections;
    protected List<Message> inbox;
    protected List<Message> sent;

    public User(String username, String password, Profile profile) {
        this.username = username;
        this.password = password;
        this.profile = profile;
        this.connections = new ArrayList<>();
        this.inbox = new ArrayList<>();
        this.sent = new ArrayList<>();
    }

    public String getUsername() { return username; }
    public Profile getProfile() { return profile; }

    public void addConnection(User user) { connections.add(user); }
    public List<User> getConnections() { return connections; }

    public void sendMessage(User receiver, String content) {
        Message msg = new Message(this, receiver, content);
        sent.add(msg);
        receiver.receiveMessage(msg);
    }

    private void receiveMessage(Message msg) {
        inbox.add(msg);
        NotificationCenter.getInstance().notifyUser(this, "📨 New message from " + msg.getSender().getUsername());
    }

    public void viewInbox() {
        System.out.println("\n📥 Inbox of " + username);
        for (Message msg : inbox) System.out.println(msg);
    }
}

// -------------------- EMPLOYER --------------------
class Employer extends User {
    private List<JobPost> jobPosts;

    public Employer(String username, String password, Profile profile) {
        super(username, password, profile);
        this.jobPosts = new ArrayList<>();
    }

    public void postJob(String title, String desc, String req, String loc) {
        JobPost job = new JobPost(title, desc, req, loc, this);
        jobPosts.add(job);
        NetworkPlatform.getInstance().addJob(job);
        NotificationCenter.getInstance().broadcast("💼 New job posted: " + title);
    }

    public List<JobPost> getJobPosts() { return jobPosts; }
}

// -------------------- CONNECTION REQUEST --------------------
class ConnectionRequest {
    private User sender;
    private User receiver;
    private RequestStatus status;

    public ConnectionRequest(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = RequestStatus.PENDING;
    }

    public void accept() {
        sender.addConnection(receiver);
        receiver.addConnection(sender);
        status = RequestStatus.ACCEPTED;
        NotificationCenter.getInstance().notifyUser(sender, "✅ " + receiver.getUsername() + "accepted your connection.");
        NotificationCenter.getInstance().notifyUser(receiver, "🤝 You are now connected with " + sender.getUsername());
    }

    public void reject() {
        status = RequestStatus.REJECTED;
    }
}

// -------------------- MESSAGE --------------------
class Message {
    private User sender;
    private User receiver;
    private String content;
    private Date timestamp;

    public Message(User sender, User receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = new Date();
    }

    public User getSender() { return sender; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender.getUsername() + ": " + content;
    }
}

// -------------------- JOB POST --------------------
class JobPost {
    private String title;
    private String description;
    private String requirements;
    private String location;
    private Employer employer;

    public JobPost(String title, String desc, String req, String loc, Employer emp) {
        this.title = title;
        this.description = desc;
        this.requirements = req;
        this.location = loc;
        this.employer = emp;
    }

    public String getTitle() { return title; }

    @Override
    public String toString() {
        return "💼 " + title + " at " + employer.getUsername() + " (" + location + ")";
    }
}

// -------------------- NOTIFICATION --------------------
class Notification {
    private String content;
    private Date timestamp;

    public Notification(String content) {
        this.content = content;
        this.timestamp = new Date();
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + content;
    }
}

// -------------------- OBSERVER: Notification Center --------------------
class NotificationCenter {
    private static NotificationCenter instance;
    private ConcurrentMap<User, List<Notification>> userNotifications;

    private NotificationCenter() {
        userNotifications = new ConcurrentHashMap<>();
    }

    public static synchronized NotificationCenter getInstance() {
        if (instance == null) instance = new NotificationCenter();
        return instance;
    }

    public void notifyUser(User user, String message) {
        userNotifications.putIfAbsent(user, new ArrayList<>());
        userNotifications.get(user).add(new Notification(message));
    }

    public void broadcast(String message) {
        for (User user : userNotifications.keySet()) {
            notifyUser(user, message);
        }
    }

    public void viewNotifications(User user) {
        System.out.println("\n🔔 Notifications for " + user.getUsername() + ":");
        List<Notification> list = userNotifications.getOrDefault(user, new ArrayList<>());
        for (Notification n : list) System.out.println(n);
    }
}

// -------------------- PLATFORM (Singleton) --------------------
class NetworkPlatform {
    private static NetworkPlatform instance;
    private Map<String, User> users;
    private List<JobPost> jobs;

    private NetworkPlatform() {
        users = new ConcurrentHashMap<>();
        jobs = new CopyOnWriteArrayList<>();
    }

    public static synchronized NetworkPlatform getInstance() {
        if (instance == null) instance = new NetworkPlatform();
        return instance;
    }

    public User register(String username, String password, Profile profile, boolean isEmployer) {
        User user = isEmployer ? new Employer(username, password, profile) : new User(username, password, profile);
        users.put(username, user);
        NotificationCenter.getInstance().notifyUser(user, "🎉 Welcome to ProConnect, " + username + "!");
        return user;
    }

    public User login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.password.equals(password)) {
            System.out.println("✅ " + username + " logged in successfully!");
            return user;
        } else {
            System.out.println("❌ Invalid credentials.");
            return null;
        }
    }

    public void addJob(JobPost job) { jobs.add(job); }

    public void showAllJobs() {
        System.out.println("\n💼 All Job Listings:");
        for (JobPost job : jobs) System.out.println(job);
    }
}

// -------------------- MAIN CLASS --------------------
public class Linkedin {
    public static void main(String[] args) {
        NetworkPlatform platform = NetworkPlatform.getInstance();

        Profile p1 = new Profile("Alice", "alice@email.com");
        p1.updateHeadline("Software Engineer");
        p1.addSkill("Java");
        p1.addSkill("Spring Boot");

        Profile p2 = new Profile("Bob", "bob@company.com");
        p2.updateHeadline("Hiring Manager");

        User alice = platform.register("alice", "pass123", p1, false);
        Employer bob = (Employer) platform.register("bob", "admin123", p2, true);

        platform.login("alice", "pass123");
        platform.login("bob", "admin123");

        bob.postJob("Backend Developer", "Work on scalable APIs", "Java, Spring", "Remote");
        platform.showAllJobs();

        ConnectionRequest req = new ConnectionRequest(alice, bob);
        req.accept();

        alice.sendMessage(bob, "Hi Bob, I’m interested in the backend role!");
        alice.viewInbox();
        NotificationCenter.getInstance().viewNotifications(alice);
    }
}
