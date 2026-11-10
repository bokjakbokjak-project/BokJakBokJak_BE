# 복작복작 백엔드 실행 이미지
# 빌드는 GitHub Actions에서 ./gradlew bootJar로 먼저 하고, 여기서는 jar만 복사한다.
# build.gradle에 jar { enabled = false }가 있어야 build/libs에 jar가 하나만 생긴다.
FROM eclipse-temurin:21-jre

ENV TZ=Asia/Seoul
WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
