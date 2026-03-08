# Event-Driven Notification System

A distributed notification system built with Java, Spring Boot, and Apache Kafka.
A single REST API accepts notification requests and publishes events to a Kafka topic,
consumed independently by channel-specific services (email, SMS).

---

## Architecture

```
[Client] --> REST API --> [notification-service] --> notification.events |--> [email-consumer]
                                                                         |--> [sms-consumer]
```

**notification-service :** 
exposes `POST /api/v1/notifications`, validates the request,
publishes a `NotificationEvent` to Kafka, and persists an audit record to PostgreSQL.

**email-consumer :** consumes events from the shared topic, filters for `EMAIL` channel,
and simulates email delivery. Failed messages are retried with exponential backoff before
being routed to a dead letter queue.

**sms-consumer :** same pattern as email-consumer, independent consumer group, `SMS` channel.

**Infrastructure :** Kafka + Zookeeper + PostgreSQL managed via Docker Compose.

---

## Stack

Java 17 - Spring Boot 3 - Apache Kafka - PostgreSQL - Liquibase - Docker Compose - Testcontainers

---

## Design Decisions

**Why Kafka over direct REST calls :**
A notification event may need to reach multiple consumers independently. Kafka decouples
the producer from delivery so adding a new channel means adding a new consumer with zero
changes to the producer.

**Single shared topic vs separate topics per channel :**
Both consumers read from `notification.events` and filter by channel. Routing to separate
topics at the producer level would eliminate wasted reads but couples the producer to
delivery channels. At this scale the shared topic is the right trade-off.

**`DefaultErrorHandler` over manual retry logic :**
Retry and backoff are infrastructure concerns, not business logic. `DefaultErrorHandler`
handles retries, backoff, and DLQ routing without a single line of retry logic in the
consumer itself.

**Audit log in PostgreSQL :**
Kafka's retention is finite. The PostgreSQL audit log is permanent proof of every delivery
attempt, independent of Kafka's retention policy.

---

## Run Locally

**Prerequisites:** Docker, Java 17, Maven

**Environment setup :** create a `.env` file at the project root:
```
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
POSTGRES_DB=db_name
POSTGRES_USER=db_username
POSTGRES_PASSWORD=user_pswd
```

For terminal runs, export the variables before starting the services:
```bash
export $(cat .env | xargs)
```

```bash
# Start infrastructure
docker-compose up -d

# Then start all 3 services
```

**Send a notification:**
```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "channel": "EMAIL",
    "recipient": "test@example.com",
    "subject": "Hello",
    "message": "Test notification"
  }'
```

---

## Testing


**Unit tests :** each class in isolation: producer publishes to the correct topic,
consumer calls the sender for the right channel, exceptions propagate for retry.

**Integration tests :** real Kafka and PostgreSQL via Testcontainers, full HTTP to Kafka to
PostgreSQL flow for the producer, published event to consumer delivery for email-consumer.

---

## Failure Handling

```
Message fails processing
    ↓
DefaultErrorHandler retries: wait 1s → wait 2s → wait 4s
    ↓
Retries exhausted → routed to notification.events.dlq
    ↓
DlqMonitor logs the failure (production: alert to PagerDuty / Slack)
    ↓
Offset committed — message never silently lost
```