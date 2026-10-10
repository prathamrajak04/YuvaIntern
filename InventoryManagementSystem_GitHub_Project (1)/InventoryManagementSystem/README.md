# Inventory Management System

A Java-based Inventory Management System designed for a small retail business.

## Features
- Product management
- Stock control
- Order processing
- Low-stock detection
- Basic reporting structure
- Repository and Service Layer architecture
- MySQL database schema
- Java Maven project structure

## Architecture

```text
User/Admin
    |
Presentation Layer
    |
Service Layer
    |---- ProductService
    |---- StockService
    |---- OrderService
    |---- ReportService
    |
Repository Layer
    |---- ProductRepository
    |---- OrderRepository
    |---- StockRepository
    |
MySQL Database
```

## Design Patterns
- Repository Pattern
- Service Layer Pattern
- Factory Pattern
- Dependency Injection through constructors

## Tech Stack
- Java 17+
- Maven
- MySQL
- JDBC

## Project Structure

```text
src/
 ├── main/
 │   ├── java/com/inventory/
 │   │   ├── model/
 │   │   ├── repository/
 │   │   ├── service/
 │   │   ├── exception/
 │   │   └── Main.java
 │   └── resources/
 │       └── schema.sql
 └── test/
```

## Future Scope
- Spring Boot REST API
- Authentication and role-based access
- Barcode scanning
- Supplier management
- Dashboard and analytics
- CSV/PDF reports
