import java.util.*;

// Enum for user roles
enum Role {
    ADMIN, DEVELOPER, CONTENT_CREATOR
}

// Class representing a User
class User {
    private UUID id;
    private String name;
    private Role role;

    public User(String name, Role role) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', role=" + role + "}";
    }
}

// Class representing a Tag
class Tag {
    private UUID id;
    private String name;
    private String description;
    private String status;  // Active or Inactive
    private Tag parentTag;

    public Tag(String name, String description, Tag parentTag) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.status = "Active";  // Tags are active by default
        this.parentTag = parentTag;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Tag getParentTag() {
        return parentTag;
    }

    @Override
    public String toString() {
        return "Tag{id=" + id + ", name='" + name + "', description='" + description + "', status=" + status + "}";
    }
}

// Class representing a TagAssociation with entities like JIRA issues or Confluence pages
class TagAssociation {
    private UUID id;
    private Tag tag;
    private String entityType;  // JIRA or Confluence
    private UUID entityId;

    public TagAssociation(Tag tag, String entityType, UUID entityId) {
        this.id = UUID.randomUUID();
        this.tag = tag;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return "TagAssociation{id=" + id + ", tag=" + tag.getName() + ", entityType='" + entityType + "', entityId=" + entityId + "}";
    }
}

// Tag Management System
public class TagManagementSystem {

    private Map<UUID, User> users = new HashMap<>();
    private Map<UUID, Tag> tags = new HashMap<>();
    private List<TagAssociation> tagAssociations = new ArrayList<>();

    // Create a new User
    public User createUser(String name, Role role) {
        User user = new User(name, role);
        users.put(user.getId(), user);
        System.out.println("User created: " + user);
        return user;
    }

    // Create a new Tag (Only Admins can create tags)
    public Tag createTag(User user, String name, String description, Tag parentTag) {
        if (user.getRole() != Role.ADMIN) {
            System.out.println("Only Admins can create tags.");
            return null;
        }
        Tag tag = new Tag(name, description, parentTag);
        tags.put(tag.getId(), tag);
        System.out.println("Tag created: " + tag);
        return tag;
    }

    // Deactivate a tag (Only Admins can deactivate tags)
    public void deactivateTag(User user, Tag tag) {
        if (user.getRole() != Role.ADMIN) {
            System.out.println("Only Admins can deactivate tags.");
            return;
        }
        tag.setStatus("Inactive");
        System.out.println("Tag deactivated: " + tag);
    }

    // Associate a tag with an entity (JIRA or Confluence)
    public void associateTag(User user, Tag tag, String entityType, UUID entityId) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.DEVELOPER || user.getRole() == Role.CONTENT_CREATOR) {
            TagAssociation association = new TagAssociation(tag, entityType, entityId);
            tagAssociations.add(association);
            System.out.println("Tag associated with " + entityType + ": " + association);
        } else {
            System.out.println("User does not have permission to associate tags.");
        }
    }

    // Search Tags
    public void searchTags(String keyword) {
        System.out.println("Searching for tags with keyword: " + keyword);
        for (Tag tag : tags.values()) {
            if (tag.getName().contains(keyword) || tag.getDescription().contains(keyword)) {
                System.out.println(tag);
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        TagManagementSystem system = new TagManagementSystem();

        // Creating users
        User admin = system.createUser("Alice", Role.ADMIN);
        User developer = system.createUser("Bob", Role.DEVELOPER);
        User contentCreator = system.createUser("Carol", Role.CONTENT_CREATOR);

        // Creating tags (Only Admin can create)
        Tag rootTag = system.createTag(admin, "Project Management", "Tags for project management", null);
        Tag childTag = system.createTag(admin, "Agile", "Agile methodology tags", rootTag);

        // Associating tags with entities (JIRA/Confluence)
        UUID jiraIssueId = UUID.randomUUID();
        system.associateTag(developer, childTag, "JIRA", jiraIssueId);

        UUID confluencePageId = UUID.randomUUID();
        system.associateTag(contentCreator, rootTag, "Confluence", confluencePageId);

        // Deactivate a tag
        system.deactivateTag(admin, childTag);

        // Search tags
        system.searchTags("Agile");
    }
}
