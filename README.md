# RentDi Backend Application

A Spring Boot application providing the backend APIs for the RentDi application, a platform for property rental management.

## Features

- User authentication with JWT and refresh tokens
- Role-based authorization (Admin, Landlord, Tenant)
- Secure logout mechanism
- PostgreSQL database integration

## Technologies

- Java 21
- Spring Boot 3.4.4
- Spring Security
- JWT Authentication (Auth0)
- PostgreSQL
- JPA/Hibernate

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL
- Maven

### Running the Application

1. Clone the repository
2. Configure the database in application-dev.properties
3. Run the SQL script to initialize roles: `src/main/resources/db/init-roles.sql`
4. Run the application:

```bash
mvn spring-boot:run
```

## API Endpoints

- POST `/api/auth/register` - Register a new user
- POST `/api/auth/login` - Login and get tokens
- POST `/api/auth/refreshtoken` - Refresh access token
- POST `/api/auth/logout` - Logout and invalidate tokens

## License

This project is licensed under the Apache License 2.0
