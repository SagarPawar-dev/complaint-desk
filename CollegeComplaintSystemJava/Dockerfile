# ==============================================================================
# Production Multi-Stage Dockerfile
# College Complaint Management System
# Runtime: Java 21 + Apache Tomcat 10.1 (Jakarta EE 10)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Compile Java Sources
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy project files into build container
COPY . .

# Compile all Java sources using Java 21 javac
# Note: servlet-api.jar in WEB-INF/lib is used ONLY for compilation
RUN mkdir -p WEB-INF/classes && \
    javac -encoding UTF-8 \
          -cp "WEB-INF/lib/*:lib/*" \
          -d WEB-INF/classes \
          src/com/complaint/model/*.java \
          src/com/complaint/dao/*.java \
          src/com/complaint/util/*.java \
          src/com/complaint/filter/*.java \
          src/com/complaint/servlet/*.java

# Remove servlet-api.jar from WEB-INF/lib so it is NOT bundled into the runtime webapp
# (Apache Tomcat 10.1 already provides Jakarta Servlet API in $CATALINA_HOME/lib)
RUN rm -f WEB-INF/lib/servlet-api.jar

# ------------------------------------------------------------------------------
# Stage 2: Runtime Image with Official Apache Tomcat 10.1 (Temurin JDK 21)
# ------------------------------------------------------------------------------
FROM tomcat:10.1-jdk21-temurin

WORKDIR /usr/local/tomcat

# Remove default Tomcat demo webapps (ROOT, examples, docs, etc.)
RUN rm -rf webapps/*

# Deploy this application at ROOT context ("/")
COPY --from=builder /build/admin /usr/local/tomcat/webapps/ROOT/admin/
COPY --from=builder /build/student /usr/local/tomcat/webapps/ROOT/student/
COPY --from=builder /build/css /usr/local/tomcat/webapps/ROOT/css/
COPY --from=builder /build/js /usr/local/tomcat/webapps/ROOT/js/
COPY --from=builder /build/index.html /usr/local/tomcat/webapps/ROOT/index.html
COPY --from=builder /build/login.html /usr/local/tomcat/webapps/ROOT/login.html
COPY --from=builder /build/WEB-INF /usr/local/tomcat/webapps/ROOT/WEB-INF/

# Ensure local db.properties (if present) is never baked into the container
RUN rm -f /usr/local/tomcat/webapps/ROOT/WEB-INF/db.properties

# Configure JVM Memory for Render 512MB RAM free tier
# -Xms128m : Initial heap 128MB
# -Xmx320m : Maximum heap 320MB (leaves plenty of headroom for Tomcat threads, OS, Metaspace)
# -XX:+UseG1GC : Modern low-latency Garbage Collector
# -XX:+ExitOnOutOfMemoryError : Fail-fast on memory exhaustion
ENV JAVA_OPTS="-Xms128m -Xmx320m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Standard Tomcat port
EXPOSE 8080

# Start Tomcat in foreground
CMD ["catalina.sh", "run"]
