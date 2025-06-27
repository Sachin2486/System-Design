#include <iostream>
#include <vector>
#include <string>
#include <map>
#include <set>
#include <ctime>
#include <sstream>
#include <algorithm>
using namespace std;

enum class Permission { VIEW, EDIT };
enum class Recurrence { NONE, DAILY, WEEKLY };

struct DateTime {
    int year, month, day, hour, minute;

    string toString() const {
        stringstream ss;
        ss << year << "-" << month << "-" << day << " " << hour << ":" << (minute < 10 ? "0" : "") << minute;
        return ss.str();
    }

    bool operator<(const DateTime& other) const {
        return tie(year, month, day, hour, minute) < tie(other.year, other.month, other.day, other.hour, other.minute);
    }

    bool overlapsWith(const DateTime& otherStart, const DateTime& otherEnd) const {
        return *this >= otherStart && *this < otherEnd;
    }

    bool operator>=(const DateTime& other) const {
        return !(*this < other);
    }
    
    bool operator>(const DateTime& other) const {
    return tie(year, month, day, hour, minute) > tie(other.year, other.month, other.day, other.hour, other.minute);
    }

    DateTime next(Recurrence recur) const {
        DateTime nextTime = *this;
        if (recur == Recurrence::DAILY) {
            nextTime.day += 1;
        } else if (recur == Recurrence::WEEKLY) {
            nextTime.day += 7;
        }
        // Simplified; does not handle month/year overflow
        return nextTime;
    }
};

struct Event {
    string eventId;
    string title;
    string location;
    DateTime startTime;
    DateTime endTime;
    Recurrence recurrence;
    set<string> sharedWithView;
    set<string> sharedWithEdit;
    bool isProposal = false;
};

class Calendar {
    string ownerId;
    map<string, Event> events;

public:
    Calendar(string userId) : ownerId(userId) {}

    void createEvent(const string& id, const string& title, const string& location,
                     const DateTime& start, const DateTime& end, Recurrence recur) {
        if (events.count(id)) {
            cout << "Event ID already exists.\n";
            return;
        }
        events[id] = {id, title, location, start, end, recur};
        cout << "Event created: " << title << " from " << start.toString() << " to " << end.toString() << endl;
    }

    void shareEvent(const string& eventId, const string& userId, Permission permission) {
        if (!events.count(eventId)) {
            cout << "Event not found.\n";
            return;
        }
        if (permission == Permission::VIEW)
            events[eventId].sharedWithView.insert(userId);
        else
            events[eventId].sharedWithEdit.insert(userId);

        cout << "Event " << eventId << " shared with " << userId << " as " << (permission == Permission::EDIT ? "EDIT" : "VIEW") << endl;
    }

    void editEvent(const string& userId, const string& eventId, const string& newTitle) {
        if (!events.count(eventId)) {
            cout << "Event not found.\n";
            return;
        }
        if (ownerId != userId && events[eventId].sharedWithEdit.find(userId) == events[eventId].sharedWithEdit.end()) {
            cout << "User has no edit permission.\n";
            return;
        }
        events[eventId].title = newTitle;
        cout << "Event updated to: " << newTitle << endl;
    }

    void proposeNewTime(const string& userId, const string& originalEventId, const DateTime& newStart, const DateTime& newEnd) {
        if (!events.count(originalEventId)) {
            cout << "Original event not found.\n";
            return;
        }
        string proposalId = originalEventId + "_proposal_" + userId;
        Event newProposal = events[originalEventId];
        newProposal.eventId = proposalId;
        newProposal.startTime = newStart;
        newProposal.endTime = newEnd;
        newProposal.title = "[Proposed] " + newProposal.title;
        newProposal.isProposal = true;
        events[proposalId] = newProposal;
        cout << "Proposed new time for event " << originalEventId << ": " << newStart.toString() << " to " << newEnd.toString() << endl;
    }

    void showEvents(const string& userId) {
        for (const auto& [id, ev] : events) {
            if (ownerId == userId || ev.sharedWithView.count(userId) || ev.sharedWithEdit.count(userId)) {
                cout << id << ": " << ev.title << " at " << ev.location << ", from " << ev.startTime.toString() << " to " << ev.endTime.toString();
                if (ev.recurrence != Recurrence::NONE)
                    cout << " [Recurring]";
                if (ev.isProposal)
                    cout << " [Proposal]";
                cout << endl;
            }
        }
    }

    void findFreeSlot(const string& userId, const DateTime& startRange, const DateTime& endRange) {
        cout << "\nFinding free slots for user: " << userId << " between " << startRange.toString() << " and " << endRange.toString() << "\n";
        vector<pair<DateTime, DateTime>> busy;

        for (auto& [id, ev] : events) {
            if (ownerId == userId || ev.sharedWithView.count(userId) || ev.sharedWithEdit.count(userId)) {
                DateTime curStart = ev.startTime;
                DateTime curEnd = ev.endTime;
                while (curStart < endRange) {
                    if (curStart >= startRange && curStart < endRange) {
                        busy.emplace_back(curStart, curEnd);
                    }
                    if (ev.recurrence == Recurrence::NONE) break;
                    curStart = curStart.next(ev.recurrence);
                    curEnd = curEnd.next(ev.recurrence);
                }
            }
        }

        sort(busy.begin(), busy.end());
        DateTime current = startRange;
        for (auto& slot : busy) {
            if (current < slot.first) {
                cout << "Free slot: " << current.toString() << " to " << slot.first.toString() << endl;
            }
            if (slot.second > current) current = slot.second;
        }
        if (current < endRange) {
            cout << "Free slot: " << current.toString() << " to " << endRange.toString() << endl;
        }
    }
};

// ------------ MAIN TEST ------------
int main() {
    Calendar cal("user123");

    DateTime start = {2025, 6, 25, 10, 0};
    DateTime end = {2025, 6, 25, 11, 0};

    cal.createEvent("e1", "Team Meeting", "Conf Room", start, end, Recurrence::WEEKLY);
    cal.shareEvent("e1", "alice", Permission::VIEW);
    cal.shareEvent("e1", "bob", Permission::EDIT);

    cal.editEvent("bob", "e1", "Updated Sync");

    DateTime newStart = {2025, 6, 25, 15, 0};
    DateTime newEnd = {2025, 6, 25, 16, 0};
    cal.proposeNewTime("alice", "e1", newStart, newEnd);

    cout << "\n--- Events for alice ---\n";
    cal.showEvents("alice");

    cout << "\n--- Free Slots for alice ---\n";
    DateTime rangeStart = {2025, 6, 25, 9, 0};
    DateTime rangeEnd = {2025, 6, 25, 18, 0};
    cal.findFreeSlot("alice", rangeStart, rangeEnd);

    return 0;
}
