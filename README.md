# Personal Expense Tracker

A simple Java-based web application that helps users manage their personal expenses, monitor monthly budgets, and understand their spending habits.

## About the Project

The Personal Expense Tracker is developed as part of the **Advanced Java Programming** course. It allows users to record expenses, categorize them, distinguish between necessary and extra spending, and monitor their monthly budget.

The application uses Java's built-in `HttpServer` class to run a local web server. The frontend is developed using HTML, CSS, and JavaScript, and communicates with the Java backend through HTTP requests.

All expense data is stored locally in a text file named `expenses.txt`, without requiring a database or online account.

## Features

- **Add Expenses:** Record expenses with date, category, type, amount, and optional notes.
- **Expense Categories:** Organize expenses into different categories.
- **Necessary and Extra Spending:** Separate essential expenses from additional spending.
- **Monthly Budget:** Set and monitor a monthly spending budget.
- **Dashboard:** View total spending, necessary expenses, and extra expenses.
- **Spending Analysis:** View necessary-versus-extra comparisons and category-wise breakdowns.
- **Search Expenses:** Search and filter the expense log.
- **Delete Expenses:** Remove unwanted expense records.
- **Data Import and Export:** Export expense data and import data from a file.
- **Automatic Data Storage:** Save changes to a local text file and load them when the application starts.

## Technologies Used

| Technology | Purpose |
|---|---|
| Java | Backend logic and HTTP server |
| HTML | Web page structure |
| CSS | User interface styling |
| JavaScript | Frontend functionality and HTTP communication |
| Java Collections | Store and manage expense records |
| File Handling | Save and load expense data |
| Exception Handling | Handle invalid input and errors |

## Project Structure

```text
Personal-Expense-Tracker/
│
├── ExpenseServer.java
├── index.html
├── expenses.txt       (created automatically)
└── README.md
```

## Requirements

- Java JDK 11 or later
- A modern web browser
- A code editor or terminal

## How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR-USERNAME/Personal-Expense-Tracker.git
```

### 2. Open the Project Folder

```bash
cd Personal-Expense-Tracker
```

### 3. Compile the Java File

```bash
javac --add-modules jdk.httpserver ExpenseServer.java
```

### 4. Run the Application

```bash
java --add-modules jdk.httpserver ExpenseServer
```

### 5. Open the Application

Open your browser and visit:

```text
http://localhost:8080
```

The Java server also attempts to open the application in your default browser automatically.

**Important:** Keep `ExpenseServer.java` and `index.html` in the same folder when running the application. The `expenses.txt` file is created automatically to store expense records.

## Data Storage

The application uses a local plain text file named `expenses.txt` for persistent storage.

- Expense records are saved whenever changes are made.
- The monthly budget is also stored in the file.
- Existing data is loaded automatically when the application starts.

No external database is required.

## Concepts Demonstrated

This project demonstrates several concepts from Advanced Java Programming:

- Classes and Objects
- Constructors and Methods
- Collections and ArrayList
- File Input and Output
- Exception Handling
- HTTP Server and Request Handling
- Data Validation
- Frontend and Backend Communication

## Limitations

- The application runs locally on the computer where the Java server is started.
- Expense data is stored in a local text file.
- It does not include user authentication or cloud synchronization.
- The application is designed for individual use.

## Future Enhancements

- User registration and login
- Cloud-based data storage
- Expense visualization with more charts
- Monthly and yearly financial reports
- Export reports in PDF and Excel formats
- Mobile-friendly improvements


---

**Personal Expense Tracker — Track your spending. Manage your budget. Understand your money.**
