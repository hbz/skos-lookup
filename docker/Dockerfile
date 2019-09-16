FROM openjdk:8-jre-alpine
RUN export http_proxy="http://192.168.10.66:3128" && export https_proxy="http://192.168.10.66:3128" && apk update && apk add bash
COPY svc /svc
EXPOSE 9000 9000
CMD /svc/bin/start -Dhttp.port=9000 
