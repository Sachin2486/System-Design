#include<bits/stdc++.h>
using namespace std;

enum class ColumnType {
	INT,
	STRING
};

class Column {
private:
	string name;
	ColumnType type;
	bool is_required;
	int max_length; // for string
	int min_value; // for integer
	int max_value;

public:
	Column(const string &name, ColumnType type, bool is_required = false, int max_length = 20, int min_value = -1024, int max_value = 1024) :
		name(name), type(type), is_required(is_required), max_length(max_length),
		min_value(min_value), max_value(max_value) {}

	void validate(const string& value) const {
		if(type == ColumnType::STRING) {
			if(value.length() > max_length) {
				throw runtime_error("String exceeds maximum length of " + to_string(max_length));
			}
		} else if(type == ColumnType::INT) {
			try {
				int int_value = stoi(value);
				if(int_value < min_value || int_value > max_value) {
					throw runtime_error("Integer value out of bounds (" + to_string(min_value) + " to " + to_string(max_value) + ")");
				}
			} catch (const invalid_argument&) {
				throw runtime_error("Invalid integer value: " + value);
			} catch (const out_of_range&) {
				throw runtime_error("Integer value is out of range: " + value);
			}
		}
	}

	ColumnType getType() const {
		return type;
	}

	bool isRequired() const {
		return is_required;
	}

	string getName() const {
		return name;
	}
};

class Record {
private:
	unordered_map<string, string> values;

public:
	void setValue(const string &columnName, const string &value) {
		values[columnName] = value;
	}

	string getValue(const string &columnName) const {
		auto it = values.find(columnName);
		if(it != values.end()) {
			return it->second;
		}
		throw runtime_error("Column not found: " + columnName);
	}

	void print() const {
		for(const auto& pair : values) {
			cout << pair.first << ": " << pair.second << " ";
		}
		cout << endl;
	}
};

class Table {
private:
	string name;
	vector<Column> columns;
	vector<Record> records;

public:
	Table(const string& name) : name(name) {}

	void addColumn(const Column& column) {
		columns.push_back(column);
	}

	void insertRecord(const vector<string>& values) {
		if (values.size() != columns.size()) {
			throw runtime_error("Mismatch between number of columns and values provided");
		}

		Record new_record;
		for (size_t i = 0; i < columns.size(); ++i) {
			if (columns[i].isRequired() && values[i].empty()) {
				throw runtime_error("Mandatory column " + columns[i].getName() + " is missing");
			}
			columns[i].validate(values[i]);
			new_record.setValue(columns[i].getName(), values[i]);
		}

		records.push_back(new_record);
	}

	void printRecords() const {
		for(const auto& record : records) {
			record.print();
		}
	}

	string getName() const {
		return name;
	}
};

class Database {
private:
	unordered_map<string, Table> tables;

public:
	void createTable(const string& table_name) {
		if (tables.find(table_name) != tables.end()) {
			throw runtime_error("Table already exists: " + table_name);
		}
		tables.emplace(table_name, Table(table_name));
		cout << "Table " << table_name << " created successfully." << endl;
	}

	void deleteTable(const string& table_name) {
		if (tables.find(table_name) == tables.end()) {
			throw runtime_error("Table not found: " + table_name);
		}
		tables.erase(table_name);
		cout << "Table " << table_name << " deleted successfully." << endl;
	}

	Table& getTable(const string& table_name) {
		if (tables.find(table_name) == tables.end()) {
			throw runtime_error("Table not found: " + table_name);
		}
		return tables.at(table_name);
	}
};

int main() {
    Database db;

    try {
        db.createTable("students");

        // Get the students table
        Table &students = db.getTable("students");

        students.addColumn(Column("ID", ColumnType::INT, true));
        students.addColumn(Column("Name", ColumnType::STRING, true));
        students.addColumn(Column("Age", ColumnType::INT));

        students.insertRecord({"1", "Alice", "22"});
        students.insertRecord({"2", "Bob", "19"});

        students.printRecords();

        db.deleteTable("students");
    } catch (const std::exception& e) {
        cerr << "Error: " << e.what() << endl;
    }

    return 0;
}
