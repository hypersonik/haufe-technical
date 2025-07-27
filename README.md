# Beer Catalogue API

## Overview
This is a simple RESTful API for managing a beer catalogue. It allows users to create, read, update, and delete beer entries.
It is built using Spring Boot + Spring Webflux and provides endpoints for beer management.

### Installation
1. Clone the repository:
    ```bash
    git clone https://github.com/hypersonik/haufe-technical.git
    ```
2. Checkout the `webflux` branch:
    ```bash
    git checkout webflux
    ```
3. Navigate to the project directory:
    ```bash
    cd haufe-technical
    ```
4. Build the project using Maven:
    ```bash
    mvnw clean package
    ```
5. Build the Docker image:
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
- In some places (like user password update), security is not enforced for the sake of simplicity, but in a real-world application, it would be important to secure them.

### Bonus features
- Implemented role-based access control using **Spring Security** and Basic Authentication.
- Flexible search functionality for beers by name, type, ABV, or manufacturer
- Pagination and sorting for manufacturer and beer entries.

### Accessing the API
- Base URL: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Postman commands for testing the API endpoints are provided below.

- curl commands for testing:
    ```bash
    #Basic Authentication
    # Replace 'username' and 'password' with your actual credentials
    curl -u username:password http://localhost:8080/api/beers
  
    # Get all manufacturers
    curl -X GET "http://localhost:8080/api/manufacturers" -H "accept: application/json"
    # Get manufacturer by ID
    curl -X GET "http://localhost:8080/api/manufacturers/{id}" -H "accept: application/json"
    # Create a new manufacturer
    curl -X POST "http://localhost:8080/api/manufacturers" -H "Content-Type: application/json" -d '{"name": "Manufacturer Name"}'
    # Update a manufacturer
    curl -X PUT "http://localhost:8080/api/manufacturers/{id}" -H "Content-Type: application/json" -d '{"name": "Updated Manufacturer Name"}'
    # Delete a manufacturer
    curl -X DELETE "http://localhost:8080/api/manufacturers/{id}"
  
    # Search manufacturers by name
    curl -X GET "http://localhost:8080/api/manufacturers/search?name=ManufacturerName" -H "accept: application/json"
    # Search beers by name, type, ABV, or manufacturer
    curl -X GET "http://localhost:8080/api/beers/search?name=BeerName&type=Lager&abv=5.0&manufacturerId=1" -H "accept: application/json"
    # Pagination and sorting for manufacturers
    curl -X GET "http://localhost:8080/api/manufacturers?page=0&size=10&sort=name,asc" -H "accept: application/json"
    # Pagination and sorting for beers
    curl -X GET "http://localhost:8080/api/beers?page=0&size=10&sort=name,asc" -H "accept: application/json"
  
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
