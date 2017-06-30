# Geolocation service
This is a tiny application that imports and serves geolocation information from various sources. On start up the application will load the geolocation information from the configured sources until drained and serve the results under ```GET http://$host:$port/ips/$ip```

## Requirements
- Sbt 0.13.*
- Mongo 3.x.x (e.g. through docker: ```docker run -p 27017:27017 mongo```)

## Running
Simply run by ```sbt run``` or build a docker image first ```sbt docker:publishLocal```

## Configuration
The main configuration file is ```src/main/resources/reference.conf```. The format is [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md). Here you can specify the database endpoint, the HTTP interface and the sources to process. Currently the application supports local CSV files. A sample configuration:
```
jeroenr.geolocation {
    import {
        batchSize = 1000
        parallellism = 4
        
        sources = [
          {
              location = "/Users/jero/dev/projects/be_code_challenge_jeroenr_1663260/input/data_dump.csv"
              type = "local-dsv"
              delimiter = ","
              header = true
          }
        ]
    }
}
```

## Known Limitations
- Should use something like Kamon for tracking metrics
- Supporting upserts? Currently we ignore existing IP entries
- Listing the Ip entries through the API should implement proper pagination


