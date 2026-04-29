#!/bin/bash
echo 'Creating booking-room topics...'

kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:29092 \
  --topic booking.room.v1 --partitions 3 --replication-factor 1

kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:29092 \
  --topic booking.reservation.v1 --partitions 3 --replication-factor 1

kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:29092 \
  --topic booking.notification.v1 --partitions 3 --replication-factor 1

echo 'All topics created successfully!'
kafka-topics --list --bootstrap-server kafka:29092