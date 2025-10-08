import java.util.*;

// Enum for player symbols
enum Symbol {
    X, O
}

// Player class representing each player
class Player {
    private String name;
    private Symbol symbol;
    
    public Player(String name, Symbol symbol) {
        this.name = name;
        this.symbol = symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
}

// Board class representing the 3x3 grid
class Board {
    private Symbol[][] grid;
    private static final int SIZE = 3;
    
    public Board() {
        grid = new Symbol[SIZE][SIZE];
    }
    
    // Display the board
    public void display() {
        System.out.println("\nCurrent Board:");
        for(int i=0;i<SIZE; i++) {
            for(int j = 0; j < SIZE; j++) {
                System.out.print((grid[i][j] == null ? "-" : grid[i][j]) + " ");
            }
            System.out.println();
        }
    }
    
    // Make a move on the board
    public boolean makeMove(int row, int col, Symbol symbol) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE || grid[row][col] != null) {
            return false;
        }
        grid[row][col] = symbol;
        return true;
    }
    
    // Check if a player has won
    public boolean checkWinner(Symbol symbol) {
        // Check rows and columns
        for (int i = 0; i < SIZE; i++) {
            if ((grid[i][0] == symbol && grid[i][1] == symbol && grid[i][2] == symbol) ||
                (grid[0][i] == symbol && grid[1][i] == symbol && grid[2][i] == symbol)) {
                return true;
            }
        }
        // Check diagonals
        return (grid[0][0] == symbol && grid[1][1] == symbol && grid[2][2] == symbol) ||
               (grid[0][2] == symbol && grid[1][1] == symbol && grid[2][0] == symbol);
    }

    // Check if the board is full
    public boolean isFull() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == null) return false;
            }
        }
        return true;
    }
}

// Game class managing the game flow
class Game {
    private Player player1;
    private Player player2;
    private Board board;
    private Player currentPlayer;
    private Scanner scanner;

    public Game(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new Board();
        this.currentPlayer = player1;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("Welcome to Tic-Tac-Toe!");
        board.display();

        while (true) {
            System.out.println("\n" + currentPlayer.getName() + "'s turn (" + currentPlayer.getSymbol() + ")");
            System.out.print("Enter row and column (0-2): ");

            int row = scanner.nextInt();
            int col = scanner.nextInt();

            if (!board.makeMove(row, col, currentPlayer.getSymbol())) {
                System.out.println("Invalid move! Try again.");
                continue;
            }

            board.display();

            if (board.checkWinner(currentPlayer.getSymbol())) {
                System.out.println("\n🎉 " + currentPlayer.getName() + " wins!");
                break;
            }

            if (board.isFull()) {
                System.out.println("\nIt's a draw!");
                break;
            }

            switchTurn();
        }
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
    }
}

// Entry point
public class TicTacToe {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter name for Player 1 (X): ");
        String name1 = scanner.nextLine();
        
        System.out.print("Enter name for Player 2 (O): ");
        String name2 = scanner.nextLine();
        
        Player player1 = new Player(name1, Symbol.X);
        Player player2 = new Player(name2, Symbol.O);
        
        Game game = new Game(player1, player2);
        game.start();
    }
}



