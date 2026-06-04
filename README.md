# Employee Management Spring Boot Project

## Technologies Used
- Spring Boot 3
- Spring Data JPA
- MySQL
- SLF4J with Logback
- Maven
- Java 17

## Database Setup

Create a MySQL database:

```sql
CREATE DATABASE employee_db;
```

Update credentials in:
`src/main/resources/application.properties`

## Build the Project

```bash
mvn clean install
```

## Run the Project

```bash
mvn spring-boot:run
```

Application runs at:

```
http://localhost:8080
```

## REST API Endpoints

### Create Employee
**POST** `/api/employees`

Request:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "department": "Engineering",
  "salary": 55000
}
```

### Get Employee By ID
**GET** `/api/employees/1`

### Get All Employees
**GET** `/api/employees`

### Update Employee
**PUT** `/api/employees/1`

### Delete Employee
**DELETE** `/api/employees/1`

## Logging

Logs are generated in:

```
logs/employee-management.log
```

## SQL Initialization

Run the provided `schema.sql` file if required.