
# Development

To start off, all dependencies must be installed using pip:
```
pip install -r requirements.txt
```

A database for development purposes can also be setup using docker compose:
```
docker-compose -f dev-docker-compose.yml up
```

The database is listening for active connections on 127.0.0.1:5432.

Credentials can also be found the docker-compose file.

To start the API server:
```
cd src
fastapi dev --port 8080 main.py
```

Your server is now active on 127.0.0.1:8080

# Deployment

The server can also be deployed in production using docker compose:
```
docker-compose up
```

The previous command will launch both the database and the API server.


# API

Login:
```
curl -v http://127.0.0.1:8080/v1/login -H 'Content-Type: application/json' --data '{"token":"TOKEN_HERE"}'
```

If login is successful, a set-cookie header is returned and must be used as a mean of authentication

Adding caregiver instruction (caregiver account only):
```
curl -v http://127.0.0.1:8080/v1/caregiver/instruction -H 'cookie: session="COOKIE_HERE"' -H 'Content-Type: application/json' --data '{"patient":PATIENT_ID, "data":{}}'
```

Assigning new caregiver to patient:
```
curl -v http://127.0.0.1:8080/v1/caregiver/assign -H 'cookie: session="COOKIE_HERE"' -H 'Content-Type: application/json' --data '{"new_caregiver":NEW_CAREGIVER_ID,"patient":PATIENT_ID}'
```

Adding patient info (patient account only):
```
curl -v http://127.0.0.1:8080/v1/patient/info -H 'cookie: session="COOKIE_HERE"' -H 'Content-Type: application/json' --data '{"data":{}}'
```

Event listener for caregiver (SSE):
```
curl -v http://127.0.0.1:8080/v1/caregiver/events -H 'cookie: session="COOKIE_HERE"' -H 'Content-Type: application/json' --data '{"last_info":LAST_INFO_ID,"last_instruction":LAST_INSTRUCTION_ID,"patient":PATIENT_ID}' -N
```

Assigning event listener for caregiver (SSE):
```
curl -v http://127.0.0.1:8080/v1/caregiver/assigned_events -H 'cookie: session="COOKIE_HERE"' -N
```

Event listener for patient (SSE):
```
curl -v http://127.0.0.1:8080/v1/patient/events -H 'cookie: session="COOKIE_HERE"' -H 'Content-Type: application/json' --data '{"last":LAST_ID}' -N
```
