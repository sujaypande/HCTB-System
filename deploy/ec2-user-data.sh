#!/bin/bash
# EC2 user data for the launch template (Ubuntu Server 24.04): every new instance installs Docker and
# starts the app, connected to the shared RDS MySQL database. Fill in the two values below before using it.
set -euxo pipefail

DB_ENDPOINT="REPLACE_WITH_RDS_ENDPOINT"     # e.g. ticketbooking-db.xxxxxxxx.ap-southeast-2.rds.amazonaws.com
DB_PASSWORD="REPLACE_WITH_RDS_PASSWORD"
DB_USERNAME="admin"
APP_IMAGE="sujayp049/ticket-booking:latest"

apt-get update -y
apt-get install -y docker.io
systemctl enable --now docker

docker run -d --name ticket-app --restart unless-stopped -p 8080:8080 \
  -e DB_URL="jdbc:mysql://${DB_ENDPOINT}:3306/ticketbooking?createDatabaseIfNotExist=true&serverTimezone=UTC" \
  -e DB_USERNAME="${DB_USERNAME}" \
  -e DB_PASSWORD="${DB_PASSWORD}" \
  -e JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m" \
  "${APP_IMAGE}"
