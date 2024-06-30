#include<bits/stdc++.h>
using namespace std;

class Player{
    private:
    string name;
    char piece;

    public:
    Player(string name, char piece) : name(name), piece(piece) {}
    
    char getPiece() {
        return piece;
    }

    string getName() {
        return name;
    }
};

class Board{
    private:
    vector<vector<char>> grid;
    int size;

    public:
    Board(int size): size(size){
        grid.resize(size, vector<char>(size,' '));
    }

    bool makeMove(int row,int col, char piece){
        if(row>=0 && row<size && col>=0 && col < size && grid[row][col] == ' '){
            grid[row][col] = piece;
            return true;
        }
        return false;
    }

    bool checkWin(char piece){
        for(int i=0;i<size;i++){
            bool rowWin = true;
            bool colWin = true;
            for(int j=0;j<size;j++){
                if(grid[i][j] != piece)
                rowWin = false;
                if(grid[i][j] != piece)
                colWin = false;
            }
            if(rowWin || colWin)
            return true;
        }

        bool diagRowWin =  true;
        bool diagColWin = true;
        for(int i=0;i<size;i++){
            if(grid[i][i] != piece) diagRowWin = false;
            if(grid[i][size-1-i] != piece) diagColWin = false;
        }
        if(diagRowWin || diagColWin) return true;
        return false;
    }

     bool isFull() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == ' ') return false;
            }
        }
        return true;
    }

    void printBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cout << grid[i][j];
                if (j < size - 1) cout << " | ";
            }
            cout << "\n";
            if (i < size - 1) cout << string(size * 4 - 1, '-') << "\n";
        }
        cout << "\n";
    }
};

class Game{
    private:
    Board board;
    vector<Player> players;
    int currentPlayerIndex;

    public:
    Game(int boardSize, vector<Player> allPlayers) : board(boardSize), players(allPlayers), currentPlayerIndex(0){}

    void play() {
        while(true){
            board.printBoard();
            Player currentPlayer = players[currentPlayerIndex];
            cout << currentPlayer.getName() << "'s turn (" << currentPlayer.getPiece() << "):\n";

            int row, col;
            cout<<"Enter the row and column:";
            cin>>row>>col;

            if(board.makeMove(row,col,currentPlayer.getPiece())){
                if(board.checkWin(currentPlayer.getPiece())){
                    board.printBoard();
                    cout<<currentPlayer.getName() <<"wins!\n";
                    break;
                }
                else if(board.isFull()){
                    board.printBoard();
                    cout<<"the game is a draw!\n";
                    break;
                }
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();

            }
            else{
                cout<<"Invalid move. Try again. \n";
            }
        }
    }

};

int main(){
   vector<Player> players;
    Player p1("Player 1", 'X');
    Player p2("Player 2", 'O');

    players.push_back(p1);
    players.push_back(p2);

    int boardSize = 3;

    Game game(boardSize, players);
    game.play();

    return 0;


};