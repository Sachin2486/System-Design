#include <iostream>
#include <vector>
#include <unordered_map>
#include <mutex>
#include <thread>
#include <memory>
#include <chrono>

using namespace std;

// ---------- ENUMS ----------
enum class NotificationType { EMAIL, SMS };

// ---------- MOVIE ----------
class Movie {
public:
    string title, language, genre;
    int duration;

    Movie(string t, string l, string g, int d)
        : title(t), language(l), genre(g), duration(d) {}
};

// ---------- SEAT ----------
class Seat {
public:
    int seatNumber;
    bool isBooked;

    Seat(int n) : seatNumber(n), isBooked(false) {}
};

// ---------- SCREEN ----------
class Screen {
public:
    int screenId;
    vector<Seat> seats;

    Screen(int id, int numSeats) : screenId(id) {
        for (int i = 1; i <= numSeats; ++i)
            seats.emplace_back(i);
    }
};

// ---------- SHOW ----------
class Show {
public:
    int showId;
    shared_ptr<Movie> movie;
    shared_ptr<Screen> screen;
    string timing;

    Show(int id, shared_ptr<Movie> m, shared_ptr<Screen> s, string t)
        : showId(id), movie(m), screen(s), timing(t) {}
};

// ---------- CINEMA ----------
class Cinema {
public:
    string name, location;
    vector<shared_ptr<Screen>> screens;
    vector<shared_ptr<Show>> shows;

    Cinema(string n, string l) : name(n), location(l) {}

    void addScreen(shared_ptr<Screen> screen) {
        screens.push_back(screen);
    }

    void addShow(shared_ptr<Show> show) {
        shows.push_back(show);
    }
};

// ---------- NOTIFICATION ----------
class INotification {
public:
    virtual void send(const string& message) = 0;
};

class EmailNotification : public INotification {
public:
    void send(const string& message) override {
        cout << "[EMAIL]: " << message << endl;
    }
};

class SMSNotification : public INotification {
public:
    void send(const string& message) override {
        cout << "[SMS]: " << message << endl;
    }
};

// ---------- USER ----------
class User {
public:
    string name;
    NotificationType preferredMode;

    User(string n, NotificationType mode) : name(n), preferredMode(mode) {}

    void notify(const string& message) {
        unique_ptr<INotification> notifier;
        if (preferredMode == NotificationType::EMAIL)
            notifier = make_unique<EmailNotification>();
        else
            notifier = make_unique<SMSNotification>();

        notifier->send(name + ": " + message);
    }
};

// ---------- BOOKING ----------
class Booking {
public:
    shared_ptr<User> user;
    shared_ptr<Show> show;
    vector<int> seatNumbers;

    Booking(shared_ptr<User> u, shared_ptr<Show> s, vector<int> seats)
        : user(u), show(s), seatNumbers(seats) {}
};

// ---------- BOOKING SERVICE ----------
class BookingService {
private:
    mutex bookingMutex;

public:
    void bookSeats(shared_ptr<User> user, shared_ptr<Show> show, vector<int> seatNumbers) {
        lock_guard<mutex> lock(bookingMutex);
        cout << user->name << " is trying to book seats..." << endl;

        for (int num : seatNumbers) {
            if (num > show->screen->seats.size() || show->screen->seats[num - 1].isBooked) {
                user->notify("Seat " + to_string(num) + " is already booked or invalid!");
                return;
            }
        }

        // Simulate delay
        this_thread::sleep_for(chrono::milliseconds(100));

        for (int num : seatNumbers)
            show->screen->seats[num - 1].isBooked = true;

        user->notify("Booking confirmed for movie: " + show->movie->title);
    }
};

// ---------- ADMIN ----------
class Admin {
public:
    static shared_ptr<Movie> createMovie(string title, string lang, string genre, int dur) {
        return make_shared<Movie>(title, lang, genre, dur);
    }

    static shared_ptr<Cinema> createCinema(string name, string loc) {
        return make_shared<Cinema>(name, loc);
    }

    static shared_ptr<Screen> createScreen(int id, int seats) {
        return make_shared<Screen>(id, seats);
    }

    static shared_ptr<Show> createShow(int id, shared_ptr<Movie> m, shared_ptr<Screen> s, string time) {
        return make_shared<Show>(id, m, s, time);
    }
};

// ---------- SEARCH SERVICE ----------
class SearchService {
public:
    static void searchMovies(const vector<shared_ptr<Movie>>& movies, string keyword) {
        cout << "\nSearching for movies containing '" << keyword << "':\n";
        for (auto& m : movies) {
            if (m->title.find(keyword) != string::npos)
                cout << "- " << m->title << " (" << m->language << ", " << m->genre << ")\n";
        }
    }
};

// ---------- MAIN ----------
int main() {
    // Admin Setup
    auto movie1 = Admin::createMovie("Inception", "English", "Sci-Fi", 148);
    auto movie2 = Admin::createMovie("Dangal", "Hindi", "Drama", 161);

    auto cinema = Admin::createCinema("PVR", "Mumbai");
    auto screen1 = Admin::createScreen(1, 10);
    cinema->addScreen(screen1);

    auto show1 = Admin::createShow(101, movie1, screen1, "7 PM");
    cinema->addShow(show1);

    vector<shared_ptr<Movie>> movieDB = { movie1, movie2 };

    // Users
    auto user1 = make_shared<User>("Sachin", NotificationType::EMAIL);
    auto user2 = make_shared<User>("Priya", NotificationType::SMS);

    BookingService bookingService;

    // Search
    SearchService::searchMovies(movieDB, "Incep");

    // Simulate Booking from two threads
    thread t1([&]() {
        bookingService.bookSeats(user1, show1, { 1, 2 });
    });

    thread t2([&]() {
        bookingService.bookSeats(user2, show1, { 2, 3 });
    });

    t1.join();
    t2.join();

    return 0;
}
