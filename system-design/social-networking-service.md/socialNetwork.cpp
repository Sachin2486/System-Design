#include <bits/stdc++.h>
using namespace std;

enum class Visibility { PUBLIC, FRIENDS_ONLY, PRIVATE };

class User {
public:
    string userId, name, email, password;
    string profilePic, bio;

    unordered_set<string> interests, friends, friendRequests;

    User(string id, string n, string e, string p)
        : userId(id), name(n), email(e), password(p) {}

    bool authenticate(const string& pwd) const {
        return password == pwd;  // FIXED: Comparison instead of assignment
    }

    void updateProfile(string newBio, string pic, vector<string> newInterests) {
        bio = newBio;
        profilePic = pic;
        interests = unordered_set<string>(newInterests.begin(), newInterests.end());
    }

    void sendRequest(User& to) {
        to.friendRequests.insert(userId);  // FIXED: use insert, not 'interests' method
    }

    void acceptRequest(const string& fromId) {
        if (friendRequests.count(fromId)) {  // FIXED: count() instead of const()
            friends.insert(fromId);
            friendRequests.erase(fromId);
        }
    }

    void declineRequest(const string& fromId) {
        friendRequests.erase(fromId);
    }
};

class Post {
public:
    string postId, authorId, content;
    vector<string> mediaUrls;
    time_t timestamp;
    Visibility visibility;
    unordered_set<string> likes;
    vector<pair<string, string>> comments;

    Post(string pid, string author, string text, vector<string> media, Visibility vis)
        : postId(pid), authorId(author), content(text), mediaUrls(media), visibility(vis) {
        timestamp = time(0);
    }

    void like(const string& userId) {
        likes.insert(userId);
    }

    void comment(const string& userId, const string& msg) {
        comments.emplace_back(userId, msg);
    }
};

class SocialNetwork {
    unordered_map<string, User*> users;
    unordered_map<string, Post*> posts;
    mutex mtx;

public:
    ~SocialNetwork() {
        for (auto& entry : users) delete entry.second;
        for (auto& entry : posts) delete entry.second;
    }

    User* registerUser(string name, string email, string pwd) {
        string uid = "U" + to_string(users.size() + 1);
        User* u = new User(uid, name, email, pwd);
        users[uid] = u;
        return u;
    }

    User* login(string email, string pwd) {
        for (auto& entry : users) {
            User* u = entry.second;
            if (u->email == email && u->authenticate(pwd)) return u;
        }
        return nullptr;
    }

    Post* createPost(User* user, string text, vector<string> media, Visibility vis) {
        lock_guard<mutex> lock(mtx);
        string pid = "P" + to_string(posts.size() + 1);
        Post* p = new Post(pid, user->userId, text, media, vis);
        posts[pid] = p;
        return p;
    }

    vector<Post*> getNewsFeed(User* user) {
        vector<Post*> feed;
        for (auto& entry : posts) {
            Post* post = entry.second;
            if (post->visibility == Visibility::PUBLIC ||
                (post->visibility == Visibility::FRIENDS_ONLY && user->friends.count(post->authorId)) ||
                post->authorId == user->userId) {
                feed.push_back(post);
            }
        }
        sort(feed.begin(), feed.end(), [](Post* a, Post* b) {
            return a->timestamp > b->timestamp;
        });
        return feed;
    }

    void showPost(Post* p) {
        cout << "[Post by: " << p->authorId << "] " << p->content << endl;
        cout << "Likes: " << p->likes.size() << " | Comments: " << p->comments.size() << endl;
        for (auto& comment : p->comments)
            cout << "\t" << comment.first << ": " << comment.second << endl;
    }
};

int main() {
    SocialNetwork net;

    // Register users
    User* sachin = net.registerUser("Sachin", "sachin@mail.com", "1234");
    User* rahul = net.registerUser("Rahul", "rahul@mail.com", "pass");

    // Friend Request
    sachin->sendRequest(*rahul);
    rahul->acceptRequest(sachin->userId);

    // Create posts
    Post* p1 = net.createPost(sachin, "Hello friends!", {}, Visibility::FRIENDS_ONLY);
    Post* p2 = net.createPost(rahul, "Good morning!", {}, Visibility::PUBLIC);

    // Like & Comment
    p2->like(sachin->userId);
    p2->comment(sachin->userId, "Nice!");

    // News Feed
    auto feed = net.getNewsFeed(sachin);
    cout << "\n---- Sachin's NewsFeed ----\n";
    for (auto* p : feed) net.showPost(p);

    return 0;
}
