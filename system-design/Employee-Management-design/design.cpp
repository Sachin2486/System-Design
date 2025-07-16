#include <bits/stdc++.h>
#include <iostream>

using namespace std;

// --------- Employee Class ----------

class Employee {
    public:
    int id;
    string name;
    string department;
    double baseSalary;
    string role;
    
    Employee() : id(0), name(""), department(""), baseSalary(0.0), role("") {}
    
    Employee(int id, string name, string dept, string role, double salary) :
    id(id), name(name), department(dept), role(role), baseSalary(salary) {}
    
    void display() const {
        cout << "ID: " << id << ", Name: " << name
            << ", Dept: " << department << ", Role: " << role
            << ", Base Salary: " << baseSalary << endl;
    }
};

// --------- Payroll Manager ----------

class PayrollManager {
    public:
    double calculateSalary(const Employee& e) {
        double bonus = 0.10 * e.baseSalary;
        return e.baseSalary + bonus;
    }
    void processPayroll(const Employee& e) {
        double total = calculateSalary(e);
        cout << "[Payroll] Salary Processed for " << e.name << ": ₹" << total << endl;
    }
};

// --------- IT Manager ----------
class ITManager {
    unordered_map<int, string> emailMap;
    unordered_map<int, string> deviceMap;

public:
    void assignLaptop(Employee& e, string device) {
        deviceMap[e.id] = device;
        cout << "[IT] " << e.name << " assigned device: " << device << endl;
    }

    void createEmail(Employee& e) {
        string email = e.name + "@company.com";
        emailMap[e.id] = email;
        cout << "[IT] Email created for " << e.name << ": " << email << endl;
    }

    void revokeAccess(int empId) {
        emailMap.erase(empId);
        deviceMap.erase(empId);
        cout << "[IT] Access revoked for Employee ID: " << empId << endl;
    }
};

// --------- Benefits Manager ----------
class BenefitsManager {
    unordered_map<int, vector<string>> benefitMap;

public:
    void assignInsurance(Employee& e, string insuranceType) {
        benefitMap[e.id].push_back("Insurance: " + insuranceType);
        cout << "[Benefits] " << e.name << " assigned insurance: " << insuranceType << endl;
    }

    void applyLeave(Employee& e, int days) {
        benefitMap[e.id].push_back("Leave: " + to_string(days) + " days");
        cout << "[Benefits] " << e.name << " granted " << days << " days leave" << endl;
    }

    void showBenefits(Employee& e) {
        cout << "[Benefits] " << e.name << " has:" << endl;
        for (auto& b : benefitMap[e.id]) {
            cout << "  - " << b << endl;
        }
    }
};

// --------- Employee Management System ----------
class EmployeeManagementSystem {
    unordered_map<int, Employee> employeeMap;
    PayrollManager payrollManager;
    ITManager itManager;
    BenefitsManager benefitsManager;

public:
    void addEmployee(const Employee& e) {
        employeeMap[e.id] = e;
        cout << "[EMS] Employee added: " << e.name << endl;
    }

    void removeEmployee(int id) {
        itManager.revokeAccess(id);
        employeeMap.erase(id);
        cout << "[EMS] Employee ID " << id << " removed" << endl;
    }

    void showEmployee(int id) {
        if (employeeMap.count(id)) {
            employeeMap[id].display();
        } else {
            cout << "Employee not found." << endl;
        }
    }

    void runPayroll(int id) {
        if (employeeMap.count(id)) {
            payrollManager.processPayroll(employeeMap[id]);
        }
    }

    void manageIT(int id) {
        if (employeeMap.count(id)) {
            itManager.createEmail(employeeMap[id]);
            itManager.assignLaptop(employeeMap[id], "Dell Latitude 7420");
        }
    }

    void manageBenefits(int id) {
        if (employeeMap.count(id)) {
            benefitsManager.assignInsurance(employeeMap[id], "Health + Dental");
            benefitsManager.applyLeave(employeeMap[id], 15);
            benefitsManager.showBenefits(employeeMap[id]);
        }
    }
};

int main() {
    EmployeeManagementSystem ems;

    Employee e1(1, "Sachin", "Engineering", "SDE 2", 100000);
    Employee e2(2, "Ravi", "HR", "HR Manager", 70000);

    ems.addEmployee(e1);
    ems.addEmployee(e2);

    ems.showEmployee(1);
    ems.runPayroll(1);
    ems.manageIT(1);
    ems.manageBenefits(1);

    ems.removeEmployee(1);

    return 0;
}
