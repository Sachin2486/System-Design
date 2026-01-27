import java.util.ArrayList;
import java.util.List;

// Main Application Class
public class ScorecardManagementSystem {

    public static void main(String[] args) {
        // Create Users
        User admin = new User(1L, "Admin User", "admin@example.com", Role.ADMIN);
        User evaluator = new User(2L, "Evaluator User", "evaluator@example.com", Role.EVALUATOR);
        User regularUser = new User(3L, "Regular User", "user@example.com", Role.USER);

        // Create Scorecard
        Scorecard scorecard = new Scorecard(1L, "Employee Evaluation", ScorecardStatus.DRAFT, admin);
        Section section1 = new Section(1L, "Technical Skills");
        Section section2 = new Section(2L, "Communication Skills");

        // Add sections to scorecard
        scorecard.addSection(section1);
        scorecard.addSection(section2);

        // Display the scorecard details
        System.out.println("Scorecard Title: " + scorecard.getTitle());
        System.out.println("Created by: " + scorecard.getCreatedBy().getName());
        System.out.println("Sections in the scorecard:");
        for (Section section : scorecard.getSections()) {
            System.out.println("- " + section.getTitle());
        }
    }
}

// User Class
class User {
    private Long id;
    private String name;
    private String email;
    private Role role;

    public User(Long id, String name, String email, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}

// Enum for User Roles
enum Role {
    ADMIN, EVALUATOR, USER
}

// Scorecard Class
class Scorecard {
    private Long id;
    private String title;
    private ScorecardStatus status;
    private User createdBy;
    private List<Section> sections = new ArrayList<>();

    public Scorecard(Long id, String title, ScorecardStatus status, User createdBy) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ScorecardStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void addSection(Section section) {
        sections.add(section);
    }
}

// Enum for Scorecard Status
enum ScorecardStatus {
    DRAFT, SUBMITTED, FINALIZED
}

// Section Class
class Section {
    private Long id;
    private String title;

    public Section(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
