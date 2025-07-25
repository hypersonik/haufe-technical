# Beer Catalogue API

## Overview
This is a simple RESTful API for managing a beer catalogue. It allows users to create, read, update, and delete beer entries.
It is built using Spring Boot + Spring Webflux and provides endpoints for beer management.

### Installation
1. Clone the repository:
    ```bash
    git clone https://github.com/hypersonik/haufe-technical.git
    ```
2. Navigate to the project directory:
    ```bash
    cd haufe-technical
    ```
3. Build the project using Maven:
    ```bash
    mvnw clean package
    ```
4. Build the Docker image:
    ```bash
    docker build -t beer-api .
    ```
   
### Running the Application
1. Run the application in the container:
    ```bash
    docker run -p 8080:8080 beer-api
    ```
2. Access the Swagger UI at `http://localhost:8080/swagger-ui.html` to explore the API endpoints.
3. Use Postman or any other API testing tool to test the endpoints.
4. To run the application locally without Docker, use:
    ```bash
    mvnw spring-boot:run
    ```

### Design decisions

- Even though the documentation says that the API should be built with **Spring Data JPA**, I chose to implement it using **Spring Webflux + Spring Data R2DBC**.
  The reason was that during the first interview, it was mentioned that **Haufe Group** is using (or interested in using) **Spring Webflux**, so I decided that would be interesting using it for this test.

### Accessing the API
- Base URL: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Postman commands for testing the API endpoints are provided below.

- curl commands for testing:
    ```bash
    # Get all beers
    curl -X GET "http://localhost:8080/api/beers" -H "accept: application/json"

    # Get beer by ID
    curl -X GET "http://localhost:8080/api/beers/{id}" -H "accept: application/json"

    # Create a new beer
    curl -X POST "http://localhost:8080/api/beers" -H "Content-Type: application/json" -d '{"name": "Beer Name", "manufacturerId": 1}'

    # Update a beer
    curl -X PUT "http://localhost:8080/api/beers/{id}" -H "Content-Type: application/json" -d '{"name": "Updated Beer Name"}'

    # Delete a beer
    curl -X DELETE "http://localhost:8080/api/beers/{id}"
    ```
  