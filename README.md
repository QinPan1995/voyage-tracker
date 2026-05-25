# voyage-tracker

Spring Boot project scaffold for a Java 8 + Maven application with jsoup.

This scaffold intentionally keeps dependencies minimal. Web, database, cache,
scheduler, and other modules can be added later as needed.

## Tech Stack

- Java 8
- Maven
- Spring Boot 2.7.18
- jsoup 1.17.2

## Project Structure

```text
.mvn/wrapper/
mvnw
mvnw.cmd
src/
  main/
    java/com/youniverse/voyagetracker/
      VoyageTrackerApplication.java
      shipmentlink/
        ShipmentLinkLoadedDate.java
    resources/
      application.yml
  test/
    java/com/youniverse/voyagetracker/
      VoyageTrackerApplicationTests.java
```

## Run

```bash
./mvnw spring-boot:run
```

## Test

```bash
./mvnw test
```
