#include <iostream>
#include <string>
#include <unordered_map>
#include <vector>
#include <mutex>

using namespace std;

class Course {
public:
	string courseCode;
	string courseName;
	string instructor;
	int maxCapacity;
	int currentEnrollment;

	// Default constructor
	Course() : maxCapacity(0), currentEnrollment(0) {}

	Course(const string &code, const string &name, const string &instructor_, int capacity)
		: courseCode(code), courseName(name), instructor(instructor_), maxCapacity(capacity), currentEnrollment(0) {}

	bool isFull() const {
		return currentEnrollment >= maxCapacity;
	}

	void enrollStudent() {
		if (!isFull()) {
			currentEnrollment++;
		} else {
			throw runtime_error("Course is Full!");
		}
	}
};

class Student {
public:
	string studentName;
	vector<string> registeredCourses;

	// Default constructor
	Student() {}

	Student(const string &name) : studentName(name) {}

	void viewRegisteredCourses() const {
		cout << studentName << "'s Registered Courses:\n";
		for (const string &course : registeredCourses) {
			cout << "- " << course << endl;
		}
	}

	void registerCourse(const string &courseCode) {
		registeredCourses.push_back(courseCode);
	}
};

class CourseRegistrationSystem {
private:
	unordered_map<string, Course> courses;
	unordered_map<string, Student> students;
	mutex mtx;

public:
	void addCourse(const string &courseCode, const string &courseName, const string &instructor, int capacity) {
		lock_guard<mutex> lock(mtx);
		courses.emplace(courseCode, Course(courseCode, courseName, instructor, capacity));
	}

	void registerForCourse(const string &studentName, const string &courseCode) {
		lock_guard<mutex> lock(mtx);

		// Check if the course exists
		if (courses.find(courseCode) == courses.end()) {
			cout << "Course " << courseCode << " not found.\n";
			return;
		}

		Course &course = courses[courseCode];

		// Check if the course is full
		if (course.isFull()) {
			cout << "Course " << course.courseName << " is full.\n";
			return;
		}

		course.enrollStudent();
		students[studentName].registerCourse(courseCode);

		cout << studentName << " successfully registered for " << course.courseName << ".\n";
	}

	void searchCourses(const string &searchTerm) const {
		cout << "Search results for '" << searchTerm << "':\n";
		for (const auto &pair : courses) {
			const Course &course = pair.second;
			if (course.courseCode.find(searchTerm) != string::npos || course.courseName.find(searchTerm) != string::npos) {
				cout << "Course Code: " << course.courseCode << ", Course Name: " << course.courseName
				     << ", Instructor: " << course.instructor << ", Capacity: " << course.currentEnrollment
				     << "/" << course.maxCapacity << endl;
			}
		}
	}

	void viewStudentCourses(const string &studentName) const {
		if (students.find(studentName) == students.end()) {
			cout << "Student " << studentName << " not found.\n";
			return;
		}
		students.at(studentName).viewRegisteredCourses();
	}
};

// Simulate concurrent registration requests from multiple students
void concurrentRegistration(CourseRegistrationSystem &system, const string &studentName, const string &courseCode) {
	system.registerForCourse(studentName, courseCode);
}

int main() {
	CourseRegistrationSystem system;
	system.addCourse("CS101", "Intro to Computer Science", "Dr. Smith", 2);
	system.addCourse("MATH201", "Calculus I", "Prof. Johnson", 3);

	system.registerForCourse("Animesh", "CS101");
	system.registerForCourse("Harsh", "CS101");
	system.registerForCourse("Ravi", "MATH201");

	system.viewStudentCourses("Ravi");

	system.searchCourses("CS");

	return 0;
}
