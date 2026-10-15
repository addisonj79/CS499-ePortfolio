//============================================================================
// Name        : Project.cpp
// Author      : Addison Janovich
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================
#include <iostream>
#include <vector>
#include <string>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <cctype>


using namespace std;

// Course Structure

struct Course{
	string number;
	string title;
	vector<string> prerequisites;
};

static void trimInPlace(string& text){
	const string whitespace = " \t\r\n";
	size_t start = text.find_first_not_of(whitespace);
	if (start == string::npos) {
		text.clear();
		return;
	}
	size_t end = text.find_last_not_of(whitespace);
	text = text.substr(start, end-start + 1);
}

static string normalizeCourseNumber(string value){
	trimInPlace(value);
	for (char& ch : value){
		ch = static_cast<char>(toupper(static_cast<unsigned char>(ch)));
	}
	return value;
}
static vector<string> splitCsvLine(const string& line){
	vector<string> tokens;
	string token;
	stringstream ss(line);

	while (getline(ss, token, ',')){
		trimInPlace(token);
		tokens.push_back(token);
	}
	return tokens;
}
static const Course* findCourse(const vector<Course>& courses, const string& courseNumber){
	for (const Course& course : courses){
		if (course.number == courseNumber){
			return &course;
		}
	}
	return nullptr;
}
// load file
static bool loadCoursesFromFile(const string& fileName, vector<Course>& courses){
	std::ifstream input(fileName);
	if (!input.is_open()){
		cout << "Error: Could not open file \"" << fileName << "\"."<< endl;
		return false;
	}

	vector<Course> tempCourses;
	string line;
	size_t lineNumber = 0;

	while (getline(input, line)){
		++lineNumber;
		trimInPlace(line);
		if (line.empty()){
			continue;
		}
		vector<string> fields = splitCsvLine(line);
		if (fields.size() < 2){
			cout << "Error: Invalid format at line " << lineNumber << "." << endl;
			return false;
		}
		Course course;
		course.number = normalizeCourseNumber(fields[0]);
		course.title = fields[1];
		trimInPlace(course.title);

		if (course.number.empty() || course.title.empty()){
			cout << "Error: Missing course number or title at line " << lineNumber << "." << endl;
			return false;
		}
		for (size_t i = 2; i < fields.size(); ++i){
			string prereq = normalizeCourseNumber(fields[i]);
			if (!prereq.empty()){
				course.prerequisites.push_back(prereq);
			}
		}
		tempCourses.push_back(course);
	}
	for (const Course& course : tempCourses){
		for (const string& prereq : course.prerequisites) {
			if (findCourse(tempCourses, prereq) == nullptr){
				cout << "Error: Course" << course.number
					<< " has invalid prerequisite " << prereq << "." << endl;
				return false;
			}
		}
	}
	courses = tempCourses;
	cout << "Loaded " << courses.size() << " courses." << endl;
	return true;
}

static int readMenuChoice(){
	string input;
	getline(cin, input);
	trimInPlace(input);

	if (input.empty()){
		return -1;
	}
	int value = 0;
	for (char c : input){
		if(!isdigit(static_cast<unsigned char>(c))) return - 1;
		value = value * 10 + (c- '0');
	}
	return value;

}
static void printCourseList(const vector<Course>& courses){
	vector<Course> sorted = courses;
	sort(sorted.begin(), sorted.end(), [](const Course& a, const Course& b){
		return a.number < b.number;
	});
	cout << "Here is a sample schedule:" << endl;
	for(const Course& course : sorted){
		cout << course.number << ", " << course.title << endl;
	}
}

static void printCourseInformation(const vector<Course>& courses, const string& courseInput){
	string courseNumber = normalizeCourseNumber(courseInput);
	const Course* course = findCourse(courses, courseNumber);
	if (course == nullptr){
		cout << "Course "<< courseNumber << " not found." << endl;
		return;
	}
	cout << course->number << ", " << course-> title << endl;
	cout << "Prerequisites: ";
	if (course->prerequisites.empty()){
		cout << "None" << endl;
		return;
	}
	for (size_t i = 0; i < course->prerequisites.size(); ++i) {
		cout << course->prerequisites[i];
		if (i + 1 < course->prerequisites.size()){
			cout << ", ";
		}
	}
	cout << endl;
}
int main() {
	vector<Course> courses;
	bool loaded = false;

	cout << "Welcome to the course planner." << endl;

	while (true){
		cout << "1. Load Data Structure." << endl;
		cout << "2. Print Course List." << endl;
		cout << "3. Print Course." << endl;
		cout << "9. Exit" << endl;
		cout << "What would you like to do? ";

		int choice = readMenuChoice();

		if (choice == 1){
			cout << "Enter file name: ";
			string fileName;
			getline(cin, fileName);
			trimInPlace(fileName);

			if (fileName.empty()){
				cout << "Error: File name cannot be empty." << endl;
			} else {
				loaded = loadCoursesFromFile(fileName, courses);
			}
		}else if (choice == 2){
			if (!loaded || courses.empty()){
				cout << "No course data loaded. Select option 1 first." << endl;
			} else {
				printCourseList(courses);
				cout << "What course do you want to know about? ";
				string courseNumber;
				getline(cin, courseNumber);
				trimInPlace(courseNumber);

				if (courseNumber.empty()){
					cout << "Error: Course number cannot be empty."<< endl;
				} else {
					printCourseInformation(courses, courseNumber);
				}
			}
		} else if (choice == 9){
			cout << "Thank you for using the course planner!" << endl;
			break;
		} else {
			cout << choice << " is not a valid option." << endl;
 		}
		cout << endl;
	}
	return 0;
}
