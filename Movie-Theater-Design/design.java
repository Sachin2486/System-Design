import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

enum ScheduleStatus {
        SCHEDULED,
        REJECTED
}

class Movie {
        final long id;
        final String title;
        final int durationInMinutes;

        Movie(long id, String title, int durationInMinutes) {
                this.id = id;
                this.title = title;
                this.durationInMinutes = durationInMinutes;
        }
}

class Screening {
        final Movie movie;
        final int startTime;
        final int endTime;

        Screening(Movie movie, int startTime) {
                this.movie = movie;
                this.startTime = startTime;
                this.endTime = startTime + movie.durationInMinutes;
        }

}

class MovieSchedule {
        final List<Screening> screenings = new ArrayList<>();

        void addScreening(Screening screening) {
                screenings.add(screening);
        }

        List<Screening> getScreenings() {
                return screenings;
        }
}

class CinemaService {
        static final int OPEN_TIME = 600;   // 10 AM
        static final int CLOSE_TIME = 1380; // 11 PM

        private final AtomicLong movieIdGen = new AtomicLong(1000);

        Movie createMovie(String title, int duration) {
                return new Movie(movieIdGen.getAndIncrement(), title, duration);
        }

        boolean canSchedule(Movie movie, MovieSchedule schedule) {
                int duration = movie.durationInMinutes;

                for(int start = OPEN_TIME; start + duration <= CLOSE_TIME; start++) {

                        int end = start + duration;
                        boolean conflict = false;

                        for (Screening existing : schedule.getScreenings()) {
                                if(isOverlapping(start, end, existing.startTime, existing.endTime)) {
                                        conflict = true;
                                        break;
                                }
                        }

                        if(!conflict) {
                                return true;
                        }
                }
                return false;
        }

        private boolean isOverlapping(int s1, int e1, int s2, int e2) {
                return s1 < e2 && e1 > s2;
        }
}


public class Main
{
        public static void main(String[] args) {

                CinemaService cinemaService = new CinemaService();
                MovieSchedule schedule = new MovieSchedule();

                Movie lotr = cinemaService.createMovie("Lord Of The Rings", 120);
                Movie bttf = cinemaService.createMovie("Back To The Future", 90);
                Movie inception = cinemaService.createMovie("Inception", 100);

                schedule.addScreening(new Screening(lotr, 660));   // 11:00 AM
                schedule.addScreening(new Screening(lotr, 840));   // 2:00 PM
                schedule.addScreening(new Screening(bttf, 1020));  // 5:00 PM
                schedule.addScreening(new Screening(lotr, 1200));  // 8:00 PM

                boolean canSchedule = cinemaService.canSchedule(inception, schedule);
                System.out.println("Can schedule Inception? " + canSchedule);

        }
}
