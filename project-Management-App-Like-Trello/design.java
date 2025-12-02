import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class IdGenerator {
    private static final AtomicInteger userGen = new AtomicInteger(1);
    private static final AtomicInteger boardGen = new AtomicInteger(1);
    private static final AtomicInteger listGen = new AtomicInteger(1);
    private static final AtomicInteger cardGen = new AtomicInteger(1);
    
    public static int nextUserId() {
        return userGen.getAndIncrement();
    }
    
    public static int nextBoardId() {
        return boardGen.getAndIncrement();
    }
    
    public static int nextListId() {
        return listGen.getAndIncrement();
    }
    
    public static int nextCardId() {
        return cardGen.getAndIncrement();
    }
    
}

/// Entities /// 

class User {
    private final int userId;
    private final String name;
    private final String email;
    
    public User(String name,String email) {
        this.userId = IdGenerator.nextUserId();
        this.name = name;
        this.email = email;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    public String toString() {
        return "User{" + userId + "," + name + "}";
    }
}

enum Privacy {
    PUBLIC,
    PRIVATE
}

class Card {
    
    private final int cardId;
    private String name;
    private String description;
    private User assignedUser;
    
    public Card(String name,String description) {
        this.cardId = IdGenerator.nextCardId();
        this.name = name;
        this.description = description;
        this.assignedUser = null; // unassigned by default in start
    }
    
    public int getCardid() {
        return cardId;
    }
    
    public void assignUser (User u) {
        this.assignedUser = u;
    }
    
    public void unassignUser() {
        this.assignedUser = null;
    }
    
    public String toString() {
        return "Card {" + cardId + "," + name + ",assigned=" + (assignedUser != null ? assignedUser.getName() : "none");
    }
}

class BoardList {
    // Each list should have id , name and Cards
    private final int listId;
    private String name;
    private final List<Card> cards = new ArrayList<>();
    
    public BoardList(String name) {
        this.listId = IdGenerator.nextListId();
        this.name = name;
    }
    
    public int getListId() {
        return listId;
    }
    
    public List<Card> getCards() {
        return cards;
    }
    
    public void addCard(Card c) {
        cards.add(c);
    }
    
    public void removeCard(Card c) {
        cards.remove(c);
    }
    
    public String toString() {
        return "List{" + listId + "," + name + "}";
    }
}

class Board {
    // req : id , name ,  private , url , members , lists
    
    private final int boardId;
    private String name;
    private Privacy privacy;
    private String url;
    private final List<User> members = new ArrayList<>();
    private final List<BoardList> lists = new ArrayList<>();
    
    public Board(String name, Privacy privacy) {
        this.boardId = IdGenerator.nextBoardId();
        this.name = name;
        this.privacy = privacy;
        this.url = "/board/" + boardId; // req : Url created based on ID of board
    }
    
    public int getBoardId() {
        return boardId;
    }
    
    public List<BoardList> getLists() {
        return lists;
    }
    
    public List<User> getMembers() {
        return members;
    }
    
    public void addMember(User u) {
        members.add(u);
    }
    
    public void removeUser(User u) {
        members.remove(u);
    }
    
    public void addList(BoardList l) {
        lists.add(l);
    }
    
    public void removeList(BoardList l) {
        lists.remove(l);
    }
    
    public String toString() {
        return "Board{" + boardId + "," + name + "," + privacy + "}";
    }
}

// Services 

class CardService {
    // req :  create/ delete cards , assign / unassign user , modify attributes
    
    public Card createCard(String name, String desc) {
        return new Card(name, desc);
    }
    
    public void deleteCard(BoardList list, int cardId) {
        list.getCards().removeIf(c -> c.getCardid() == cardId);
    }
    
    public void assignUser(Card c, User u) {
        c.assignUser(u);
    }
    
    public void unassignUser(Card c) {
        c.unassignUser();
    }
    
    public void moveCard(BoardList from, BoardList to, Card card) {
        from.removeCard(card);
        to.addCard(card);
    }
}

class ListService {
    
    private final CardService cardService;
    
    public ListService(CardService cardService) {
        this.cardService = cardService;
    }
    
    // Req : create / delete lists , modify attributes
    
    public BoardList createList(String name) {
        return new BoardList(name);
    }
    
    public void deleteList(Board board, int listId) {
        BoardList target = board.getLists()
        .stream().filter(l -> l.getListId() == listId).findFirst().orElse(null);
        
        if (target != null) {
            // req : deleting this 
            
            target.getCards().clear();
            board.removeList(target);
        }
    }
}

class BoardService {
    
    private final Map<Integer, Board> boards = new ConcurrentHashMap<>();
    private final CardService cardService = new CardService();
    private final ListService listService = new ListService(cardService);
    
    // REq :  create , delete boards , add /remove people , modify attributes
    
    public Board createBoard(String name, Privacy p) {
        Board board = new Board(name, p);
        boards.put(board.getBoardId(),board);
        return board;
    }
    
    public void deleteBoard(int boardId) {
        Board b = boards.get(boardId);
        if(b != null) {
            b.getLists().clear();
            b.getMembers().clear();
            boards.remove(boardId);
        }
    }
    
    public BoardList addListToBoard(int boardId, String listName) {
        Board b = boards.get(boardId);
        if(b == null) return null;
        
        BoardList newList = listService.createList(listName);
        b.addList(newList);
        return newList;
    }
    
    public Card addCardToList(int boardId, int listId, String cardName, String desc) {
        Board b = boards.get(boardId);
        
        if(b == null) return null;
        
        BoardList list = b.getLists().stream()
        .filter(l -> l.getListId() == listId)
        .findFirst().orElse(null);
        
        if (list == null) return null;
        
        Card card = cardService.createCard(cardName, desc);
        list.addCard(card);
        return card;
    }
    
    public void moveCard(int boardId, int fromListId, int toListId, int  cardId) {
        
        Board b = boards.get(boardId);
        
        if(b == null) return;
        
        BoardList from = b.getLists().stream()
        .filter(l -> l.getListId() == fromListId)
        .findFirst().orElse(null);
        
        BoardList to = b.getLists().stream()
        .filter(l -> l.getListId() == toListId)
        .findFirst().orElse(null);
        
        
        if(from == null || to == null) return;
        
        Card card = from.getCards().stream()
        .filter(c -> c.getCardid() == cardId).findFirst().orElse(null);
        
        if(card != null) {
            cardService.moveCard(from, to,card);
        }
    }
    
    public Board getBoard(int id) {
        return boards.get(id);
    }
}

public class TrelloDemo
{
	public static void main(String[] args) {
		
		
		BoardService boardService = new BoardService();
		
		// Creating a Users
		
		User sachin = new User("Sachin", "sachin@mail.com");
		User rohit = new User("Rohit", "rohit@mail.com");
		
		// Create BoardPrivacy
		
		Board b1 = boardService.createBoard("Project Content", Privacy.PUBLIC);
		
		// Add list
		BoardList todo = boardService.addListToBoard(b1.getBoardId(), "To Do");
		BoardList doing = boardService.addListToBoard(b1.getBoardId(), "In Progress");
		
		// Add card
		
		Card c1 = boardService.addCardToList(b1.getBoardId(), todo.getListId(),
		"Fix Login Bug", "Users cannot login");
		
		// Assign to Users
		
		c1.assignUser(sachin);
		
		//Move Card
		boardService.moveCard(b1.getBoardId(), todo.getListId(),doing.getListId(), c1.getCardid());
		
		// print the board status here 
		
		System.out.println("\n == Board State ==");
		for(BoardList list : b1.getLists()){
		    
		    System.out.println(list);
		    for(Card card : list.getCards()) {
		        System.out.println(" " + card); 
		    }
		}
	}
}
