#gradle build && java -Xms512m -Xmx1024m -jar build/libs/ljcrawler-0.0.1.jar zyalt drugoi
gradle assemble && cd build/distributions && unp *.zip && ./run.sh
