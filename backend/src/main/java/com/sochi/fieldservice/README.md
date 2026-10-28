# Field Service & Network Incident Management System

## Overview

A backend application for managing network incidents, assigning field engineers, tracking service visits, and recording incident resolution.

## Technology Stack

* Java 21
* Spring Boot 3.5.x
* Spring Web
* Spring Data JPA
* PostgreSQL
* Maven
* Docker (planned for local infrastructure)

## Current Features

* Application health endpoint
* API welcome endpoint

## API Endpoints

| Method | Endpoint         | Purpose                  |
| ------ | ---------------- | ------------------------ |
| GET    | `/api/v1`        | Application information  |
| GET    | `/api/v1/health` | Check application health |

## Local Setup

1. Install Java 21.
2. Open the project in IntelliJ IDEA.
3. Run `FieldserviceApplication.java`.
4. Open `http://localhost:8080/api/v1/health`.

## Planned Features

* User and role management
* Network incident creation and tracking
* Field engineer assignment
* Service visit records
* Incident priority and status management
* PostgreSQL persistence
* Docker-based local environment
