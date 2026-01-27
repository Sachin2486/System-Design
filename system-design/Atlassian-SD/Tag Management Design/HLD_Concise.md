# Tag Management System - Concise HLD

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   JIRA UI   │  │ Confluence UI│  │  Mobile App  │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │ HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API GATEWAY LAYER                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  API Gateway (Kong/AWS API Gateway)                     │  │
│  │  • Authentication (JWT Validation)                        │  │
│  │  • Rate Limiting (100 req/min per user)                 │  │
│  │  • Request Routing                                      │  │
│  └───────────────────────┬──────────────────────────────────┘  │
└──────────────────────────┼──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TAG SERVICE LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Tag Service (Multiple Instances)                │  │
│  │  • Tag CRUD Operations                                  │  │
│  │  • Tag Association Management                           │  │
│  │  • Role-Based Authorization (RBAC)                      │  │
│  │  • Search & Filtering Logic                            │  │
│  └───────┬───────────────────────────┬──────────────────────┘  │
└──────────┼───────────────────────────┼──────────────────────────┘
           │                           │
           │ Write Path                │ Read Path
           ▼                           ▼
┌──────────────────────┐      ┌──────────────────────┐
│   Redis Cache        │      │  Read Replicas       │
│  • Popular Tags      │      │  (PostgreSQL)        │
│  • Tag Associations  │      │  • Tag Search        │
│  • Search Results    │      │  • Entity Tags       │
└───────┬──────────────┘      └──────────────────────┘
        │
        │ Cache Miss
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DATA LAYER                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL (Primary)                                   │  │
│  │  • Users, Tags, Tag Associations                        │  │
│  │  • Audit Logs                                           │  │
│  │  • ACID Transactions                                     │  │
│  └───────────────────────┬──────────────────────────────────┘  │
└──────────────────────────┼──────────────────────────────────────┘
                           │ Events
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  ASYNC PROCESSING LAYER                        │
│  ┌──────────────────────┐      ┌──────────────────────┐       │
│  │  Message Queue       │      │  Notification        │       │
│  │  (Kafka/RabbitMQ)    │─────▶│  Service             │       │
│  │  • Tag Events        │      │  • Email/Push        │       │
│  └──────────────────────┘      └──────────────────────┘       │
│                                                               │
│  ┌──────────────────────┐                                     │
│  │  Statistics Service  │                                     │
│  │  • Usage Frequency   │                                     │
│  │  • Popular Tags      │                                     │
│  └──────────────────────┘                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component-to-Requirement Mapping

### Functional Requirements (FR) → Components

| FR | Component | How It's Solved |
|---|---|---|
| **FR1: User Roles** | API Gateway + Tag Service | • API Gateway validates JWT token<br>• Tag Service checks role from token<br>• RBAC enforced at service layer |
| **FR2: Tag Lifecycle** | Tag Service + PostgreSQL | • `status` field in `tags` table (Active/Inactive)<br>• Admin-only DELETE endpoint deactivates tags<br>• Audit logs track all state changes |
| **FR3: Hierarchical Tags** | PostgreSQL + Tag Service | • `parent_tag_id` self-referencing FK in `tags` table<br>• Recursive queries for hierarchy traversal<br>• API supports parent-child relationships |
| **FR3: Multi-select Tagging** | Tag Service + PostgreSQL | • `POST /tags/associate` accepts array of `tag_ids`<br>• Batch insert into `tag_associations` table<br>• Transaction ensures atomicity |
| **FR4: Search & Filtering** | Redis Cache + Read Replicas | • Search queries cached in Redis (5min TTL)<br>• Read replicas handle complex filter queries<br>• Indexes on `name`, `status`, `category_id` |
| **FR5: Notifications** | Message Queue + Notification Service | • Tag events published to queue (async)<br>• Notification Service consumes events<br>• Email/push notifications sent to users |

### Non-Functional Requirements (NFR) → Components

| NFR | Component | How It's Solved |
|---|---|---|
| **NFR1: Scalability** | • Read Replicas<br>• Redis Cache<br>• Horizontal Scaling | • Read replicas scale read operations<br>• Cache reduces DB load by 80%<br>• Tag Service instances scale independently |
| **NFR2: Availability (99.9%)** | • Multi-region Deployment<br>• Database Replication<br>• Circuit Breaker | • 3 DB replicas (failover in 30s)<br>• Load balancer removes unhealthy instances<br>• Cache fallback if Redis fails |
| **NFR3: Low Latency (<200ms)** | • Redis Cache<br>• Read Replicas<br>• Database Indexes | • Cache hit: ~5ms (vs 50ms DB query)<br>• Read replicas reduce primary DB load<br>• Indexes optimize query performance |
| **NFR4: Security** | • API Gateway<br>• Tag Service RBAC<br>• Rate Limiting | • JWT validation at gateway<br>• Role checks before operations<br>• Rate limiting prevents abuse |
| **NFR5: Auditability** | • PostgreSQL Audit Logs<br>• Message Queue Events | • All tag operations logged with user_id<br>• Events stored for replay<br>• Compliance queries supported |

---

## Key Design Decisions & Trade-offs

### 1. **Redis Cache vs Direct DB Queries**

**Decision**: Cache-Aside pattern with Redis

**Solves**: NFR3 (Low Latency), NFR1 (Scalability)

**Trade-off**:
- ✅ **Gain**: 90% latency reduction (5ms vs 50ms), 80% DB load reduction
- ❌ **Lose**: Eventual consistency (cache may be stale for 5min), additional infrastructure cost

**Alternative**: Direct DB queries only
- **Why rejected**: Can't meet <200ms latency at scale, DB becomes bottleneck

---

### 2. **Read Replicas vs Single Database**

**Decision**: Primary DB (writes) + Read Replicas (reads)

**Solves**: NFR1 (Scalability), NFR2 (Availability)

**Trade-off**:
- ✅ **Gain**: Scale reads independently, failover capability, read load distribution
- ❌ **Lose**: Replication lag (50-100ms), eventual consistency for reads

**Alternative**: Single database
- **Why rejected**: Can't handle read-heavy workload (80% reads), single point of failure

---

### 3. **Synchronous vs Async Notifications**

**Decision**: Async via Message Queue

**Solves**: FR5 (Notifications), NFR3 (Low Latency)

**Trade-off**:
- ✅ **Gain**: API responds in <30ms (vs 200ms+ with sync), decoupled services, scalable
- ❌ **Lose**: Notifications delayed by 1-2 seconds, message queue complexity

**Alternative**: Synchronous notification calls
- **Why rejected**: Blocks API response, notification failures break tag operations

---

### 4. **Relational DB vs NoSQL**

**Decision**: PostgreSQL (Relational)

**Solves**: FR3 (Hierarchical Tags), NFR5 (Auditability), Data Integrity

**Trade-off**:
- ✅ **Gain**: ACID transactions, complex queries (joins, hierarchies), strong consistency
- ❌ **Lose**: Harder horizontal scaling (need sharding), more complex than NoSQL

**Alternative**: MongoDB/Cassandra
- **Why rejected**: Weaker consistency, complex hierarchical queries harder, audit logs need transactions

---

### 5. **Strong vs Eventual Consistency**

**Decision**: Strong for Tag CRUD, Eventual for Associations

**Solves**: NFR3 (Latency), Data Integrity

**Trade-off**:
- ✅ **Gain**: Fast writes (no cross-service coordination), eventual consistency acceptable for associations
- ❌ **Lose**: Possible stale reads for tag associations (acceptable for this use case)

**Alternative**: Strong consistency everywhere
- **Why rejected**: Higher latency, more conflicts, lower throughput

---

## Data Flow Examples

### Read Path: Get Tags for JIRA Issue
```
Client → API Gateway (Auth) → Tag Service → Redis Cache
  ↓ Cache Hit (5ms)
  Return tags
  
  ↓ Cache Miss
  Tag Service → Read Replica → Return tags (50ms)
  Tag Service → Redis (cache write)
```

**Solves**: NFR3 (Latency), FR4 (Search)

---

### Write Path: Create Tag
```
Client → API Gateway (Auth, Rate Limit) → Tag Service (RBAC Check)
  ↓
Tag Service → PostgreSQL Primary (Write, Audit Log)
  ↓
Tag Service → Redis (Invalidate cache)
  ↓
Tag Service → Message Queue (Publish event)
  ↓
Return 201 Created (30ms)

[Async] Notification Service → Send notifications
```

**Solves**: FR1 (User Roles), FR2 (Tag Lifecycle), FR5 (Notifications), NFR5 (Auditability)

---

## Failure Handling

| Failure | Handling | Impact |
|---|---|---|
| **Redis Down** | Fallback to DB, Circuit Breaker opens | Latency: 5ms → 50ms (acceptable) |
| **Primary DB Down** | Auto-failover to replica (30s) | Writes blocked 30s, reads continue |
| **Message Queue Down** | Local retry queue, degraded mode | Notifications delayed (non-critical) |
| **Tag Service Down** | Load balancer removes instance | Other instances handle traffic |

**Solves**: NFR2 (Availability)

---

## Summary

**Core Architecture**: Microservices with Cache-Aside, CQRS, Event-Driven patterns

**Key Components**:
1. **API Gateway** → Security, Rate Limiting (NFR4)
2. **Tag Service** → Business Logic (All FRs)
3. **Redis Cache** → Performance (NFR3, NFR1)
4. **PostgreSQL** → Data Persistence (FR2, FR3, NFR5)
5. **Read Replicas** → Scalability (NFR1, NFR2)
6. **Message Queue** → Async Processing (FR5, NFR3)

**Trade-offs Made**:
- Cache for speed (eventual consistency acceptable)
- Read replicas for scale (replication lag acceptable)
- Async notifications for performance (slight delay acceptable)
- Relational DB for integrity (scaling complexity acceptable)

**Result**: System meets all FRs and NFRs with acceptable trade-offs.
