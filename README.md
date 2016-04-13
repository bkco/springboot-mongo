# springboot-mongo

Spring Boot projects that demonstrate generation of CSV files.

Project "cursor" demos:
* conversion of known document types to java objects using core-jackson library.
* streaming a JSON file of unknown structure to CSV.

## Build Project
1.  Java 8 must be used to build.
2.  mvn clean install

## Run Project
1.  Run with "java -jar /path/to/cursor-0.0.1-SNAPSHOT.jar"
2.  CURL request:
        curl -X GET -H "Content-Type: text/csv" http://localhost:8080/csv
        curl -X GET -H "Content-Type: text/csv" http://localhost:8080/csvfromfile
