# distributed-hotel-booking-system
Distributed hotel booking system in Java with Raft-based coordination, JavaFX GUI and Docker deployment

# Vision
Develop a Distributed Hotel Booking System managing room reservations across multiple remote hotels. A central GUI client coordinates bookings preventing overbooking, while hotel nodes handle local availability with replication and synchronization.​

# System Functionalities

Dynamic hotel (node) registration to cluster.

Room booking with distributed locking (Raft consensus).

GUI dashboard: hotel map, real-time availability, booking form.

Booking replication (configurable factor R).

Idempotent cancel/modify operations.

Heartbeats for failure detection.

Aggregate occupancy/revenue reports.

CLI + JavaFX GUI client.​

# Learning Goals
Applies core course concepts:

Distributed locking/consensus (Raft) for atomic bookings.

CAP Theorem: CP (Consistency>Availability) during booking.​

Fault tolerance: operational despite hotel failures.

Scalability: dynamic hotel scaling/re-partitioning.

Synchronization: consistent room state across nodes.

Performance: high-throughput booking peaks.

Idempotency: retry-safe operations.​

# Intended Technologies

Backend: Java 17+ (enterprise-grade).

Storage: H2 embedded (in-memory rooms/bookings).

Frontend: JavaFX GUI (dashboard + booking UI).

Deployment: Docker Compose (6 containers: 5 hotels + client).

Testing: JUnit 5 + Testcontainers.​

# Intended Deliverables

Complete backend (central coordinator + hotel nodes).

JavaFX GUI: interactive dashboard, real-time maps, booking interface.

Docker Compose cluster deployment.

Demo scenarios: booking, hotel crash/recovery, scaling.

Unit/integration tests.

Technical report: architecture, design decisions, evaluation.​

# Usage Scenarios
S1: Normal Booking

GUI shows hotel map → select date/room → Raft lock acquired → replicated to backups.
​
S2: Conflict Handling

Two clients book same room → second fails (distributed lock).

S3: Hotel Failure

Hotel1 crashes mid-session → GUI uses replica data seamlessly.

S4: Cluster Scaling

New Hotel6 joins → automatic room re-partitioning, GUI refresh.


Group Members
[Saponaro Mattia] — mattia.saponaro2@studio.unibo.it
