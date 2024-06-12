#include <bits/stdc++.h>
using namespace std;

class bookMovie{
private:
    string movie_name;
    string movie_type;
    int time_duration;  // let us keep this in minutes

public:
    bookMovie(const string& movie_name,const string& movie_type,int time_duration)
        : movie_name(movie_name), movie_type(movie_type), time_duration(time_duration) {}

        string getMovieName() const{
            return movie_name;
        }

        string getMovieType() const{
            return movie_type;
        }

        int getTimeDuration() const{
            return time_duration;
        }
};

class movieHall{
private:
   int total_seats;
   int available_seats;

public:
   movieHall(int total_seats) : total_seats(total_seats),available_seats(total_seats){}

   int getAvailableSeats() const{
    return available_seats;
   }

   bool bookSeatsStatus(int number_seats){
    if (number_seats <= available_seats){
        available_seats -= number_seats;
        return true;
    }
    return false;
   }
};

class Show {
private:
   bookMovie movie;
   movieHall hall;
   string show_time;

public:
    Show(const bookMovie& movie,const movieHall& hall, const string& show_time)
        : movie(movie), hall(hall), show_time(show_time) {}

        bookMovie getMovie() const {
            return movie;
        }

        movieHall getMovieHall() const{
            return hall;
        }

        string getShowTime() const{
            return show_time;
        }
};

class Ticket {
private:
   Show show;
   int seat_number;

public:
    Ticket(const Show& show,int seat_number) : show(show), seat_number(seat_number) {}

    Show getShow() const {
        return show;
    }

    int getSeatNumber() const{
        return seat_number;
    }
};

class Customer {
private:
   string cust_name;
   string cust_email;

public:
   Customer(const string& cust_name, const string& cust_email)
      : cust_name(cust_name), cust_email(cust_email) {}

    string getCustomerName() const {
        return cust_name;
    }

    string getCustomerEmail() const {
        return cust_email;
    }
};

class Booking {
private:
    Customer customer;
    Show show;
    Ticket ticket;

public:
    Booking(const Customer& customer, const Show& show, const Ticket& ticket, int seat_number)
      : customer(customer) , show(show) , ticket(show,seat_number) {}

      Customer getCustomer() const {
        return customer;
      }

      Show getShow() const {
        return show;
      }

      Ticket getTicket() const{
        return ticket;
      }
};

class Payment {
private:
   double amount_paid;
   string payment_method;

public:
    Payment(double amount_paid, const string& payment_method)
      : amount_paid(amount_paid) , payment_method(payment_method) {}

    double getAmount() const {
        return amount_paid;
    }

    string getPaymentMethod() const {
        return payment_method;
    }
};

class BookingSystem {
private:
   vector<bookMovie> movies;
   vector<movieHall> cinemaHalls;
   vector<Show> shows;
   vector<Booking> bookings;

public:
   void addMovie(const bookMovie& movie){
    movies.push_back(movie);
   }

   void addCinemaHall(const movieHall& hall){
    cinemaHalls.push_back(hall);
   }

   void addShow(const Show& show){
    shows.push_back(show);
   }

   Booking bookTicket(const Customer& customer, const Show& show,int number_seats){
    int hallIndex = 0; //assuming one hall
    movieHall& hall = cinemaHalls[hallIndex];

    if(hall.bookSeatsStatus(number_seats)){
        Booking booking(customer,show,hall.getAvailableSeats() + 1 - number_seats);
        bookings.push_back(booking);
        return booking;
    }
    else{
        cout<<"Seats are Filled - Not Available for Booking"<<endl;
            return bookTicket(Customer("", ""), Show(bookMovie("", "", 0), movieHall(0), ""), -1);
    }
   }

   void displayShows() {
    for (const auto& show : shows){
        cout<<"Movie:"<< show.getMovie().getMovieName()<< ", Genre:"<< show.getMovie().getMovieName()
        << ", Duration:"<< show.getMovie().getTimeDuration()<<"miutes"
        <<", Hall: "<<show.getMovieHall().getAvailableSeats()<<"seats available for booking"
        <<", Show Time:"<<show.getShowTime() << endl;
    }
   }
};

int main() {
    BookingSystem bookingSystem;

    bookMovie movie1("Elitmus","Sci-thriller",111);
    bookMovie movie2("Spiderman","Adventure",999);
    movieHall hall1(60);
    movieHall hall2(99);

    bookingSystem.addMovie(movie1);
    bookingSystem.addMovie(movie2);
    bookingSystem.addCinemaHall(hall1);
    bookingSystem.addCinemaHall(hall2);

    Show show1(movie1,hall1, "2024-06-12 06:00 HRS");
    Show show2(movie2,hall2, "2024-06-22 12:00 Noon");

    bookingSystem.addShow(show1);
    bookingSystem.addShow(show2);

    cout<<"Available shows to choose:"<<endl;
    bookingSystem.displayShows();

    Customer customer("Raghu Kumar","raghu.kumar@test.com");
    int numberSeatsToBook = 2;
    cout<<"\nBooking"<<numberSeatsToBook<<"seats available for"<<customer.getCustomerName()<<endl;
    Booking booking = bookingSystem.bookTicket(customer, show1, numberSeatsToBook);

    if(booking.getTicket().getSeatNumber() != -1){
        cout<<"Booking is Confirmed!"<<endl;
        cout<<"Movie:"<<booking.getShow().getMovie().getMovieName() <<endl;
        cout<<"Show Timing:"<<booking.getShow().getShowTime()<<endl;
        cout<<"Seat Number:"<<booking.getTicket().getSeatNumber()<<endl;
    }

    return 0;



}
