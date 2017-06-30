# Cinema service
This is a tiny API that allows reserving seats for movies. Example API calls can be found in this Postman collection https://www.getpostman.com/collections/21b2c655662e473d2896

## Requirements
- Sbt 0.13.*
- Mongo 3.x.x (e.g. through docker: ```docker run -p 27017:27017 mongo```)

## Running
Simply run by ```sbt run``` or build a docker image first ```sbt docker:publishLocal```

## Configuration
The main configuration file is ```src/main/resources/reference.conf```. The format is [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md). Here you can specify the database endpoint and the HTTP interface.

## Example flow

### Create a movie
```
curl -X POST \
  http://localhost:9090/movies \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
  "movieTitle": "The Shawshank Redemption"
}'
```

### Create a screening
Use returned id of created movie call as imdbId
```
curl -X POST \
  http://localhost:9090/screenings \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
  "imdbId": "78476384-5af7-4251-ba0e-cb78748b2a1a",
  "availableSeats": 100,
  "screenId": "screen_123456"
}'
```

### Make reservations
```
curl -X POST \
  http://localhost:9090/reservations \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
  "imdbId": "78476384-5af7-4251-ba0e-cb78748b2a1a",
  "screenId": "screen_123456"
}'
```


## Known Limitations
- No transactions
- Can only screen a movie with the same screen id after deleting the screening through REST api


