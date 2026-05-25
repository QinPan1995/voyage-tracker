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
src/
  main/
    java/com/youniverse/voyagetracker/
      VoyageTrackerApplication.java
      coscoshipping/
        CoscoShipmentSchedule.java
      shipmentlink/
        ShipmentLinkLoadedDate.java
    resources/
      application.yml
  test/
    java/com/youniverse/voyagetracker/
      VoyageTrackerApplicationTests.java
      coscoshipping/
        CoscoShipmentScheduleTests.java
```

## Run

```bash
mvn spring-boot:run
```

## Test

```bash
mvn test
```
