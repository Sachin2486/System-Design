#include <bits/stdc++.h>
#include <iostream>

using namespace std;

class User {
    public:
    int id;
    string name;
    int reputation;
    
    User(int id, const string& name) : id(id), name(name), reputation(0) {}
    
    void addReputation(int delta) {
        lock_guard<mutex> lock(mtx);
        reputation += delta;
    }
    
    private:
    mutex mtx;
};

class Comment {
    public:
    string content;
    shared_ptr<User> author;
    
    Comment(const string& content, shared_ptr<User> author)
    : content(content), author(author) {}
    
};

class Answer {
    public:
    string content;
    shared_ptr<User> author;
    int votes;
    vector<Comment> comments;
    mutex mtx;
    
    Answer(const string& content, shared_ptr<User> author)
    : content(content), author(author), votes(0) {}
    
    void addComment(const Comment& c) {
        lock_guard<mutex> lock(mtx);
        comments.push_back(c);
    }
    
    void vote(int delta) {
        lock_guard<mutex> lock(mtx);
        votes += delta;
        author->addReputation(delta > 0 ? 10 : -2);
    }
};

class Question {
public:
    std::string title;
    std::string description;
    std::vector<std::string> tags;
    std::shared_ptr<User> author;
    int votes;
    std::vector<Comment> comments;
    std::vector<std::shared_ptr<Answer>> answers;
    std::mutex mtx;

    Question(const std::string& title, const std::string& description,
             const std::vector<std::string>& tags, std::shared_ptr<User> author)
        : title(title), description(description), tags(tags), author(author), votes(0) {}

    void addComment(const Comment& c) {
        std::lock_guard<std::mutex> lock(mtx);
        comments.push_back(c);
    }

    void addAnswer(std::shared_ptr<Answer> ans) {
        std::lock_guard<std::mutex> lock(mtx);
        answers.push_back(ans);
    }

    void vote(int delta) {
        std::lock_guard<std::mutex> lock(mtx);
        votes += delta;
        author->addReputation(delta > 0 ? 5 : -1);
    }

    bool matchesKeyword(const std::string& keyword) {
        return title.find(keyword) != std::string::npos || description.find(keyword) != std::string::npos;
    }

    bool hasTag(const std::string& tag) {
        return std::find(tags.begin(), tags.end(), tag) != tags.end();
    }

    bool isByUser(const std::shared_ptr<User>& user) {
        return author->id == user->id;
    }
};

class QASystem {
private:
    std::vector<std::shared_ptr<User>> users;
    std::vector<std::shared_ptr<Question>> questions;
    std::mutex mtx;

public:
    std::shared_ptr<User> createUser(const std::string& name) {
        static int userId = 1;
        auto user = std::make_shared<User>(userId++, name);
        std::lock_guard<std::mutex> lock(mtx);
        users.push_back(user);
        return user;
    }

    std::shared_ptr<Question> postQuestion(std::shared_ptr<User> user, const std::string& title,
                                           const std::string& desc, const std::vector<std::string>& tags) {
        auto q = std::make_shared<Question>(title, desc, tags, user);
        std::lock_guard<std::mutex> lock(mtx);
        questions.push_back(q);
        return q;
    }

    void searchByKeyword(const std::string& keyword) {
        std::lock_guard<std::mutex> lock(mtx);
        std::cout << "Questions matching keyword '" << keyword << "':\n";
        for (auto& q : questions) {
            if (q->matchesKeyword(keyword)) {
                std::cout << "- " << q->title << "\n";
            }
        }
    }

    void searchByTag(const std::string& tag) {
        std::lock_guard<std::mutex> lock(mtx);
        std::cout << "Questions with tag '" << tag << "':\n";
        for (auto& q : questions) {
            if (q->hasTag(tag)) {
                std::cout << "- " << q->title << "\n";
            }
        }
    }

    void searchByUser(std::shared_ptr<User> user) {
        std::lock_guard<std::mutex> lock(mtx);
        std::cout << "Questions by user '" << user->name << "':\n";
        for (auto& q : questions) {
            if (q->isByUser(user)) {
                std::cout << "- " << q->title << "\n";
            }
        }
    }
};

int main() {
    QASystem system;

    auto alice = system.createUser("Alice");
    auto bob = system.createUser("Bob");

    auto q1 = system.postQuestion(alice, "What is a mutex?", "How does a mutex work in C++?",
                                  {"c++", "threads", "mutex"});

    auto ans1 = std::make_shared<Answer>("A mutex is a mutual exclusion lock.", bob);
    q1->addAnswer(ans1);

    q1->addComment(Comment("Nice question!", bob));
    ans1->addComment(Comment("Great explanation!", alice));

    q1->vote(1);
    ans1->vote(1);

    system.searchByKeyword("mutex");
    system.searchByTag("threads");
    system.searchByUser(alice);

    std::cout << alice->name << "'s Reputation: " << alice->reputation << "\n";
    std::cout << bob->name << "'s Reputation: " << bob->reputation << "\n";

    return 0;
}
