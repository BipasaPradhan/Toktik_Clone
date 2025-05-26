Server runs on port 8080

```shell
mvn spring-boot:run
```

To build a jar file:
```shell
mvn package
```

To run the jar file:
```shell
java -jar jarfilename.jar
```

Database Requirements:
- MariaDB

Environment Variables:
- `UMS_DB_NAME` (default: ums)
- `UMS_DB_USERNAME` (default: dev)
- `UMS_DB_PASSWORD` (default: devpass)
- `UMS_DB_PORT` (default: 3306)
- `UMS_DB_IP` (default: localhost)
