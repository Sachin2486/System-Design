import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* Represents a player and their statistics */
class Player {
	private String name;
	private String role;
	private int runs;
	private int wickets;

	public Player(String name, String role) {
		this.name = name;
		this.role = role;
	}

	public String getName() {
		return name;
	}

	public String getRole() {
		return role;
	}

	public void addRuns(int r) {
		runs += r;
	}

	public void addWicket() {
		wickets++;
	}

	public String getStats() {
		return name + " (" + role + ") - Runs: " + runs + ", Wickets: " + wickets;
	}
}

/* Represents a team which contains multiple players */
class Team {
	private String name;
	private List<Player> players = new ArrayList<>();

	public Team(String name) {
		this.name = name;
	}

	public void addPlayer(Player p) {
		players.add(p);
	}

	public String getName() {
		return name;
	}

	public List<Player> getPlayers() {
		return players;
	}
}

/* Represents the scorecard of a match */
class Scorecard {
	private int runs = 0;
	private int wickets = 0;
	private float overs = 0;

	public synchronized void updateScore(int run, boolean wicket) {
		runs += run;
		if (wicket) wickets++;
		overs += 0.1; // simple overs increment
	}

	public String getScore() {
		return runs + "/" + wickets + " in " + String.format("%.1f", overs) + " overs";
	}
}

/* Holds commentary lines for a match */
class Commentary {
	private List<String> lines = new ArrayList<>();

	public void addLine(String line) {
		lines.add(line);
	}
	public List<String> getAll() {
		return lines;
	}
}

/* Represents a cricket match between two teams */
class Match {
	private String matchId;
	private Team teamA, teamB;
	private Scorecard scorecard = new Scorecard();
	private Commentary commentary = new Commentary();
	private String status = "Upcoming";

	public Match(String matchId, Team a, Team b) {
		this.matchId = matchId;
		this.teamA = a;
		this.teamB = b;
	}

	public String getMatchId() {
		return matchId;
	}
	public Scorecard getScorecard() {
		return scorecard;
	}
	public Commentary getCommentary() {
		return commentary;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String s) {
		status = s;
	}
	public String getTitle() {
		return teamA.getName() + " vs " + teamB.getName();
	}
}

/* Observer interface for live score updates */
interface ScoreSubscriber {
    void onScoreUpdate(Match match);
}

/* Handles publishing live score updates to subscribers (Observer pattern) */
class LiveScoreUpdater {
    private List<ScoreSubscriber> subscribers = new ArrayList<>();

    public void subscribe(ScoreSubscriber sub) { subscribers.add(sub); }
    public void unsubscribe(ScoreSubscriber sub) { subscribers.remove(sub); }

    public void publish(Match m) {
        for (ScoreSubscriber s : subscribers) {
            s.onScoreUpdate(m);
        }
    }
}

/* Central registry and manager of all matches (Singleton pattern) */
class MatchManager {
    private static MatchManager instance;
    private Map<String, Match> matches = new ConcurrentHashMap<>();
    private LiveScoreUpdater updater = new LiveScoreUpdater();

    private MatchManager() {}

    public static synchronized MatchManager getInstance() {
        if (instance == null) instance = new MatchManager();
        return instance;
    }

    public void addMatch(Match m) { matches.put(m.getMatchId(), m); }
    public Match getMatch(String id) { return matches.get(id); }
    public Collection<Match> getAllMatches() { return matches.values(); }
    public LiveScoreUpdater getUpdater() { return updater; }
}

/* Provides searching capability for matches, teams and players */
class SearchService {
    public static List<Match> searchMatchesByTeam(String teamName) {
        List<Match> result = new ArrayList<>();
        for (Match m : MatchManager.getInstance().getAllMatches()) {
            if (m.getTitle().toLowerCase().contains(teamName.toLowerCase())) {
                result.add(m);
            }
        }
        return result;
    }
}

public class CricinfoDemo {
    public static void main(String[] args) {
        // Create Teams and Players
        Team india = new Team("India");
        india.addPlayer(new Player("Virat Kohli", "Batsman"));
        india.addPlayer(new Player("Bumrah", "Bowler"));

        Team aus = new Team("Australia");
        aus.addPlayer(new Player("Steve Smith", "Batsman"));
        aus.addPlayer(new Player("Starc", "Bowler"));

        // Create Match and add to manager
        Match match = new Match("M1", india, aus);
        match.setStatus("Live");
        MatchManager.getInstance().addMatch(match);

        // Subscribe to live updates
        MatchManager.getInstance().getUpdater().subscribe(
            m -> System.out.println("Live update: " + m.getTitle() + " Score: " + m.getScorecard().getScore())
        );

        // Update score (simulating live updates)
        match.getScorecard().updateScore(4, false);
        match.getCommentary().addLine("Kohli hits a boundary!");
        MatchManager.getInstance().getUpdater().publish(match);

        match.getScorecard().updateScore(6, false);
        match.getCommentary().addLine("Kohli hits a six!");
        MatchManager.getInstance().getUpdater().publish(match);

        // Search for matches by team name
        System.out.println("\nSearching for matches with 'India':");
        for (Match m : SearchService.searchMatchesByTeam("India")) {
            System.out.println("- " + m.getTitle() + " (" + m.getStatus() + ")");
        }

        // View commentary
        System.out.println("\nCommentary:");
        for (String line : match.getCommentary().getAll()) {
            System.out.println(line);
        }
    }
}