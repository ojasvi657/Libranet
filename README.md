# LibraNet Library Management System

---

## 📖 Introduction

*LibraNet* is a simple, command-line based library management system written in Java. It provides core functionalities for managing a catalog of various library items like books, audiobooks, and e-magazines, and also handles borrowing, returning, and managing user fines.

---

## 🚀 Features

* *Diverse Item Catalog:* Manages different types of library items, each with unique attributes:
    * Book: Physical items with a page count.
    * Audiobook: Digital items that are Playable with a specific playback duration.
    * EMagazine: Periodicals that can be archived.
* *Borrowing & Returns:* Allows users to borrow items for a specified duration and handles the return process.
* *Fine Management:* Calculates and manages fines for overdue items using a FineLedger based on a daily fine rate.
* *Robust Parsing:* The DurationParser utility class can parse human-readable duration strings (e.g., "1 day 5 hours") into a standard Duration object.
* *Exception Handling:* Custom exceptions like ItemNotAvailableException and InvalidDurationException provide clear error messages for library operations.

---

## 📦 How to Use

### Prerequisites

* Java Development Kit (JDK) 8 or later.

### Compilation and Execution

1.  *Save the Code:* Save the provided Java code as LibraNetDemo.java.
2.  *Compile:* Open your terminal or command prompt and compile the file using the Java compiler:
    sh
    javac LibraNetDemo.java
    
3.  *Run:* Execute the compiled class file:
    sh
    java LibraNetDemo
    

---

## 🛠 Code Structure

* LibraryException.java: Base class for custom library-related exceptions.
* ItemNotAvailableException.java: Exception thrown when an item is already borrowed.
* InvalidDurationException.java: Exception for improperly formatted duration strings.
* DurationParser.java: A utility class with a static method to parse strings into java.time.Duration objects.
* FineLedger.java: Manages user fines, allowing for adding, retrieving, and paying fines.
* LibraryItem.java: The abstract base class for all library items, defining common properties and methods like borrow() and returnItem().
* Book.java, Audiobook.java, EMagazine.java: Concrete subclasses of LibraryItem, each with specific attributes and behaviors.
* Playable.java: An interface for items that can be played, such as Audiobook.
* Catalog.java: Manages the collection of all `LibraryItem`s, supporting adding, finding, and searching items.
* LibraNetDemo.java: The main class containing the main method to demonstrate the library system's functionality.
