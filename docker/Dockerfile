FROM openjdk:8-jre-alpine
RUN apk update && apk add bash
COPY svc /svc
EXPOSE 9000 9000
CMD /svc/bin/start -Dhttp.port=9000 
