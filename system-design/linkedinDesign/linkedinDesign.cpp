#include<bits/stdc++.h>
using namespace std;

class Posts{
    private:
    int id;
    string postContent;

    public:
    Posts(int id,string postContent) : id(id) , postContent(postContent) {}

    void setPostContent(){
        postContent = postContent;
    }

    string getPostContent(){
        return postContent;
    }
};

class Jobs{
    private:
    int jobId;
    string roleDescription;

    public:
    Jobs(int jobId, string roleDescription) : jobId(jobId) , roleDescription(roleDescription) {}

    void setJobId(int jobId){
        jobId = jobId;
    }

    int getJobId(){
        return jobId;
    }

    void setRoleDescription(string newRoleDescription){
        roleDescription = newRoleDescription;
    }

    string getRoleDescription(){
        return roleDescription;
    }
};

class Users{
    private:
    int userId;
    string name;
    string description;
    int connections;
    vector<Users*> userConnections;
    vector<Posts> userPosts;
    vector<Jobs> userJobsApplied;

    public:
    Users(string name, string description, int userId) : name(name), description(description), userId(userId) {}

        void setName(string newName){
            name = newName;
        }

        string getName(){
            return name;
        }

        vector<Users*> getUserConnectionsList(){
            return userConnections;
        }

        void addConnection(Users* user){
            userConnections.push_back(user);
        }

        vector<Posts> getUserPostList(){
            return userPosts;
        }

        void addPost(Posts post){
            userPosts.push_back(post);
        }

        vector<Jobs> getUserJobAppliedList(){
            return userJobsApplied;
        }

        void addJobsApplied(Jobs job){
            userJobsApplied.push_back(job);
        }
};

class Linkedin{
    private:
    vector<Users> usersDatabase;

    public:
    void addUsertoDatabase(Users u){
        usersDatabase.push_back(u);
    }

    vector<Users> getAllUsers(){
        return usersDatabase;
    }

    vector<Posts> getUserFeed(Users user){
        vector<Posts> userFeeds;
        vector<Users*> userConnections = user.getUserConnectionsList();

        for(Users* userConnection : userConnections){
            vector<Posts> connectionsPost = userConnection->getUserPostList();
            userFeeds.insert(userFeeds.end(),connectionsPost.begin(),connectionsPost.end());
        }
        return userFeeds;
    }
};

int main() {
    Linkedin linkedin;
    Users user1("Sachin","Software developer",1);
    Users user2("John","QA Engineer",2);

    linkedin.addUsertoDatabase(user1);
    linkedin.addUsertoDatabase(user2);

    user1.addConnection(&user2);
    user2.addConnection(&user1);

    vector<Users*> ConnectionList = user1.getUserConnectionsList();
    cout<<"User connection count is:"<<ConnectionList.size()<<"\n";
    cout<<"\n";

    Posts post1(1 ,"Leetcode DSA challenge faced");
    Posts post2(2,"Open source programs");

    user2.addPost(post1);
    user2.addPost(post2);

    vector<Posts> johnPosts = user2.getUserPostList();
    cout<<"John posts count is:"<<johnPosts.size()<<"\n";
    cout<<"\n";

    vector<Posts> userFeedList = linkedin.getUserFeed(user1);
    cout<<"Sachin feed list is:"<<"\n";
    for(auto post:userFeedList){
        cout<<post.getPostContent()<<"\n";
    }
};