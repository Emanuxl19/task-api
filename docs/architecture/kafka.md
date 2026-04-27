# Kafka Adoption Notes

## Short Answer

Yes, Kafka can fit this project, but not as a replacement for the current REST request path.

For this API, Kafka makes sense when you need asynchronous processing, event fan-out, or decoupled consumers. It does not make sense just to make CRUD "more enterprise".

## Where Kafka Fits Well

Good candidates:

- `user.registered` for onboarding, analytics, audit, or notifications
- `task.created` and `task.completed` for downstream automations
- `auth.login.failed` for security monitoring and alerting
- `ai.requested` and `ai.completed` for async AI workflows and usage tracking
- audit and event streaming into a data pipeline

## Where Kafka Does Not Help

Avoid Kafka for:

- JWT validation
- request-response CRUD reads
- synchronous login and token refresh
- simple controller-to-service communication inside the same application

Those paths should stay synchronous and transactional in the API.

## Recommended Rollout

1. Keep the current API synchronous for users, tasks, and authentication.
2. Introduce domain events only where there is a real second consumer.
3. Publish events after successful commits using an outbox table.
4. Consume events with idempotent handlers.
5. Add dead-letter handling and monitoring before moving critical flows onto Kafka.

## First Events Worth Adding

If we adopt Kafka, start with low-risk events:

- `task.created`
- `task.updated`
- `task.completed`
- `user.registered`
- `auth.login.failed`
- `ai.requested`

Example payload:

```json
{
  "eventId": "0b9d90ea-7a33-4b7a-90a1-78f1a76a4b85",
  "eventType": "task.created",
  "occurredAt": "2026-04-23T21:00:00Z",
  "taskId": 42,
  "userId": 7
}
```

## Implementation Guidance

When the time comes, prefer:

- `spring-kafka`
- topic-per-domain-event or a small bounded set of topics
- JSON or Avro with explicit versioning
- outbox pattern instead of direct publish inside transactions
- consumer idempotency based on `eventId`

## Decision

Kafka is a good next step only if one of these becomes true:

- we need async workloads for AI or notifications
- more than one downstream consumer needs the same event
- audit and analytics pipelines become product requirements

Until then, the current synchronous architecture is the right default.
