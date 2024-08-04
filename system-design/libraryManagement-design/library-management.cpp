#include<bits/stdc++.h>
#include <stdio.h>

using namespace std;

class Books {
private:
	int book_id;
	string author;
	string title;
	bool isAvailable = false;
	std::queue<string>lastOrders;

public:
	Books(int book_id,string author, string title, bool isAvailable) : book_id(book_id), author(author), title(title), isAvailable(isAvailable) {}

	int getBookId() {
		return book_id;
	}

	string getAuthor() {
		return author;
	}

	string getBookTitle() {
		return title;
	}

	void setAvailabilityStatus(bool status) {
		isAvailable = status;
	}

	bool getBookStatus() {
		return isAvailable;
	}

	void addMembersToQueue(string memberName) {
		if(lastOrders.size()>10) lastOrders.pop();
		lastOrders.push(memberName);
	}

	queue<string> getLast10Orders() {
		return lastOrders;
	}

	bool operator<(const Books& other) const {
		return book_id < other.book_id;
	}

};

class Member {
private:
	int member_id;
	string member_name;
	std::map<Books, bool> booksRecord ;
	queue<Books> last10Books;

public:
	Member(int member_id, string member_name) : member_id(member_id), member_name(member_name) {}

	void addBookToQueue(Books &book) {
		if(last10Books.size() >= 10) last10Books.pop();
		last10Books.push(book);
	}

	int getMemberId() {
		return member_id;
	}

	string getMemberName() {
		return member_name;
	}

	void borrowBook(Books &book) {
		if(booksRecord.find(book) != booksRecord.end()) {
			cout<<"Book is alraedy borrowed by the member"<<"/n";
			return;
		}
		booksRecord[book] = true;
	}

	void unBorrowBook(Books &book) {
		if(booksRecord.find(book) == booksRecord.end()) {
			cout<<"The user has  not borrowed the book"<<"\n";
			return;
		}
		booksRecord.erase(book);
		book.setAvailabilityStatus(true);
	}

	int getBorrowBooksCount() {
		return booksRecord.size();
	}

	queue<Books> getLast10Orders() {
		return last10Books;
	}
};

class LibraryManagement {
private:
	std::vector<Books> libraryBooks;
	std::vector<Member> libraryMembers;


public:
	void addBooksInLibrary(Books &book) {
		libraryBooks.push_back(book);
	}

	vector<Books> getLibraryBook() {
		return libraryBooks;
	}

	void addMemberToLibrary(Member &member) {
		libraryMembers.push_back(member);
	}

	vector<Member> getMemberDetails() {
		return libraryMembers;
	}

	void borrowBook(Books &book, Member &member) {
		if(book.getBookStatus() == false) {
			cout<<"Book is not available for borrowing"<<"\n";
			return;
		}
		member.borrowBook(book);
		member.addBookToQueue(book);
	}

	void unBorrowBook(Books &book, Member &member) {
		member.unBorrowBook(book);
	}

	queue<Books> getMemberBooksHistory(Member &member) {
		return member.getLast10Orders();
	}

};

int main()
{
	Books B1(1,"Harsh","JHKHKHK", true);
	Member M1(1, "Harsha bhogle");

	LibraryManagement library;
	library.borrowBook(B1,M1);

	cout<<"member borrowed the following number of books"<<M1.getBorrowBooksCount()<<"\n";
}