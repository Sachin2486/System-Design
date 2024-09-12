#include<bits/stdc++.h>

using namespace std;

class Employee {
public:
    string emp_name;
    string emp_email;
    int level;

    Employee(string &emp_name, string emp_email, int level) :
        emp_name(emp_name), emp_email(emp_email), level(level) {}
};

class Incident {
public:
    int id;
    string details;
    int currentLevel;

    Incident(int id, string details, int currentLevel) :
        id(id), details(details), currentLevel(currentLevel) {}
};

class Project {
public:
    string name;
    map<int,Employee*> levelToEmployee;
    map<int,Incident*> incidents;
    int incidentCount = 0;

    Project(string name) : name(name) {}

    string getName() {
        return name;
    }

    bool addEmployee(Employee* employee) {
        if (levelToEmployee.count(employee->level)) {
            cout << "Error: Level " << employee->level << " already assigned to "
                 << levelToEmployee[employee->level]->emp_name << endl;
            return false;
        }
        levelToEmployee[employee->level] = employee;
        return true;
    }

    Incident* createIncident(string incidentDetails) {
        int incidentId = ++incidentCount;
        Incident* newIncident = new Incident(incidentId, incidentDetails, 1);
        incidents[incidentId] = newIncident;
        return newIncident;
    }

    bool notifyIncident(int incidentId) {
        if (incidents.count(incidentId)) {
            Incident* incident = incidents[incidentId];
            Employee* emp = levelToEmployee[incident->currentLevel];
            if (emp) {
                cout << "Notifying " << emp->emp_name << " (Level " << emp->level << ")" << endl;
                return true;
            } else {
                cout << "Error: No employee found at level " << incident->currentLevel << endl;
                return false;
            }
        } else {
            cout << "Error: Incident not found" << endl;
            return false;
        }
    }

    bool escalateIncident(int incidentId) {
        if (incidents.count(incidentId)) {
            Incident* incident = incidents[incidentId];
            incident->currentLevel++;
            if (levelToEmployee.count(incident->currentLevel)) {
                notifyIncident(incidentId);
                return true;
            } else {
                cout << "Error: No more levels to escalate in project " << name << endl;
                return false;
            }
        }
        return false;
    }

    bool ackIncident(int incidentId, int level) {
        if (incidents.count(incidentId)) {
            Incident* incident = incidents[incidentId];
            if (incident->currentLevel == level) {
                cout << "Incident " << incidentId << " acknowledged by employee at level " << level << endl;
                return true;
            } else {
                cout << "Error: Incident " << incidentId << " is not at level " << level << endl;
                return false;
            }
        } else {
            cout << "Error: Incident not found" << endl;
            return false;
        }
    }
};

class IncidentAlertingSystem {
private:
    map<string, Project*> projects;
    map<string, Employee*> employees;

public:
    bool addProject(string projectName) {
        if (projects.count(projectName)) {
            cout << "Error: Project already exists" << endl;
            return false;
        }
        projects[projectName] = new Project(projectName);
        return true;
    }

    bool addEmployee(string empName, string email, int level) {
        if (employees.count(empName)) {
            cout << "Error: Employee already exists" << endl;
            return false;
        }
        employees[empName] = new Employee(empName, email, level);
        return true;
    }

    bool assignProject(string projectName, string empName) {
        if (projects.count(projectName) && employees.count(empName)) {
            return projects[projectName]->addEmployee(employees[empName]);
        } else {
            cout << "Error: Project or Employee does not exist" << endl;
            return false;
        }
    }

    int createIncident(string projectName, string incidentDetails) {
        if (projects.count(projectName)) {
            Incident* newIncident = projects[projectName]->createIncident(incidentDetails);
            return newIncident->id;
        } else {
            cout << "Error: Project does not exist" << endl;
            return -1;
        }
    }

    bool notifyIncident(string projectName, int incidentId) {
        if (projects.count(projectName)) {
            return projects[projectName]->notifyIncident(incidentId);
        } else {
            cout << "Error: Project does not exist" << endl;
            return false;
        }
    }

    bool escalateIncident(string projectName, int incidentId) {
        if (projects.count(projectName)) {
            return projects[projectName]->escalateIncident(incidentId);
        } else {
            cout << "Error: Project does not exist" << endl;
            return false;
        }
    }

    bool ackIncident(string projectName, int incidentId, int level) {
        if (projects.count(projectName)) {
            return projects[projectName]->ackIncident(incidentId, level);
        } else {
            cout << "Error: Project does not exist" << endl;
            return false;
        }
    }
};

// Usage
int main() {
    IncidentAlertingSystem system;

    // Adding projects and employees
    system.addProject("proj1");
    system.addEmployee("emp1", "emp1@gmail.com", 1);
    system.addEmployee("emp2", "emp2@gmail.com", 2);
    system.addEmployee("emp3", "emp3@gmail.com", 3);

    // Assign employees to project
    system.assignProject("proj1", "emp1");
    system.assignProject("proj1", "emp2");
    system.assignProject("proj1", "emp3");

    // Create an incident
    int incidentId = system.createIncident("proj1", "Server outage");

    // Notify employee at level 1
    system.notifyIncident("proj1", incidentId);

    // Escalate to next level
    system.escalateIncident("proj1", incidentId);

    // Acknowledge by level 1 employee
    system.ackIncident("proj1", incidentId, 1);

    return 0;
}
