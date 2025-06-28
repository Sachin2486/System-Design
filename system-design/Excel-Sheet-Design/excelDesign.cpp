#include <iostream>
#include <unordered_map>
#include <unordered_set>
#include <sstream>
#include <functional>
#include <stack>
#include <cctype>
using namespace std;

// ---------------------- Cell ----------------------
class Cell {
public:
	string formula;
	double value;
	bool isFormula;

	Cell() : value(0), isFormula(false) {}
};

// ---------------------- Cell DAO ----------------------
class CellDao {
private:
	unordered_map<string, Cell> cells;

public:
	void saveCell(const string& name, const Cell& cell) {
		cells[name] = cell;
	}

	Cell* getCell(const string& name) {
		if (cells.count(name)) return &cells[name];
		return nullptr;
	}

	unordered_map<string, Cell>& getAllCells() {
		return cells;
	}
};

// ---------------------- Expression Evaluator ----------------------
class ExpressionEvaluator {
public:
	static double evaluate(const string& expr, function<double(const string&)> resolveCell) {
		istringstream in(expr);
		return parseExpression(in, resolveCell);
	}

private:
	static double parseExpression(istringstream& in, function<double(const string&)> resolveCell) {
		stack<double> values;
		stack<char> ops;
		double num;
		string token;

		auto applyOp = [](double a, double b, char op) -> double {
			switch (op) {
			case '+':
				return a + b;
			case '-':
				return a - b;
			case '*':
				return a * b;
			case '/':
				return b != 0 ? a / b : 0;
			}
			return 0;
		};

		while (in >> ws && !in.eof()) {
			char ch = in.peek();

			if (isdigit(ch)) {
				in >> num;
				values.push(num);
			} else if (isalpha(ch)) {
				token = "";
				while (isalnum(in.peek())) {
					token += in.get();
				}
				values.push(resolveCell(token));
			} else if (ch == '(') {
				in.get();
				values.push(parseExpression(in, resolveCell));
			} else if (ch == ')') {
				in.get();
				break;
			} else if (ch == '+' || ch == '-' || ch == '*' || ch == '/') {
				char op;
				in >> op;
				while (!ops.empty() && precedence(ops.top()) >= precedence(op)) {
					double b = values.top();
					values.pop();
					double a = values.top();
					values.pop();
					values.push(applyOp(a, b, ops.top()));
					ops.pop();
				}
				ops.push(op);
			} else {
				in.get();
			}
		}

		while (!ops.empty()) {
			double b = values.top();
			values.pop();
			double a = values.top();
			values.pop();
			values.push(applyOp(a, b, ops.top()));
			ops.pop();
		}

		return values.empty() ? 0 : values.top();
	}

	static int precedence(char op) {
		if (op == '+' || op == '-') return 1;
		if (op == '*' || op == '/') return 2;
		return 0;
	}
};

// ---------------------- Spreadsheet ----------------------
class Spreadsheet {
private:
	CellDao dao;
	unordered_map<string, unordered_set<string>> forwardDeps;
	unordered_map<string, unordered_set<string>> backwardDeps;

public:
	void setCell(const string& cellName, const string& input) {
		Cell* cell = dao.getCell(cellName);
		if (!cell) {
			dao.saveCell(cellName, Cell());
			cell = dao.getCell(cellName);
		}

		clearDependencies(cellName);

		if (!input.empty() && input[0] == '=') {
			cell->formula = input.substr(1);
			cell->isFormula = true;
		} else {
			cell->formula = "";
			cell->value = stod(input);
			cell->isFormula = false;
		}

		dao.saveCell(cellName, *cell);
		unordered_set<string> visiting;
		evaluateCell(cellName, visiting);
		updateDependents(cellName);
	}

	string getCellValue(const string& cellName) {
		Cell* cell = dao.getCell(cellName);
		return cell ? to_string(cell->value) : "0";
	}

private:
	double evaluateCell(const string& cellName, unordered_set<string>& visiting) {
		if (visiting.count(cellName)) {
			throw runtime_error("Circular dependency detected at " + cellName);
		}

		visiting.insert(cellName);
		Cell* cell = dao.getCell(cellName);
		if (!cell) {
			dao.saveCell(cellName, Cell());
			cell = dao.getCell(cellName);
		}

		if (!cell->isFormula) {
			visiting.erase(cellName);
			return cell->value;
		}

		double val = ExpressionEvaluator::evaluate(cell->formula, [&](const string& ref) -> double {
			Cell* refCell = dao.getCell(ref);
			if (!refCell) {
				dao.saveCell(ref, Cell());
				refCell = dao.getCell(ref);
			}
			addDependency(ref, cellName);
			unordered_set<string> nestedVisiting = visiting; // prevent shared mutation
			return evaluateCell(ref, nestedVisiting);
		});

		cell->value = val;
		dao.saveCell(cellName, *cell);
		visiting.erase(cellName);
		return val;
	}

	void addDependency(const string& from, const string& to) {
		forwardDeps[from].insert(to);
		backwardDeps[to].insert(from);
	}

	void clearDependencies(const string& cellName) {
		for (const auto& dep : backwardDeps[cellName]) {
			forwardDeps[dep].erase(cellName);
		}
		backwardDeps[cellName].clear();
	}

	void updateDependents(const string& cellName) {
		unordered_set<string> visited;
		dfsUpdate(cellName, visited);
	}

	void dfsUpdate(const string& cellName, unordered_set<string>& visited) {
		if (visited.count(cellName)) return;
		visited.insert(cellName);

		for (const auto& dep : forwardDeps[cellName]) {
			unordered_set<string> dummy;
			evaluateCell(dep, dummy);
			dfsUpdate(dep, visited);
		}
	}
};

// ---------------------- Demo ----------------------
int main() {
	Spreadsheet sheet;

	sheet.setCell("A1", "10");
	sheet.setCell("A2", "20");
	sheet.setCell("B1", "=A1+A2");
	sheet.setCell("C1", "=B1*2");

	cout << "B1: " << sheet.getCellValue("B1") << endl;
	cout << "C1: " << sheet.getCellValue("C1") << endl;

	sheet.setCell("A1", "40");

	cout << "After updating A1 = 40" << endl;
	cout << "B1: " << sheet.getCellValue("B1") << endl;
	cout << "C1: " << sheet.getCellValue("C1") << endl;

	return 0;
}
