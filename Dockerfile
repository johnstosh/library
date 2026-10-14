# Use a slim Java base image matching the project's Java 25 configuration
FROM eclipse-temurin:25-jre-alpine

RUN apk upgrade --no-cache

# Set working directory
WORKDIR /app

# Copy the Spring Boot JAR and extract it so the JVM can use a class data
# sharing (CDS) archive (CDS needs plain jars on the classpath).
COPY build/libs/*.jar /tmp/application.jar
RUN java -Djarmode=tools -jar /tmp/application.jar extract --destination /app/extracted \
    && rm /tmp/application.jar

# Memory: Cloud Run gives this container 512Mi. A 350m heap was killed at
# 564-575Mi (library-dev-00511-4rq, library-dev-00513-c5r). A 280m heap was
# killed at 517-541Mi (library-dev-00514-d8x). A 240m heap without compact
# headers died with Java heap space (library-dev-00512-vvp). With compact
# headers, library-dev-00515-wxt at 240m became healthy, then new instances
# were killed at 512-515Mi. A local 240m run peaked at 344Mi. Compact object
# headers shrink each header from 12 bytes to 8. The CDS archive is trained
# with the same flags. Heap is 230m. Without the non-heap caps the JVM used
# about 170-240Mi outside the heap and Cloud Run killed it before port 8080
# opened.
#   -XX:SharedArchiveFile        class metadata comes from the CDS archive below
#   -XX:ArchiveRelocationMode=0  map the archive at its own address so its read-only
#                                pages stay clean file pages instead of patched copies
#   -XX:+UseCompactObjectHeaders 8-byte object headers (Java 25); archive must match
#   -XX:+UseSerialGC             smallest GC overhead for 1 vCPU
#   -XX:TieredStopAtLevel=1      C1 JIT only: no C2 compiler memory spikes, less code cache
#   -XX:ReservedCodeCacheSize    cap JIT code (C1 uses about 18Mi)
#   -XX:MaxMetaspaceSize         cap class metadata (about 17Mi with CDS, 97Mi without)
#   -Xss512k                     smaller thread stacks
#   -XX:MaxDirectMemorySize      cap NIO direct buffers (default is the heap size)
# Keep the flags in the training run identical to CMD so the archive matches.

# CDS training run: start the Spring context without a database, exit after
# refresh, and write the loaded classes to /app/application.jsa.
RUN java -Xmx230m -XX:+UseCompactObjectHeaders -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=160m -Xss512k -XX:MaxDirectMemorySize=32m \
      -XX:ArchiveClassesAtExit=/app/application.jsa \
      -Dspring.context.exit=onRefresh \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.sql.init.mode=never \
      -Dlogging.level.root=WARN \
      -jar /app/extracted/application.jar > /tmp/cds-training.log 2>&1 \
    || (tail -50 /tmp/cds-training.log; exit 1); \
    test -s /app/application.jsa || (tail -50 /tmp/cds-training.log; exit 1); \
    rm -f /tmp/cds-training.log

# Expose the port (Cloud Run uses PORT env variable, default 8080)
EXPOSE ${PORT:-8080}

# Run the app. Heap is 230m. Compact headers are on, matching the CDS archive.
CMD ["java", "-Xmx230m", "-XX:+UseCompactObjectHeaders", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-XX:ReservedCodeCacheSize=48m", "-XX:MaxMetaspaceSize=160m", "-Xss512k", "-XX:MaxDirectMemorySize=32m", "-XX:SharedArchiveFile=/app/application.jsa", "-XX:+UnlockDiagnosticVMOptions", "-XX:ArchiveRelocationMode=0", "-jar", "/app/extracted/application.jar"]
