#include <bits/stdc++.h>
using namespace std;

class Players
{
private:
    string name;
    int position;

public:
    Players(string name, int position) : name(name), position(position) {}

    int getPosition()
    {
        return position;
    }

    void setPosition(int newPosition)
    {
        position = newPosition;
    }

    string getName()
    {
        return name;
    }
};

class Board
{
private:
    int size;
    vector<int> snakesLadder;

public:
    Board(int size) : size(size)
    {
        snakesLadder.resize(size, 0);
    }

    void setSnakeLadder(int startPosition, int endPosition)
    {
        snakesLadder[startPosition] = endPosition;
    }

    int getNextPosition(int newPosition)
    {
        return newPosition + snakesLadder[newPosition];
    }

    int getBoardSize()
    {
        return size;
    }

    bool isWinning(int position)
    {
        return position == size - 1;
    }
};

class Dice
{
public:
    int roll()
    {
        return rand() % 6 + 1;
    }
};

class Game
{
private:
    Board board;
    vector<Players> players;
    Dice dice;

public:
    Game(int boardSize, int numberOfPlayer, vector<Players> allPlayers) : board(boardSize), players(allPlayers)
    {
        board.setSnakeLadder(21, 5);
        board.setSnakeLadder(5, 3);
        board.setSnakeLadder(7, 20);
        board.setSnakeLadder(9, 6);
    }

    void play()
    {
        cout << "total players" << players.size() << "\n";
        int boardSize = board.getBoardSize();
        cout << "total players" << players.size() << "\n";
        while (true)
        {
            for (int i = 0; i < players.size(); i++)
            {
                int roll = dice.roll();
                int currentPosition = players[i].getPosition();

                int newPosition = currentPosition + roll;

                if (newPosition < boardSize)
                {
                    newPosition = board.getNextPosition(newPosition);
                    players[i].setPosition(newPosition);

                    cout << players[i].getName() << "moved to position : " << newPosition << "\n";
                    if (board.isWinning(newPosition))
                    {
                        cout << players[i].getName() << "Wins the Game\n";
                        return;
                    }
                    cout << "\n";
                }
            }
        }
    }
};

int main()
{
    vector<Players> players;
    int numberOfPlayer = 2;

    Players p1("Sachin", 0);
    Players p2("John", 0);

    players.push_back(p1);
    players.push_back(p2);

    int boardSize = 38;
    cout << "total number of players" << players.size() << "\n";
    Game game(boardSize, numberOfPlayer, players);
    game.play();

    return 0;
};