FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY pom.xml .

RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests && \
    mkdir -p target/lib && \
    mvn dependency:copy-dependencies -DoutputDirectory=target/lib

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN mkdir -p /app/files

COPY --from=build /app/target/JMSApp-1.0-SNAPSHOT.jar /app/JMSApp.jar

COPY --from=build /app/target/lib/ /app/lib/

COPY run.sh /app/

RUN chmod +x /app/run.sh

COPY .env* /app/

RUN echo '#!/bin/bash\n\
    echo "=================================="\n\
    echo "JMS Application Terminal"\n\
    echo "=================================="\n\
    echo "To start the application, run:"\n\
    echo "  java -jar JMSApp.jar"\n\
    echo ""\n\
    echo "=================================="\n\
    exec bash\n\
    ' > /app/welcome.sh && chmod +x /app/welcome.sh

ENTRYPOINT ["/app/welcome.sh"]