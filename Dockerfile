# ==============================================================================
# Production Multi-Stage Dockerfile (Repository Root)
# College Complaint Management System
# Runtime: Java 21 + Apache Tomcat 10.1 (Jakarta EE 10)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Compile Java Sources
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy application sources from CollegeComplaintSystemJava directory
COPY CollegeComplaintSystemJava .

# Compile all Java sources using Java 21 javac
RUN mkdir -p WEB-INF/classes && \
    javac -encoding UTF-8 \
          -cp "WEB-INF/lib/*:lib/*" \
          -d WEB-INF/classes \
          src/com/complaint/model/*.java \
          src/com/complaint/dao/*.java \
          src/com/complaint/util/*.java \
          src/com/complaint/filter/*.java \
          src/com/complaint/servlet/*.java

# Remove servlet-api.jar so it is not bundled as a runtime dependency
RUN rm -f WEB-INF/lib/servlet-api.jar

# ------------------------------------------------------------------------------
# Stage 2: Runtime Image with Official Apache Tomcat 10.1 (Temurin JDK 21)
# ------------------------------------------------------------------------------
FROM tomcat:10.1-jdk21-temurin

WORKDIR /usr/local/tomcat

# Remove default Tomcat demo applications
RUN rm -rf webapps/*

# Deploy this application at ROOT context ("/")
COPY --from=builder /build/admin /usr/local/tomcat/webapps/ROOT/admin/
COPY --from=builder /build/student /usr/local/tomcat/webapps/ROOT/student/
COPY --from=builder /build/css /usr/local/tomcat/webapps/ROOT/css/
COPY --from=builder /build/js /usr/local/tomcat/webapps/ROOT/js/
COPY --from=builder /build/index.html /usr/local/tomcat/webapps/ROOT/index.html
COPY --from=builder /build/login.html /usr/local/tomcat/webapps/ROOT/login.html
COPY --from=builder /build/WEB-INF /usr/local/tomcat/webapps/ROOT/WEB-INF/

# Ensure local db.properties is never baked into the container
RUN rm -f /usr/local/tomcat/webapps/ROOT/WEB-INF/db.properties

# Configure JVM Memory for Render 512MB RAM free tier
ENV JAVA_OPTS="-Xms128m -Xmx320m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Standard Tomcat port
EXPOSE 8080

# Start Tomcat in foreground
CMD ["catalina.sh", "run"]
