import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// ---------------------- ENTITY: Student ----------------------
class Student {
    private final String studentId;
    private final String name;
    private final Set<Course> registeredCourses;
    
    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
        this.registeredCourses = Collections.synchronizedSet(new HashSet<>());
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public String getName() {
        return name;
    }
    
    public Set<Course> getRegisteredCourses() {
        return registeredCourses;
    }
    
    public void addCourse(Course course) {
        registeredCourses.add(course);
    }
    
    public void viewRegisteredCourses() {
        System.out.println("Courses registered by " + name + ":");
        if (registeredCourses.isEmpty()) {
            System.out.println("No courses registered.");
        } else {
            for (Course c : registeredCourses) {
                System.out.println("- " + c.getCourseName() + " (" + c.getCourseCode() + ")");
            }
        }
    }
}

// ---------------------- ENTITY: Course ----------------------
class Course {
    private final String courseCode;
    private final String courseName;
    private final String instructor;
    private final int maxCapacity;
    private final Set<Student> enrolledStudents;
    private final ReentrantLock lock = new ReentrantLock();

    public Course(String courseCode, String courseName, String instructor, int maxCapacity) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.instructor = instructor;
        this.maxCapacity = maxCapacity;
        this.enrolledStudents = Collections.synchronizedSet(new HashSet<>());
    }

    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getInstructor() { return instructor; }
    public int getMaxCapacity() { return maxCapacity; }
    public Set<Student> getEnrolledStudents() { return enrolledStudents; }

    public boolean registerStudent(Student student) {
        lock.lock();
        try {
            if (enrolledStudents.size() >= maxCapacity) {
                System.out.println("Course: " + courseName + " is full. Registration failed for " + student.getName());
                return false;
            }
            if (enrolledStudents.add(student)) {
                System.out.println("student: " + student.getName() + " successfully registered for " + courseName);
                return true;
            } else {
                System.out.println("student: " + student.getName() + " is already registered for " + courseName);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }
}

// ---------------------- MODULE: CourseCatalog ----------------------
class CourseCatalog {
    private final Map<String, Course> courseMap = new ConcurrentHashMap<>();

    public void addCourse(Course course) {
        courseMap.put(course.getCourseCode().toLowerCase(), course);
    }

    public Course getCourseByCode(String code) {
        return courseMap.get(code.toLowerCase());
    }

    public List<Course> searchCoursesByName(String keyword) {
        List<Course> results = new ArrayList<>();
        for (Course c : courseMap.values()) {
            if (c.getCourseName().toLowerCase().contains(keyword.toLowerCase())) {
                results.add(c);
            }
        }
        return results;
    }

    public void displayAllCourses() {
        System.out.println("\n Available Courses:");
        for (Course c : courseMap.values()) {
            System.out.println(c.getCourseCode() + " - " + c.getCourseName() +
                    " | Instructor: " + c.getInstructor() +
                    " | Capacity: " + c.getEnrolledStudents().size() + "/" + c.getMaxCapacity());
        }
    }
}

// ---------------------- MODULE: RegistrationService ----------------------
class RegistrationService {
    private final CourseCatalog catalog;

    public RegistrationService(CourseCatalog catalog) {
        this.catalog = catalog;
    }

    public void registerStudentForCourse(Student student, String courseCode) {
        Course course = catalog.getCourseByCode(courseCode);
        if (course == null) {
            System.out.println("❌ Course code " + courseCode + " not found.");
            return;
        }

        boolean success = course.registerStudent(student);
        if (success) {
            student.addCourse(course);
        }
    }
}

public class CourseRegistrationSystem {
    public static void main(String[] args) throws InterruptedException {
        CourseCatalog catalog = new CourseCatalog();

        // Add some courses
        catalog.addCourse(new Course("CS101", "Data Structures", "Dr. Mehta", 2));
        catalog.addCourse(new Course("CS102", "Operating Systems", "Dr. Sharma", 1));
        catalog.addCourse(new Course("CS103", "Computer Networks", "Dr. Verma", 3));

        RegistrationService service = new RegistrationService(catalog);

        // Create Students
        Student s1 = new Student("S001", "Alice");
        Student s2 = new Student("S002", "Bob");
        Student s3 = new Student("S003", "Charlie");

        // Simulate concurrent registration using threads
        Runnable task1 = () -> service.registerStudentForCourse(s1, "CS101");
        Runnable task2 = () -> service.registerStudentForCourse(s2, "CS101");
        Runnable task3 = () -> service.registerStudentForCourse(s3, "CS101");

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        Thread t3 = new Thread(task3);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println();
        catalog.displayAllCourses();

        System.out.println();
        s1.viewRegisteredCourses();
        s2.viewRegisteredCourses();
        s3.viewRegisteredCourses();

        System.out.println();
        System.out.println("🔍 Searching for 'Operating':");
        for (Course c : catalog.searchCoursesByName("Operating")) {
            System.out.println("Found: " + c.getCourseName());
        }
    }
}


    