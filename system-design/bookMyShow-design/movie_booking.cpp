#include<bits/stdc++.h>

using namespace std;

enum class SeatType {
    REGULAR,
    PREMIUM
};

enum class SeatStatus {
    AVAILABLE,
    BOOKED
};

class Seat {
    int row,col;
    SeatType type;
    SeatStatus status;
    double price;
    mutex seatMutex;
    
    public:
    Seat(int r, int c, SeatType type, double price) : 
    row(r), col(c), type(type), price(price), status(SeatStatus::AVAILABLE) {}
    
    bool bookSeat() {
        lock_guard<mutex> lock(seatMutex);
        if(status == SeatStatus::AVAILABLE){
            status = SeatStatus::BOOKED;
            return true;
        }
        return false;
    }
    
    SeatStatus getStatus() const {
        return status;
    }
    
    double getPrice() const {
        return price;
    }
    
    string getSeatLabel() const {
        return "R" + to_string(row) + "C" + to_string(col);
    }
};

class Movie {
    string id, title, genre;
    int duration;
    
    public:
    Movie(string id, string title, string genre, int duration) :
    id(id), title(title), genre(genre), duration(duration) {}
    
    string getTitle() const {
        return title;
    }
    
    string getId() const {
        return id;
    }
};

class Show {
    string id;
    shared_ptr<Movie> movie;
    string time;
    vector<vector<shared_ptr<Seat>>> seats;
    
    public:
    Show(string id, shared_ptr<Movie> movie, string time, int rows,int cols) :
    id(id), movie(movie), time(time) {
        for (int i=0; i<rows; ++i){
            vector<shared_ptr<Seat>> row;
            for(int j=0; j<cols; ++j) {
                SeatType type = (i < 2) ? SeatType::PREMIUM : SeatType::REGULAR;
                double price = (type == SeatType::PREMIUM) ? 300 : 150;
                row.push_back(make_shared<Seat>(i, j, type, price));
            }
            seats.push_back(row);
        }
    }
    
    shared_ptr<Movie> getMovie() const {
        return movie;
    }
    
    string getTime() const {
        return time;
    }
    
    string getId() const {
        return id;
    }
    
    void displaySeats() {
        cout << "Seating for Show [" << movie->getTitle() << " at " << time << "]\n";
        for (auto& row : seats) {
            for (auto& seat : row) {
                cout << (seat->getStatus() == SeatStatus::AVAILABLE ? "[O]" : "[X]");
            }
            cout<<endl;
        }
    }
    
    bool bookSeats(const vector<pair<int,int>> &requestedSeats) {
        for(auto& seat : requestedSeats) {
            if(!seats[seat.first][seat.second]->bookSeat()){
                return false;
            }
        }
        return true;
    }
    
    double calculateTotal(const vector<pair<int,int>> &requestedSeats) {
        double total = 0;
        for(auto& s: requestedSeats)
        total += seats[s.first][s.second]->getPrice();
        return total;
    }
};

class Screen {
    string id;
    vector<shared_ptr<Show>> shows;
    
    public:
    Screen(string id) : id(id) {}
    
    void addShow(shared_ptr<Show> show) {
        shows.push_back(show);
    }
    
    const vector<shared_ptr<Show>>& getShows() const { 
        return shows; 
    }
};

class Theater {
    string id, name, location;
    vector<shared_ptr<Screen>> screens;

public:
    Theater(string id, string name, string location)
        : id(id), name(name), location(location) {}

    void addScreen(shared_ptr<Screen> screen) {
        screens.push_back(screen);
    }

    const vector<shared_ptr<Screen>>& getScreens() const {
        return screens;
    }

    string getName() const { return name; }
    string getLocation() const { return location; }
};

class BookingSystem {
    unordered_map<string, shared_ptr<Movie>> movies;
    unordered_map<string, shared_ptr<Theater>> theaters;

public:
    void addMovie(shared_ptr<Movie> movie) {
        movies[movie->getId()] = movie;
    }

    void addTheater(shared_ptr<Theater> theater) {
        theaters[theater->getName()] = theater;
    }

    void showAllMovies() {
        cout << "\nAvailable Movies:\n";
        for (auto& m : movies) {
            cout << m.second->getTitle() << endl;
        }
    }

    void showTheaterShows(string theaterName) {
        if (!theaters.count(theaterName)) return;

        auto theater = theaters[theaterName];
        cout << "\nShows in " << theater->getName() << ":\n";
        for (auto& screen : theater->getScreens()) {
            for (auto& show : screen->getShows()) {
                cout << "Movie: " << show->getMovie()->getTitle()
                     << ", Time: " << show->getTime()
                     << ", Show ID: " << show->getId() << endl;
            }
        }
    }

    shared_ptr<Show> getShowById(string showId) {
        for (auto& theater : theaters) {
            for (auto& screen : theater.second->getScreens()) {
                for (auto& show : screen->getShows()) {
                    if (show->getId() == showId) {
                        return show;
                    }
                }
            }
        }
        return nullptr;
    }

    void bookTicket(string showId, vector<pair<int, int>> seatList) {
        auto show = getShowById(showId);
        if (!show) {
            cout << "Show not found.\n";
            return;
        }

        show->displaySeats();
        if (show->bookSeats(seatList)) {
            double total = show->calculateTotal(seatList);
            cout << "Payment of ₹" << total << " successful. Booking confirmed!\n";
        } else {
            cout << "Booking failed. One or more seats unavailable.\n";
        }
    }
};

int main() {
    BookingSystem system;

    auto m1 = make_shared<Movie>("m1", "Inception", "Sci-fi", 148);
    auto m2 = make_shared<Movie>("m2", "Joker", "Drama", 122);
    system.addMovie(m1);
    system.addMovie(m2);

    auto t1 = make_shared<Theater>("t1", "PVR Phoenix", "Mumbai");
    auto screen1 = make_shared<Screen>("s1");

    auto show1 = make_shared<Show>("sh1", m1, "6:00 PM", 5, 5);
    auto show2 = make_shared<Show>("sh2", m2, "9:00 PM", 5, 5);

    screen1->addShow(show1);
    screen1->addShow(show2);
    t1->addScreen(screen1);

    system.addTheater(t1);

    system.showAllMovies();
    system.showTheaterShows("PVR Phoenix");

    // User wants to book seats for show 'sh1'
    vector<pair<int, int>> seats = { {0, 0}, {0, 1} };
    system.bookTicket("sh1", seats);

    return 0;
}
