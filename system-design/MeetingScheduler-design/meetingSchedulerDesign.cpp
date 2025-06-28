#include<bits/stdc++.h>

using namespace std;

class User {
    public:
    int id;
    string name;
    vector<int> meetingIds;
    
    User() = default;
    User(int id, const string &name) : id(id), name(name) {}
};

class Meeting {
    public:
    int id;
    string title;
    int startTime ;
    int endTime;
    vector<int> participants;
    int roomId;
    
    Meeting() = default;
    Meeting(int id, const string& title, int startTime, int endTime,int roomId, const vector<int> &participants) :
    id(id), title(title), startTime(startTime), endTime(endTime), roomId(roomId), participants(participants) {}
};

class MeetingRoom {
    public:
    int id;
    string name;
    vector<int> meetingIds;
    
    MeetingRoom() = default; 
    MeetingRoom(int id, const string& name) : id(id), name(name) {}
};

class NotificationService {
public:
    static void notify(const User& user, const string& message) {
        cout << "Notification to " << user.name << ": " << message << endl;
    }
};

class UserService {
    unordered_map<int, User> users;
    
    public:
    void addUser(int id, const string& name) {
        users.emplace(id, User(id,name));
    }
    
    User* getUser(int id) {
        if(users.find(id) != users.end()) return &users[id];
        return nullptr;
    }
    
    vector<User*> getUsers(const vector<int> &ids) {
        vector<User*> result;
        for (int id : ids) {
            if (users.count(id)) result.push_back(&users[id]);
        }
        return result;
    }
};

class RoomManagementService {
    unordered_map<int, MeetingRoom> rooms;
    
    public:
    void addRoom(int id,const string& name) {
        rooms.emplace(id, MeetingRoom(id, name));
    }
    
    MeetingRoom* getRoom(int id) {
        if (rooms.count(id)) return &rooms[id];
        return nullptr;
    }
};

class MeetingDao {
    unordered_map<int, Meeting> meetings;
    int idCounter = 1;
    
    public:
    int saveMeeting(const Meeting& meeting) {
        meetings[meeting.id] = meeting;
        return meeting.id;
    }
    
    void removeMeeting(int id) {
        meetings.erase(id);
    }
    
    vector<Meeting> getAllMeetings() {
        vector<Meeting> res;
        for (auto& [_, m] : meetings) res.push_back(m);
        return res;
    }
    
    Meeting* getMeeting(int id) {
        if (meetings.count(id)) return &meetings[id];
        return nullptr;
    }

    int generateId() { return idCounter++; }
};

class MeetingScheduler {
    UserService& userService;
    RoomManagementService& roomService;
    MeetingDao& dao;
    
    public:
    MeetingScheduler(UserService& us, RoomManagementService& rs, MeetingDao& d)
        : userService(us), roomService(rs), dao(d) {}
        
    bool hasConflict(const vector<int>& userIds, int start, int end) {
        for (int uid : userIds) {
            User* user = userService.getUser(uid);
            if (!user) continue;
            for (int mid : user->meetingIds) {
                Meeting* m = dao.getMeeting(mid);
                if (m && !(end <= m->startTime || start >= m->endTime)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    void scheduleMeeting(const string& title, int start, int end, int roomId, const vector<int>& participants) {
        if (hasConflict(participants, start, end)) {
            cout << "Scheduling failed: Time conflict detected.\n";
            return;
        }
        
        int meetingId = dao.generateId();
        Meeting meeting(meetingId, title, start, end, roomId, participants);
        dao.saveMeeting(meeting);
        
        MeetingRoom* room = roomService.getRoom(roomId);
        if (room) room->meetingIds.push_back(meetingId);

        for (int uid : participants) {
            User* user = userService.getUser(uid);
            if (user) {
                user->meetingIds.push_back(meetingId);
                NotificationService::notify(*user, "Meeting '" + title + "' scheduled.");
            }
        }

        cout << "Meeting scheduled with ID: " << meetingId << endl;
    }
    
    void cancelMeeting(int meetingId) {
        Meeting* meeting = dao.getMeeting(meetingId);
        if (!meeting) {
            cout << "Meeting not found.\n";
            return;
        }

        for (int uid : meeting->participants) {
            User* user = userService.getUser(uid);
            if (user) {
                user->meetingIds.erase(remove(user->meetingIds.begin(), user->meetingIds.end(), meetingId), user->meetingIds.end());
                NotificationService::notify(*user, "Meeting '" + meeting->title + "' cancelled.");
            }
        }

        MeetingRoom* room = roomService.getRoom(meeting->roomId);
        if (room) {
            room->meetingIds.erase(remove(room->meetingIds.begin(), room->meetingIds.end(), meetingId), room->meetingIds.end());
        }

        dao.removeMeeting(meetingId);
        cout << "Meeting cancelled.\n";
    }
    
    void viewMeetings(int userId) {
        User* user = userService.getUser(userId);
        if (!user) {
            cout << "User not found.\n";
            return;
        }

        cout << "Meetings for user " << user->name << ":\n";
        for (int mid : user->meetingIds) {
            Meeting* m = dao.getMeeting(mid);
            if (m) {
                cout << "  [" << m->id << "] " << m->title << " (" << m->startTime << "-" << m->endTime << ")\n";
            }
        }
    }
};

int main() {
    UserService userService;
    RoomManagementService roomService;
    MeetingDao meetingDao;

    userService.addUser(1, "Alice");
    userService.addUser(2, "Bob");
    userService.addUser(3, "Charlie");

    roomService.addRoom(101, "Room A");

    MeetingScheduler scheduler(userService, roomService, meetingDao);

    scheduler.scheduleMeeting("Team Sync", 10, 11, 101, {1, 2});
    scheduler.scheduleMeeting("Project Brief", 10, 11, 101, {1, 3}); // Should conflict for Alice

    scheduler.viewMeetings(1);

    scheduler.cancelMeeting(1);

    scheduler.viewMeetings(1);

    return 0;
}