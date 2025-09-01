import java.util.Scanner;

public class TicTacToe {
    private static char[][] board = new char[3][3];
    
    private static char currentPlayer = 'X';
    
    public static void main(String[] args) {
        
        Scanner scanner = new Scanner(System.in);
        
        initializeBoard();
        
        while(true) {
            printBoard();
            System.out.println("Player" + currentPlayer + ", enter your move (row and column: 0, 1, or 2): ");
            
            int row = scanner.nextInt();
            int col = scanner.nextInt();
            
            if (row < 0 || row > 2 || col < 0 || col > 2) {
                System.out.println("Invalid position! Try again.");
                continue; // Ask again
            }
            
            
            if(board[row][col] != ' ') {
                System.out.println("Cell already occupied Try again.");
                continue;
            }
            
            board[row][col] = currentPlayer;
            
            if(checkWinner()) {
                printBoard();
                System.out.println("Player " + currentPlayer + " wins!");
                break;
            }
            
            if(isBoardFull()) {
                printBoard();
                System.out.println("It's a draw");
                break;
            }
            
            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        }
        
        scanner.close();
    }
    
    // Initialize the board with empty spaces
    private static void initializeBoard() {
        for(int i=0; i < 3; i++) {
            for(int j=0; j < 3 ;j++){
                board[i][j] = ' ';
            }
        }
    }
    
    // Print the current board
    private static void printBoard() {
        System.out.println("Board:");
        for (int i = 0; i < 3; i++) {
            System.out.print(" ");
            for (int j = 0; j < 3; j++) {
                System.out.print(board[i][j]);
                if (j < 2) System.out.print(" | "); // Column separator
            }
            System.out.println();
            if (i < 2) System.out.println("---+---+---"); // Row separator
        }
    }
    
    private static boolean checkWinner() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == currentPlayer && 
                board[i][1] == currentPlayer && 
                board[i][2] == currentPlayer) {
                return true;
            }
        }

        // Check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == currentPlayer && 
                board[1][j] == currentPlayer && 
                board[2][j] == currentPlayer) {
                return true;
            }
        }

        // Check diagonals
        if (board[0][0] == currentPlayer && 
            board[1][1] == currentPlayer && 
            board[2][2] == currentPlayer) {
            return true;
        }
        
        if (board[0][2] == currentPlayer && 
            board[1][1] == currentPlayer && 
            board[2][0] == currentPlayer) {
            return true;
        }

        return false;
}

private static boolean isBoardFull() {
    for(int i = 0; i < 3; i++) {
        for(int j = 0; j < 3;j++){
            if(board[i][j] == ' '){
                return false;
            }
        }
    }
    return true;
}
}
