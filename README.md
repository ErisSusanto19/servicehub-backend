# ServiceHub Backend

A robust backend for a service marketplace platform, connecting customers with service providers. Built with Java and the Spring Boot framework, it features a clean, layered architecture designed for scalability and maintainability.

## Key Features

-   **User Authentication & Authorization**: Secure registration and login flow using JWT. Role-based access control for `CUSTOMER`, `PROVIDER`, and `ADMIN` roles.
-   **Service Management**: Providers can create, read, update, and delete their services, including image uploads handled by Cloudinary.
-   **Order Lifecycle Management**: Customers can place orders for services. Providers can manage the order status from `PENDING` to `COMPLETED`.
-   **Payment Integration**: Integrated with Midtrans to handle payment transactions for orders.
-   **Review & Rating System**: Customers can leave reviews and ratings for completed orders, helping to build trust on the platform.
-   **Admin Panel**: Endpoints for administrators to manage users (e.g., ban/unban) and oversee platform operations.
-   **Notifications**: A system to notify users of important events, such as new orders or status changes.
-   **Performance Caching**: Implemented with Redis to cache frequently accessed data (like categories and service details), reducing database load and improving response times.

## Technology Stack

-   **Backend**: Java 21, Spring Boot 3.5
-   **Database**: PostgreSQL
-   **Database Migration**: Liquibase
-   **Data Access**: Spring Data JPA (Hibernate)
-   **Security**: Spring Security (JWT Authentication)
-   **Caching**: Redis (via Spring Data Redis)
-   **File Storage**: Cloudinary for image uploads.
-   **Payment Gateway**: Midtrans
-   **Build & Dependency Management**: Maven
-   **Testing**: JUnit 5, Testcontainers, Mockito

## Architecture Overview

The project follows a classic **Layered Architecture** to ensure a strong separation of concerns:

-   **`Controllers`**: Handle all incoming HTTP requests, validate input (DTOs), and delegate to the service layer.
-   **`Services`**: Contain the core business logic of the application. This layer is agnostic of web concerns.
-   **`Repositories`**: Manage data persistence and retrieval using Spring Data JPA.
-   **`Entities`**: JPA entities that map to the database schema.
-   **`Config`**: Contains configuration for security, caching, and external service integrations.

## Getting Started

Follow these instructions to get the project up and running on your local machine.

### Prerequisites

-   Git
-   JDK 21 or later
-   Apache Maven
-   Docker and Docker Compose (for running PostgreSQL and Redis)
-   An IDE like IntelliJ IDEA or VS Code.

### 1. Clone the Repository

```bash
git clone https://github.com/ErisSusanto19/servicehub-backend
cd servicehub-backend
```

### 2. Configure Environment Variables

1.  Copy the example environment file:
    ```bash
    cp .env.example .env
    ```

2.  **Set up the Database and Cache:**
    You have two main options for running PostgreSQL and Redis for local development.

    **Option A: Using Docker (Recommended for a quick start)**
    This is the easiest way to get the required services running.

    -   **Start PostgreSQL Container:**
        ```bash
        docker run --name servicehub-db -e POSTGRES_USER=myuser -e POSTGRES_PASSWORD=mypassword -e POSTGRES_DB=servicehub_db -p 5432:5432 -d postgres
        ```
    -   **Start Redis Container:**
        ```bash
        docker run --name servicehub-redis -p 6379:6379 -d redis
        ```

    **Option B: Using a Local Installation**
    If you have PostgreSQL and Redis already installed and running on your local machine, simply ensure your connection details match those in the `.env` file.

3.  **Update your `.env` file** with the correct values. For the local setup above, it would look like this:

    ```dotenv
    # Server Port
    DEFAULT_PORT=8080

    # PostgreSQL Database
    DB_URL=jdbc:postgresql://localhost:5432/servicehub_db
    DB_USERNAME=myuser
    DB_PASSWORD=mypassword
    POSTGRES_DB=servicehub_db

    # JWT Secrets (Generate your own strong secrets)
    JWT_KEY=your-super-secret-key-for-jwt-hs256-minimum-256-bits
    JWT_EXPIRATION=86400000 # 24 hours in ms

    # External Services (Get these from their respective dashboards)
    CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
    MIDTRANS_SERVER_KEY=your_midtrans_server_key
    MIDTRANS_CLIENT_KEY=your_midtrans_client_key
    ```

### 3. Run the Application

You can run the application using the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

The application will start on the port specified by `DEFAULT_PORT` (e.g., `http://localhost:8080`).

### 4. Running Tests

The integration tests use **Testcontainers** to automatically spin up and manage a dedicated PostgreSQL container.

**Important:** You must have Docker running on your machine for the tests to execute successfully.

To run the full suite of integration and unit tests:

```bash
mvn test
```

## API Endpoints Overview

Here is overview of the main API resources:

-   `/auth` - User registration and login.
-   `/api/categories` - CRUD for service categories (Admin-only for write operations).
-   `/api/services` - Public browsing of services, and CRUD for providers to manage their own services.
-   `/api/orders` - Endpoints for customers to create orders and for providers to manage them.
-   `/api/reviews` - Endpoints for customers to post reviews on completed orders.
-   `/api/profile` - Endpoints for users to manage their own profiles.
-   `/api/admin` - Endpoints for administrative tasks.

---