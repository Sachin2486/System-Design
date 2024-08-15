#include <iostream>
#include <vector>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <algorithm>

using namespace std;

class User{
public:
    string userName;
    int reputation;

    // Default constructor
    User() : userName(""), reputation(0) {}
    
    // Parameterized constructor
    User(string name) : userName(name), reputation(0) {}
};

class Question {
public:
    int id;
    string text;
    string author;
    vector<string> tags;
    int votes;
    vector<string> comments;

    Question(int id, string text, string author, vector<string> tags)
        : id(id), text(text), author(author), tags(tags), votes(0) {}
};

class Answer {
public:
    int id;
    string text;
    string author;
    int votes;
    vector<string> comments;

    Answer(int id, string text, string author) 
        : id(id), text(text), author(author), votes(0) {}
};

class QA_System {
private:
    unordered_map<string, User> users;
    vector<Question> questions;
    vector<Answer> answers;
    int questionIdCounter = 0;
    int answerIdCounter = 0;

public:
    void registerUser(string username) {
        users[username] = User(username);  
    }

    void postQuestion(string username, string text, vector<string> tags) {
        questionIdCounter++;
        questions.push_back(Question(questionIdCounter, text, username, tags));
    }

    void postAnswer(string username, int questionId, string text) {
        answerIdCounter++;
        answers.push_back(Answer(answerIdCounter, text, username));
    }

    void addCommentToQuestion(int questionId, string comment) {
        for (auto &q : questions) {
            if (q.id == questionId) {
                q.comments.push_back(comment);
                break;
            }
        }
    }

    void addCommentToAnswer(int answerId, string comment) {
        for (auto &a : answers) {
            if (a.id == answerId) {
                a.comments.push_back(comment);
                break;
            }
        }
    }

    void voteQuestion(int questionId, bool upvote) {
        for (auto &q : questions) {
            if (q.id == questionId) {
                q.votes += (upvote) ? 1 : -1;
                updateReputation(q.author, upvote);
                break;
            }
        }
    }

    void voteAnswer(int answerId, bool upvote) {
        for (auto &a : answers) {
            if (a.id == answerId) {
                a.votes += (upvote) ? 1 : -1;
                updateReputation(a.author, upvote);
                break;
            }
        }
    }

    vector<Question> searchByKeyword(string keyword) {
        vector<Question> results;
        for (auto &q : questions) {
            if (q.text.find(keyword) != string::npos) {
                results.push_back(q);
            }
        }
        return results;
    }

    vector<Question> searchByTag(string tag) {
        vector<Question> results;
        for (auto &q : questions) {
            if (find(q.tags.begin(), q.tags.end(), tag) != q.tags.end()) {
                results.push_back(q);
            }
        }
        return results;
    }

    vector<Question> searchByUser(string username) {
        vector<Question> results;
        for (auto &q : questions) {
            if (q.author == username) {
                results.push_back(q);
            }
        }
        return results;
    }

    void displayQuestions() {
        for (auto &q : questions) {
            cout << "Question ID: " << q.id << "\nAuthor: " << q.author << "\nText: " << q.text << "\nVotes: " << q.votes << endl;
            cout << "Comments: " << endl;
            for (auto &comment : q.comments) {
                cout << "- " << comment << endl;
            }
            cout << endl;
        }
    }

private:
    void updateReputation(string username, bool increase) {
        if (users.find(username) != users.end()) {
            users[username].reputation += (increase) ? 10 : -5;
        }
    }
};

int main() {
    QA_System system;

    system.registerUser("Alice");
    system.registerUser("Bob");

    system.postQuestion("Alice", "What is polymorphism?", {"C++", "OOP"});
    system.postQuestion("Bob", "What is the difference between a pointer and a reference?", {"C++", "Pointers"});

    system.postAnswer("Bob", 1, "Polymorphism is the ability to present the same interface for different data types.");
    
    system.addCommentToQuestion(1, "This is a great question!");
    system.addCommentToAnswer(1, "Good answer!");

    system.voteQuestion(1, true); 
    system.voteAnswer(1, true);   

    system.displayQuestions();

    vector<Question> results = system.searchByKeyword("pointer");
    cout << "Search Results:" << endl;
    for (auto &q : results) {
        cout << "Question ID: " << q.id << "\nText: " << q.text << endl;
    }

    return 0;
}
