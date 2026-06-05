# Similar Products API

## Requirements

- Docker

## Run

All `docker-compose` commands must be run from the **root of the repository** (one level up from this folder). No need to follow the instructions in the root README — this README is self-contained.

Starts all services (mock, app, influxdb, grafana):
```bash
docker-compose up -d
```

| Service | Description |
|---------|-------------|
| `similar-products` | This API (port 5000) |
| `simulado` | Mock server for existing APIs (port 3001) |
| `influxdb` | Metrics storage for k6 |
| `grafana` | Dashboard to view k6 results (port 3000) |

## Usage

```
GET http://localhost:5000/product/{productId}/similar
```

## k6 tests

```bash
docker-compose run --rm k6 run scripts/test.js
```

View results at [http://localhost:3000/d/Le2Ku9NMk/k6-performance-test](http://localhost:3000/d/Le2Ku9NMk/k6-performance-test).

## Stop

```bash
docker-compose down
```
