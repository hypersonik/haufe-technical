FROM amazoncorretto:21.0.7-alpine

# Set the working directory
WORKDIR /haufe

# Copy the JAR file into the container
COPY target/api-0.0.1-SNAPSHOT.jar api-0.0.1-SNAPSHOT.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "api-0.0.1-SNAPSHOT.jar"]

# Build the Docker image with the following command:
# docker build -t haufe-app .

# Run the Docker container with the following command:
# docker run -p 8080:8080 haufe-app

# Note: Ensure that the JAR file is built and located in the target directory before building the Docker image.
# To build the JAR file, use the following command:
# mvn clean package
# Ensure that the Maven build is successful and the JAR file is present in the target directory.
