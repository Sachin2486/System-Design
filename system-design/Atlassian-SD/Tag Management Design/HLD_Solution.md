# Tag Management System - High-Level Design (HLD)

Following the structured HLD framework for interview-ready design.

---

## 1️⃣ Understanding the Problem

### Problem Restatement
Design a **Tag Management System** that enables unified tag creation, management, and application across **JIRA** (for issues) and **Confluence** (for pages). The system must ensure tag consistency, support hierarchical relationships, and provide role-based access control for different user types.

### Scope Clarification

**In Scope:**
- Tag CRUD operations (create, read, update, deactivate)
- Tag association with JIRA issues and Confluence pages
- Role-based access control (Admin, Developer, Content Creator)
- Hierarchical tag relationships (parent-child)
- Search and filtering capabilities
- Notification system for tag lifecycle events
- Audit logging for compliance

**Out of Scope:**
- Integration with external authentication systems (assume existing auth)
- Real-time collaboration features
- Tag recommendation using ML
- Advanced analytics and reporting dashboards
- Tag versioning/history beyond audit logs
- Bulk import/export of tags

### Critical Clarifying Questions
1. **Scale**: How many tags? (Assumption: 10K-100K tags, millions of associations)
2. **Integration**: Is this a standalone service or integrated into JIRA/Confluence? (Assumption: Standalone microservice)
3. **Notifications**: Real-time push or async? (Assumption: Async via message queue)
4. **Deactivated Tags**: Should they remain visible on existing entities? (Assumption: Yes, but marked as inactive)

### Assumptions
- Existing user authentication system (JIRA/Confluence SSO)
- Tags are global across all projects (not project-specific)
- Read-heavy workload (80% reads, 20% writes)
- Eventual consistency acceptable for tag updates
- 99.9% availability requirement

---

## 2️⃣ Functional Requirements (FR)

### P0 (Must Have)
1. **Tag Management**
   - Admins can create, update, and deactivate tags
   - Tags have name, description, status (Active/Inactive)
   - Support hierarchical tags (parent-child relationships)

2. **Tag Application**
   - Developers can apply tags to JIRA issues
   - Content Creators can apply tags to Confluence pages
   - Multi-select tagging support

3. **Search & Discovery**
   - Search tags by name, category
   - Filter by status, usage frequency, hierarchy
   - Retrieve tags for a specific entity (issue/page)

4. **Role-Based Access**
   - Admin: Full tag management + view statistics
   - Developer: Apply tags to issues only
   - Content Creator: Apply tags to pages only

5. **Notifications**
   - Notify users when tags are created/modified/deactivated

### P1 (Should Have)
6. **Tag Categories**
   - Group tags into categories for better organization

7. **Usage Statistics**
   - View tag usage frequency across projects

8. **Audit Trail**
   - Log all tag-related actions for compliance

---

## 3️⃣ Non-Functional Requirements (NFR)

### Scalability
- Handle **10K-100K tags** efficiently
- Support **millions of tag associations** (tags → issues/pages)
- Scale horizontally for read operations
- Database sharding strategy for tag associations

### Performance
- **Latency**: < 200ms for tag search/retrieval (p95)
- **Throughput**: 10K reads/sec, 1K writes/sec
- **Cache hit ratio**: > 80% for popular tags

### Availability
- **Uptime**: 99.9% (8.76 hours downtime/year)
- **Multi-region deployment** with failover
- **Graceful degradation** if cache fails

### Consistency
- **Strong consistency** for tag CRUD operations
- **Eventual consistency** acceptable for tag association updates
- **Read-after-write consistency** for tag creation

### Reliability
- **Durability**: Zero data loss for tag operations
- **Replication**: 3 replicas for primary database
- **Backup**: Daily backups with 30-day retention

### Security
- **Authentication**: JWT/OAuth tokens
- **Authorization**: Role-based access control (RBAC)
- **Rate limiting**: Prevent abuse (100 req/min per user)
- **Data encryption**: TLS in transit, encryption at rest

### Device Compatibility
- Web (JIRA/Confluence UI)
- Mobile apps (via REST APIs)
- CLI tools (for bulk operations)

---

## 4️⃣ Planning the Approach

### Architecture Philosophy
- **Microservices**: Standalone tag service, loosely coupled with JIRA/Confluence
- **Read-Heavy**: Optimize for reads (caching, read replicas)
- **Event-Driven**: Use message queue for notifications and async updates

### Key Design Decisions

**1. Online vs Batch Processing**
- **Online**: Tag CRUD, tag associations (synchronous)
- **Batch**: Usage statistics computation, notification batching (asynchronous)

**2. Real-time vs Eventual Consistency**
- **Real-time**: Tag CRUD operations (strong consistency)
- **Eventual**: Tag association updates, usage statistics (eventual consistency)

**3. Push vs Pull**
- **Push**: Notifications via message queue (event-driven)
- **Pull**: Tag search/retrieval (on-demand)

**4. Read-Heavy Optimization**
- **Cache-first**: Redis for popular tags and associations
- **Read replicas**: Database replication for read scaling
- **CDN**: For static tag metadata (if applicable)

**5. Data Model**
- **Relational DB**: PostgreSQL (ACID compliance, complex queries)
- **Cache**: Redis (fast lookups, sorted sets for rankings)
- **Message Queue**: Kafka/RabbitMQ (notifications, events)

---

## 5️⃣ Define Core Domain Entities

### 1. User
**Fields:**
- `id` (UUID, PK)
- `name` (String)
- `email` (String, unique)
- `role` (Enum: Admin, Developer, Content Creator)
- `created_at`, `updated_at`

**Relationships:**
- 1:N → AuditLogs (user performs actions)
- 1:N → Notifications (user receives notifications)

### 2. Tag
**Fields:**
- `id` (UUID, PK)
- `name` (String, unique)
- `description` (Text, nullable)
- `status` (Enum: Active, Inactive)
- `category_id` (UUID, FK → TagCategory)
- `parent_tag_id` (UUID, FK → Tag, self-referencing, nullable)
- `created_at`, `updated_at`

**Relationships:**
- N:1 → TagCategory (tag belongs to category)
- 1:N → Tag (self-referencing, parent-child)
- 1:N → TagAssociation (tag applied to entities)
- 1:N → AuditLogs (tag changes logged)

### 3. TagCategory
**Fields:**
- `id` (UUID, PK)
- `name` (String, unique)
- `description` (Text, nullable)
- `created_at`, `updated_at`

**Relationships:**
- 1:N → Tag (category contains tags)

### 4. TagAssociation
**Fields:**
- `id` (UUID, PK)
- `tag_id` (UUID, FK → Tag)
- `entity_type` (Enum: JIRA, Confluence)
- `entity_id` (UUID, identifies issue/page)
- `created_at`

**Relationships:**
- N:1 → Tag (association references tag)
- Composite key: (tag_id, entity_type, entity_id) for uniqueness

### 5. Permission
**Fields:**
- `id` (UUID, PK)
- `role` (Enum: Admin, Developer, Content Creator)
- `action` (String: CreateTag, UpdateTag, DeleteTag, ApplyTag, etc.)
- `created_at`

**Relationships:**
- N:1 → Role (permissions mapped to roles)

### 6. Notification
**Fields:**
- `id` (UUID, PK)
- `user_id` (UUID, FK → User)
- `message` (Text)
- `status` (Enum: Sent, Read)
- `created_at`

**Relationships:**
- N:1 → User (notification sent to user)

### 7. AuditLog
**Fields:**
- `id` (UUID, PK)
- `action` (String: CreateTag, UpdateTag, DeleteTag, etc.)
- `user_id` (UUID, FK → User)
- `tag_id` (UUID, FK → Tag, nullable)
- `metadata` (JSON, stores additional context)
- `timestamp` (Timestamp)

**Relationships:**
- N:1 → User (action performed by user)
- N:1 → Tag (action on tag)

---

## 6️⃣ API / System Interfaces

### Authentication APIs
```
POST /api/v1/auth/login
Request: { email, password }
Response: { token, user_id, role, expires_at }

POST /api/v1/auth/refresh
Request: { refresh_token }
Response: { token, expires_at }
```

### Tag Management APIs

**Create Tag** (Admin only)
```
POST /api/v1/tags
Request: {
  name: string,
  description?: string,
  category_id?: uuid,
  parent_tag_id?: uuid
}
Response: {
  id: uuid,
  name: string,
  status: "Active",
  created_at: timestamp
}
```

**Update Tag** (Admin only)
```
PUT /api/v1/tags/{tag_id}
Request: {
  name?: string,
  description?: string,
  category_id?: uuid,
  parent_tag_id?: uuid
}
Response: { id, name, status, updated_at }
```

**Deactivate Tag** (Admin only)
```
DELETE /api/v1/tags/{tag_id}
Response: { id, status: "Inactive" }
```

**Get Tag**
```
GET /api/v1/tags/{tag_id}
Response: {
  id, name, description, status,
  category: { id, name },
  parent_tag: { id, name },
  usage_count: number
}
```

**Search Tags**
```
GET /api/v1/tags/search?q={query}&category={category_id}&status={status}&page={page}&limit={limit}
Response: {
  tags: [Tag],
  total: number,
  page: number,
  limit: number
}
```

**Get Tag Hierarchy**
```
GET /api/v1/tags/{tag_id}/hierarchy
Response: {
  tag: Tag,
  children: [Tag],
  parent: Tag | null
}
```

### Tag Association APIs

**Associate Tags** (Developer/Content Creator)
```
POST /api/v1/tags/associate
Request: {
  tag_ids: [uuid],
  entity_type: "JIRA" | "Confluence",
  entity_id: uuid
}
Response: {
  associations: [{
    id, tag_id, entity_type, entity_id
  }]
}
```

**Remove Tag Association**
```
DELETE /api/v1/tags/associate/{association_id}
Response: { success: true }
```

**Get Tags for Entity**
```
GET /api/v1/entities/{entity_type}/{entity_id}/tags
Response: {
  tags: [Tag],
  total: number
}
```

**Get Entities by Tag**
```
GET /api/v1/tags/{tag_id}/entities?entity_type={type}&page={page}&limit={limit}
Response: {
  entities: [{ entity_type, entity_id }],
  total: number
}
```

### Statistics APIs (Admin only)

**Tag Usage Statistics**
```
GET /api/v1/tags/{tag_id}/statistics
Response: {
  tag_id, tag_name,
  total_usage: number,
  jira_usage: number,
  confluence_usage: number,
  usage_by_project: [{ project_id, count }]
}
```

**Popular Tags**
```
GET /api/v1/tags/popular?limit={limit}&time_range={7d|30d|all}
Response: {
  tags: [{ id, name, usage_count }]
}
```

### Notification APIs

**Get Notifications**
```
GET /api/v1/notifications?status={Sent|Read}&page={page}&limit={limit}
Response: {
  notifications: [Notification],
  total: number
}
```

**Mark Notification as Read**
```
PUT /api/v1/notifications/{notification_id}/read
Response: { success: true }
```

---

## 7️⃣ High-Level Architecture (Incremental)

### Phase 1: Basic System (MVP)

```
┌─────────────┐
│   Client    │ (JIRA/Confluence UI)
└──────┬──────┘
       │ HTTPS
       ▼
┌─────────────────┐
│  API Gateway     │ (Load Balancer, Rate Limiting)
└──────┬───────────┘
       │
       ▼
┌─────────────────┐
│  Tag Service    │ (REST API, Business Logic)
└──────┬───────────┘
       │
       ▼
┌─────────────────┐
│  PostgreSQL     │ (Primary Database)
└─────────────────┘
```

**Components:**
- **Client**: Web UI (JIRA/Confluence plugins)
- **API Gateway**: Request routing, authentication, rate limiting
- **Tag Service**: Core business logic, CRUD operations
- **PostgreSQL**: Primary data store

### Phase 2: Add Caching & Read Replicas

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  API Gateway     │
└──────┬───────────┘
       │
       ▼
┌─────────────────┐      ┌──────────────┐
│  Tag Service    │─────▶│    Redis     │ (Cache)
└──────┬───────────┘      └──────────────┘
       │
       ├─────────────────┐
       │                 │
       ▼                 ▼
┌──────────────┐  ┌──────────────┐
│  PostgreSQL  │  │ Read Replica │ (Read Scaling)
│  (Primary)   │  │  (PostgreSQL)│
└──────────────┘  └──────────────┘
```

**Additions:**
- **Redis**: Cache popular tags, tag associations (Cache-Aside pattern)
- **Read Replicas**: Scale read operations

### Phase 3: Add Async Processing & Notifications

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  API Gateway     │
└──────┬───────────┘
       │
       ▼
┌─────────────────┐      ┌──────────────┐
│  Tag Service    │─────▶│    Redis     │
└──────┬───────────┘      └──────────────┘
       │
       ├─────────────────┐
       │                 │
       ▼                 ▼
┌──────────────┐  ┌──────────────┐
│  PostgreSQL  │  │ Read Replica │
│  (Primary)   │  │  (PostgreSQL)│
└──────┬───────┘  └──────────────┘
       │
       │ Events
       ▼
┌─────────────────┐
│ Message Queue   │ (Kafka/RabbitMQ)
│  (Tag Events)   │
└──────┬──────────┘
       │
       ├─────────────────┐
       │                 │
       ▼                 ▼
┌──────────────┐  ┌──────────────┐
│ Notification │  │  Statistics  │
│   Service    │  │   Service    │
└──────────────┘  └──────────────┘
```

**Additions:**
- **Message Queue**: Async event processing
- **Notification Service**: Send notifications on tag events
- **Statistics Service**: Compute usage statistics asynchronously

### Phase 4: Full Production System

```
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │ HTTPS
                           ▼
                    ┌─────────────────┐
                    │  API Gateway     │ (CDN, WAF)
                    └──────┬───────────┘
                           │
                           ▼
                    ┌─────────────────┐
                    │  Tag Service    │ (Microservice)
                    │  (Multiple      │
                    │   Instances)    │
                    └──────┬───────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│    Redis     │  │  PostgreSQL  │  │ Message Queue│
│  (Cluster)   │  │  (Primary +  │  │   (Kafka)    │
│              │  │   Replicas)  │  │              │
└──────────────┘  └──────┬───────┘  └──────┬───────┘
                         │                  │
                         │                  │
                         ▼                  ▼
                    ┌──────────────┐  ┌──────────────┐
                    │  Notification│  │  Statistics  │
                    │   Service    │  │   Service    │
                    └──────────────┘  └──────────────┘
                         │                  │
                         ▼                  ▼
                    ┌──────────────────────────┐
                    │   Monitoring & Logging   │
                    │  (Prometheus, ELK Stack) │
                    └──────────────────────────┘
```

**Full Production Features:**
- **Multi-region**: Geographic distribution
- **Monitoring**: Prometheus, Grafana
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Service Discovery**: Consul/Eureka
- **Circuit Breaker**: Resilience patterns

---

## 8️⃣ Data Flow (Mandatory)

### Read Path: Get Tags for Entity

```
1. Client → API Gateway
   GET /api/v1/entities/JIRA/{issue_id}/tags

2. API Gateway → Tag Service
   - Authenticate user (JWT validation)
   - Rate limiting check

3. Tag Service → Redis (Cache Lookup)
   Key: "entity:JIRA:{issue_id}:tags"
   - Cache HIT: Return tags immediately
   - Cache MISS: Continue to step 4

4. Tag Service → PostgreSQL (Read Replica)
   SELECT t.* FROM tags t
   JOIN tag_associations ta ON t.id = ta.tag_id
   WHERE ta.entity_type = 'JIRA' AND ta.entity_id = {issue_id}

5. Tag Service → Redis (Cache Write)
   Set cache with TTL (5 minutes)

6. Tag Service → API Gateway → Client
   Return tags list
```

**Latency Breakdown:**
- Cache Hit: ~5ms
- Cache Miss: ~50ms (DB query) + ~2ms (cache write) = ~52ms

### Write Path: Create Tag

```
1. Client → API Gateway
   POST /api/v1/tags
   { name: "bug", description: "..." }

2. API Gateway → Tag Service
   - Authenticate user (Admin role required)
   - Validate request

3. Tag Service → PostgreSQL (Primary)
   BEGIN TRANSACTION
   - Check tag name uniqueness
   - INSERT INTO tags (name, description, status)
   - INSERT INTO audit_logs (action, user_id, tag_id)
   COMMIT TRANSACTION

4. Tag Service → Redis (Invalidate Cache)
   - Delete related cache keys
   - Pattern: "tag:*", "tags:search:*"

5. Tag Service → Message Queue (Async)
   Publish event: { event_type: "TagCreated", tag_id, user_id }

6. Tag Service → API Gateway → Client
   Return created tag (201 Created)

7. [Async] Message Queue → Notification Service
   - Fetch subscribed users
   - Create notifications
   - Send via email/push

8. [Async] Message Queue → Statistics Service
   - Update tag usage metrics (if needed)
```

**Latency Breakdown:**
- DB Write: ~20ms
- Cache Invalidation: ~2ms
- Message Publish: ~5ms
- Total: ~27ms (synchronous)
- Notifications: Async (doesn't block)

### Write Path: Associate Tag with Entity

```
1. Client → API Gateway
   POST /api/v1/tags/associate
   { tag_ids: [uuid1, uuid2], entity_type: "JIRA", entity_id: uuid }

2. API Gateway → Tag Service
   - Authenticate user (Developer/Content Creator)
   - Validate permissions (role can tag entity_type)

3. Tag Service → PostgreSQL (Primary)
   BEGIN TRANSACTION
   - Validate tags exist and are Active
   - INSERT INTO tag_associations (tag_id, entity_type, entity_id)
     ON CONFLICT DO NOTHING (idempotent)
   COMMIT TRANSACTION

4. Tag Service → Redis (Invalidate Cache)
   - Delete: "entity:{entity_type}:{entity_id}:tags"
   - Delete: "tag:{tag_id}:entities"

5. Tag Service → Message Queue (Async)
   Publish: { event_type: "TagAssociated", tag_ids, entity_type, entity_id }

6. Tag Service → API Gateway → Client
   Return associations (200 OK)

7. [Async] Statistics Service
   - Update tag usage counters
   - Update popular tags leaderboard (Redis sorted set)
```

### Cache Hit vs Miss Flow

**Cache Hit (80% of requests):**
```
Request → API Gateway → Tag Service → Redis → Return (5ms)
```

**Cache Miss (20% of requests):**
```
Request → API Gateway → Tag Service → Redis (miss) → PostgreSQL → Redis (write) → Return (52ms)
```

### Re-computation Flow: Tag Usage Statistics

```
1. Scheduled Job (Every hour)
   - Query PostgreSQL for tag usage counts
   - Aggregate by tag_id, entity_type, project

2. Statistics Service → Redis
   - Update sorted set: "tags:popular:{time_range}"
   - Update hash: "tag:{tag_id}:stats"

3. Cache TTL: 1 hour (matches computation frequency)
```

---

## 9️⃣ Database Schema Design (MANDATORY)

### Tables

#### 1. users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role ENUM('Admin', 'Developer', 'Content Creator') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
```

#### 2. tag_categories
```sql
CREATE TABLE tag_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tag_categories_name ON tag_categories(name);
```

#### 3. tags
```sql
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    status ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active',
    category_id UUID REFERENCES tag_categories(id) ON DELETE SET NULL,
    parent_tag_id UUID REFERENCES tags(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (parent_tag_id != id) -- Prevent self-reference
);

CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_tags_status ON tags(status);
CREATE INDEX idx_tags_category ON tags(category_id);
CREATE INDEX idx_tags_parent ON tags(parent_tag_id);
CREATE INDEX idx_tags_name_status ON tags(name, status); -- Composite for search
```

#### 4. tag_associations
```sql
CREATE TABLE tag_associations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    entity_type ENUM('JIRA', 'Confluence') NOT NULL,
    entity_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tag_id, entity_type, entity_id) -- Prevent duplicate associations
);

CREATE INDEX idx_tag_associations_tag ON tag_associations(tag_id);
CREATE INDEX idx_tag_associations_entity ON tag_associations(entity_type, entity_id);
CREATE INDEX idx_tag_associations_composite ON tag_associations(tag_id, entity_type, entity_id);
```

#### 5. permissions
```sql
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role ENUM('Admin', 'Developer', 'Content Creator') NOT NULL,
    action VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role, action)
);

CREATE INDEX idx_permissions_role ON permissions(role);
```

#### 6. notifications
```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    status ENUM('Sent', 'Read') NOT NULL DEFAULT 'Sent',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);
```

#### 7. audit_logs
```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    tag_id UUID REFERENCES tags(id) ON DELETE SET NULL,
    metadata JSONB, -- Store additional context
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_tag ON audit_logs(tag_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
```

### Relationships Summary

```
users (1) ──→ (N) audit_logs
users (1) ──→ (N) notifications

tag_categories (1) ──→ (N) tags
tags (1) ──→ (N) tags (self-referencing, parent-child)
tags (1) ──→ (N) tag_associations
tags (1) ──→ (N) audit_logs

tag_associations (N) ──→ (1) tags
```

### Partitioning / Sharding Strategy

**For Scale (Future):**

1. **tag_associations Table Sharding**
   - **Shard Key**: `entity_type` + hash(`entity_id`)
   - **Reason**: Most queries filter by entity_type and entity_id
   - **Shards**: 4 shards (can scale to 16+)
   - **Routing**: Application-level shard router

2. **audit_logs Table Partitioning**
   - **Partition Key**: `timestamp` (monthly partitions)
   - **Reason**: Time-based queries, old data can be archived
   - **Retention**: 2 years active, archive older data

3. **tags Table**
   - **No sharding initially** (10K-100K tags fits in single DB)
   - **Future**: Shard by `category_id` if needed

### Indexing Strategy

**Critical Indexes:**
- `tags(name, status)` - Composite for search queries
- `tag_associations(entity_type, entity_id)` - Fast entity tag lookup
- `tag_associations(tag_id)` - Fast tag usage queries
- `audit_logs(timestamp)` - Time-range queries for compliance

---

## 🔟 Design Patterns Used

### 1. Cache-Aside Pattern
**What**: Application manages cache directly
**Why**: 
- Simple to implement
- Cache failures don't break system (fallback to DB)
- Full control over cache invalidation

**Alternative**: Write-Through (rejected - higher write latency)

### 2. Event-Driven Architecture
**What**: Tag events published to message queue
**Why**:
- Decouple notification/statistics from main flow
- Async processing improves response time
- Easy to add new consumers (analytics, ML)

**Alternative**: Synchronous calls (rejected - blocks API response)

### 3. CQRS (Command Query Responsibility Segregation)
**What**: Separate read (replicas) and write (primary) paths
**Why**:
- Scale reads independently
- Optimize read queries without affecting writes
- Read replicas can have different indexes

**Alternative**: Single database (rejected - doesn't scale reads)

### 4. Leaderboard Pattern (Redis Sorted Sets)
**What**: Use Redis sorted sets for popular tags
**Why**:
- O(log N) updates, O(log N + M) range queries
- Real-time rankings without DB aggregation
- Automatic expiration support

**Alternative**: DB aggregation (rejected - too slow for real-time)

### 5. Idempotency Pattern
**What**: Tag associations use `ON CONFLICT DO NOTHING`
**Why**:
- Prevents duplicate associations on retries
- Safe for concurrent requests
- Better UX (no errors on duplicate tags)

**Alternative**: Check-then-insert (rejected - race conditions)

### 6. Circuit Breaker Pattern
**What**: Fail fast if cache/DB is down
**Why**:
- Prevents cascading failures
- Graceful degradation (return cached data or error)
- System resilience

**Alternative**: Always retry (rejected - can cause timeouts)

---

## 1️⃣1️⃣ Scalability & Trade-offs

### Decision 1: Redis vs Database Sorting for Popular Tags

**Chosen**: Redis Sorted Sets
**Why**:
- O(log N) updates vs O(N log N) DB sort
- Real-time rankings without expensive queries
- Can handle millions of tags efficiently

**Trade-offs**:
- ✅ **Gain**: Sub-10ms query latency
- ❌ **Lose**: Additional infrastructure, eventual consistency

**Alternative**: PostgreSQL with materialized views
- **Why rejected**: Refresh overhead, stale data, slower queries

### Decision 2: Push vs Pull for Notifications

**Chosen**: Push (Event-Driven)
**Why**:
- Real-time delivery
- Scalable (multiple consumers)
- Decouples notification logic

**Trade-offs**:
- ✅ **Gain**: Immediate notifications, scalable
- ❌ **Lose**: Message queue complexity, potential message loss

**Alternative**: Polling
- **Why rejected**: High DB load, delayed notifications

### Decision 3: Strong vs Eventual Consistency for Tag Updates

**Chosen**: Strong consistency for CRUD, Eventual for associations
**Why**:
- Tag CRUD: Must be immediately visible (strong)
- Tag associations: Can tolerate slight delay (eventual)

**Trade-offs**:
- ✅ **Gain**: Fast writes, eventual consistency reduces conflicts
- ❌ **Lose**: Possible stale reads for associations (acceptable)

**Alternative**: Strong consistency everywhere
- **Why rejected**: Higher latency, more conflicts, lower throughput

### Decision 4: Horizontal vs Vertical Scaling

**Chosen**: Horizontal (microservices, read replicas)
**Why**:
- Scale independently per component
- Cost-effective (commodity hardware)
- Better fault tolerance

**Trade-offs**:
- ✅ **Gain**: Unlimited scale, fault tolerance
- ❌ **Lose**: Network latency, distributed system complexity

**Alternative**: Vertical scaling
- **Why rejected**: Limited by single machine, single point of failure

### Decision 5: Relational DB vs NoSQL

**Chosen**: PostgreSQL (Relational)
**Why**:
- ACID compliance (critical for tag management)
- Complex queries (hierarchical tags, joins)
- Mature ecosystem, strong consistency

**Trade-offs**:
- ✅ **Gain**: Data integrity, complex queries, transactions
- ❌ **Lose**: Horizontal scaling harder (need sharding)

**Alternative**: MongoDB/Cassandra
- **Why rejected**: Weaker consistency guarantees, complex queries harder

---

## 1️⃣2️⃣ Failure Handling & Monitoring

### Cache Failure (Redis Down)

**Handling**:
1. **Circuit Breaker**: Open circuit after 5 failures
2. **Fallback**: Direct DB queries (slower but functional)
3. **Alert**: PagerDuty alert to on-call engineer
4. **Recovery**: Auto-retry with exponential backoff

**Impact**: Latency increases from 5ms → 50ms (acceptable)

### Database Failure (Primary DB Down)

**Handling**:
1. **Failover**: Automatic promotion of read replica to primary
2. **Read-Only Mode**: Temporarily allow reads only
3. **Alert**: Critical alert, immediate response
4. **Data Loss**: Zero (synchronous replication)

**Impact**: Writes blocked until failover (30-60 seconds)

### Message Queue Failure (Kafka Down)

**Handling**:
1. **Retry Queue**: Store failed events in local queue
2. **Degraded Mode**: Continue core operations, skip notifications
3. **Alert**: Warning alert (non-critical)
4. **Recovery**: Replay events when queue recovers

**Impact**: Notifications delayed (non-critical)

### Partial Service Failure

**Handling**:
1. **Health Checks**: /health endpoint for each service
2. **Load Balancer**: Remove unhealthy instances
3. **Graceful Degradation**: Return cached data or error message
4. **Circuit Breaker**: Prevent cascading failures

### Monitoring & Metrics

**Key Metrics**:
- **Latency**: p50, p95, p99 for tag operations
- **Throughput**: Requests/sec, cache hit ratio
- **Error Rate**: 4xx, 5xx errors per minute
- **Availability**: Uptime percentage (target: 99.9%)

**Tools**:
- **Prometheus**: Metrics collection
- **Grafana**: Dashboards and visualization
- **ELK Stack**: Log aggregation and analysis
- **PagerDuty**: Alerting and on-call

**Alerts**:
- **Critical**: DB down, service unavailable
- **Warning**: High latency (>200ms p95), low cache hit ratio (<70%)
- **Info**: Tag creation rate spike, notification queue backlog

---

## 1️⃣3️⃣ Security (Brief but Clear)

### Authentication
- **Method**: JWT tokens (issued by JIRA/Confluence SSO)
- **Token Expiry**: 15 minutes (access), 7 days (refresh)
- **Validation**: API Gateway validates JWT before forwarding

### Authorization
- **RBAC**: Role-based access control (Admin, Developer, Content Creator)
- **Permission Check**: Tag Service validates role before operations
- **Example**: Only Admin can create/delete tags

### Rate Limiting
- **Per User**: 100 requests/minute
- **Per IP**: 1000 requests/minute
- **Enforcement**: API Gateway (token bucket algorithm)
- **Response**: 429 Too Many Requests

### Abuse Prevention
- **Input Validation**: Sanitize tag names (prevent SQL injection, XSS)
- **Tag Name Limits**: Max 50 characters, alphanumeric + hyphens only
- **Association Limits**: Max 10 tags per entity (prevent spam)
- **Audit Logging**: All actions logged for forensics

### Data Protection
- **Encryption in Transit**: TLS 1.3 (HTTPS)
- **Encryption at Rest**: Database encryption (AES-256)
- **PII Handling**: User emails encrypted in audit logs
- **GDPR Compliance**: User data deletion support

---

## 1️⃣4️⃣ What's Out of Scope

### Explicitly Out of Scope

1. **Personalization**
   - Tag recommendations based on user history
   - ML-based tag suggestions
   - User-specific tag preferences

2. **Advanced Analytics**
   - Real-time dashboards
   - Predictive analytics
   - Tag trend analysis over time

3. **Tag Versioning**
   - Tag history/rollback beyond audit logs
   - Tag merge/split operations
   - Tag migration tools

4. **Bulk Operations**
   - Bulk tag import/export (CSV)
   - Bulk tag association updates
   - Tag migration between systems

5. **Real-Time Collaboration**
   - Live tag editing
   - Conflict resolution for concurrent edits
   - WebSocket for real-time updates

6. **External Integrations**
   - Third-party tag systems
   - Tag sync with external tools
   - API for external tag providers

**Reason**: Focus on core tag management first, extend later based on user feedback.

---

## 1️⃣5️⃣ Extensibility

### Adding New Metrics

**Current**: Usage count, popularity
**Future**: View count, click-through rate, tag co-occurrence

**How**:
1. Add new columns to `tags` table or separate `tag_metrics` table
2. Publish new events to message queue
3. Statistics Service consumes events and updates metrics
4. No changes to core Tag Service (loosely coupled)

### Changing Ranking Logic

**Current**: Usage frequency
**Future**: Weighted scoring (recency + frequency + relevance)

**How**:
1. Ranking logic in Statistics Service (not in Tag Service)
2. Update Redis sorted set scoring algorithm
3. No schema changes needed
4. Can A/B test different algorithms

### Adding New Entity Types

**Current**: JIRA, Confluence
**Future**: Bitbucket, Trello

**How**:
1. Extend `entity_type` ENUM in `tag_associations`
2. Add permission checks for new entity types
3. Update API to support new types
4. Backward compatible (existing associations unchanged)

### Supporting Tag Hierarchies (Multi-level)

**Current**: Single parent-child level
**Future**: N-level hierarchies (tags → sub-tags → sub-sub-tags)

**How**:
1. Use recursive CTE queries for hierarchy traversal
2. Cache hierarchy in Redis (tree structure)
3. Add `depth` column to `tags` for optimization
4. API already supports `parent_tag_id` (extensible)

### Adding Tag Synonyms

**Future**: "bug" = "defect" = "issue"

**How**:
1. New table: `tag_synonyms` (tag_id, synonym_tag_id)
2. Search API expands query to include synonyms
3. Cache synonym mappings in Redis
4. No breaking changes to existing APIs

### Multi-Tenancy Support

**Future**: Tags scoped to organizations/workspaces

**How**:
1. Add `organization_id` to `tags` and `tag_associations`
2. Shard by `organization_id`
3. API Gateway adds org context from JWT
4. Backward compatible (default org for existing data)

---

## Summary

This HLD provides a **production-ready design** for a Tag Management System that:
- ✅ Scales to 100K+ tags and millions of associations
- ✅ Achieves <200ms latency with caching
- ✅ Maintains 99.9% availability
- ✅ Supports extensibility without rewrites
- ✅ Follows industry best practices (Cache-Aside, Event-Driven, CQRS)

**Key Strengths**:
- Incremental architecture (start simple, scale as needed)
- Clear trade-offs for every decision
- Comprehensive failure handling
- Extensible design for future requirements

---

**Next Steps** (if building):
1. Implement MVP (Phase 1)
2. Add caching and monitoring
3. Load test and optimize
4. Add async processing
5. Scale horizontally as needed
