// This is the question that was asked in Zepto SDE-3 Machine Coding round interview as per LC discuss section

#include <bits/stdc++.h>

using namespace std;

enum class Permission {
	VIEW,
	EDIT
};

class Event {
public:
	string title;
	string location;
	time_t startTime;
	time_t endTime;

	Event(string title, string location, time_t startTime, time_t endTime)
		: title(title), location(location), startTime(startTime), endTime(endTime) {}

	void displayEvent() const {
		cout << "Event: " << title << "\nLocation: " << location << "\nStart Time: " << ctime(&startTime)
		     << "End Time: " << ctime(&endTime) << endl;
	}

	void editEvent(string newTitle, string newLocation, time_t newStartTime, time_t newEndTime) {
		title = newTitle;
		location = newLocation;
		startTime = newStartTime;
		endTime = newEndTime;
	}
};

class RecurringEvent : public Event {
public:
	string recurrencePattern;   // daily, weekly, or monthly

	RecurringEvent(string title, string location, time_t startTime, time_t endTime, string pattern)
		: Event(title, location, startTime, endTime), recurrencePattern(pattern) {}

	void displayRecurringEvent() const {
		displayEvent();
		cout << "Recurs: " << recurrencePattern << endl;
	}
};

class User {
public:
	string username;
	unordered_map<string, Permission> sharedUsers;
	vector<Event> events;
	vector<RecurringEvent> recurringEvents;

	User() : username("Unknown") {} //default constructor

	User(string name) : username(name) {}

	// Create event
	void createEvent(string title, string location, time_t start, time_t end) {
		Event newEvent(title, location, start, end);
		events.push_back(newEvent);
	}

	// Create recurring event
	void createRecurringEvent(string title, string location, time_t start, time_t end, string pattern) {
		RecurringEvent newEvent(title, location, start, end, pattern);
		recurringEvents.push_back(newEvent);
	}

	// Share event with permission
	void shareEvent(string otherUser, Permission permission) {
		sharedUsers[otherUser] = permission;
	}

	// Propose change for an event
	void proposeChange(int eventIndex, string newTitle, string newLocation, time_t newStart, time_t newEnd) {
		if (eventIndex < events.size()) {
			events[eventIndex].editEvent(newTitle, newLocation, newStart, newEnd);
			cout << "Proposed changes updated.\n";
		} else {
			cout << "Invalid event index.\n";
		}
	}

	// Find time conflicts
	void findAvailableTime() {
		cout << "Finding available time slots...\n";
	}

	// View all events
	void viewEvents() {
		cout << "All events for user: " << username << "\n";
		for (const auto &event : events) {
			event.displayEvent();
		}
		for (const auto &recEvent : recurringEvents) {
			recEvent.displayRecurringEvent();
		}
	}
};

class CalendarSystem {
public:
	unordered_map<string, User> users;

	// Add a new user to the system
	void addUser(string username) {
		if (users.find(username) == users.end()) {
			users.emplace(username, User(username));
		}
	}

	// Retrieve a user from the system
	User* getUser(string username) {
		if (users.find(username) != users.end()) {
			return &users[username];
		}
		return nullptr;
	}
};

time_t createTime(int year, int month, int day, int hour, int min) {
	tm timeStruct = {};
	timeStruct.tm_year = year - 1900;
	timeStruct.tm_mon = month - 1;
	timeStruct.tm_mday = day;
	timeStruct.tm_hour = hour;
	timeStruct.tm_min = min;
	return mktime(&timeStruct);
}

int main() {
	CalendarSystem calendar;

	// Add users to the calendar system
	calendar.addUser("alice");
	calendar.addUser("bob");

	// Get details for user Alice .. :)
	User* alice = calendar.getUser("alice");

	// Create events for Alice.. :)
	if (alice) {
		time_t start = createTime(2024, 9, 10, 10, 0);
		time_t end = createTime(2024, 9, 10, 12, 0);
		alice->createEvent("Team Meeting", "Conference Room", start, end);

		start = createTime(2024, 9, 11, 9, 0);
		end = createTime(2024, 9, 11, 10, 0);
		alice->createRecurringEvent("Daily Standup", "Online", start, end, "daily");

		// Share event with Bob
		alice->shareEvent("bob", Permission::VIEW);

		// View Alice's events
		alice->viewEvents();

		// Alice proposes a change
		alice->proposeChange(0, "Updated Meeting", "Main Hall", createTime(2024, 9, 10, 11, 0), createTime(2024, 9, 10, 13, 0));

		// Find available time slots :)
		alice->findAvailableTime();
	}

	return 0;
}
