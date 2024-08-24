
# Development

To start off, all dependencies must be installed using pip:
```
pip install -r requirements.txt
```

A database for development purposes can also be setup using docker compose:
```
docker-compose -f dev-docker-compose.yml up
```

to start the server:
```
fastapi dev --port 8080 src/main.py
```

Your server is now active on 127.0.0.1:8080

# Deployment

The server can also be deployed in production using docker compose:
```
docker-compose up
```

The previous command will launch both the database and the API server.
