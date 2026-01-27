# ✅ HLD (High-Level Design) Rules / Template

**Use this framework in the same order, every single time.**

---

## 1️⃣ Understanding the Problem

* Restate the problem in **your own words**
* Clarify **scope** (what is included vs excluded)
* Ask **only critical clarifying questions**
* Call out assumptions explicitly

> *This shows structured thinking before jumping into design.*

---

## 2️⃣ Functional Requirements (FR)

* List **must-have features**
* Keep them **crisp and numbered**
* Prioritize if needed (P0 / P1)

**Example:**
* Users can view top pages
* System refreshes when rankings change

---

## 3️⃣ Non-Functional Requirements (NFR)

Always include:

* Scalability
* Performance (latency, throughput)
* Availability
* Consistency
* Reliability
* Security (brief)
* Device compatibility (if relevant)

> Interviewers judge seniority heavily here.

---

## 4️⃣ Planning the Approach (Very Important)

Before drawing architecture:

* Explain **how you will approach the solution**
* Online vs batch?
* Real-time vs eventual?
* Push vs pull?
* Read-heavy vs write-heavy?

> This separates **thinkers** from diagram-drawers.

---

## 5️⃣ Define Core Domain Entities

Clearly define main objects:

* User
* Page
* Like / Metric
* Ranking

Mention:

* Key fields
* Relationships (1-N, N-N)

---

## 6️⃣ API / System Interfaces

Design APIs aligned with FRs:

* Request / Response shape
* Pagination
* Sorting
* Caching headers if relevant

**Example:**
* `GET /top-pages`
* `POST /like`

---

## 7️⃣ High-Level Architecture (Incremental)

Design step-by-step:

* Client (Web / Mobile)
* API Gateway
* Services
* Cache
* DB
* Async components (Kafka / Queue)

📌 **Do NOT dump everything at once**
Build it as requirements grow.

---

## 8️⃣ Data Flow (Mandatory)

Explain **how data moves**:

* Read path
* Write path
* Cache hit vs miss
* Re-computation flow

> This is where most candidates fail — you won't.

---

## 9️⃣ Database Schema Design (MANDATORY)

Always include:

* Tables / Collections
* Primary keys
* Indexes
* Relationships
* Partitioning / sharding idea (if scale)

> Skipping DB = incomplete HLD ❌

---

## 🔟 Design Patterns Used

Explicitly name them:

* Cache-Aside
* Pub-Sub
* CQRS
* Event-Driven
* Leaderboard / Ranking pattern

Also mention **why chosen over alternatives**.

---

## 1️⃣1️⃣ Scalability & Trade-offs

For each major decision:

* Why this?
* What are alternatives?
* What do we lose / gain?

**Example:**
* Redis vs DB sorting
* Push vs Polling

---

## 1️⃣2️⃣ Failure Handling & Monitoring (Brief)

Cover:

* Cache failure
* DB failure
* Message loss
* Metrics & alerts

---

## 1️⃣3️⃣ Security (Brief but Clear)

* Auth
* Rate limiting
* Abuse prevention

---

## 1️⃣4️⃣ What's Out of Scope

Explicitly state:

* Personalization (if not needed)
* ML ranking
* Offline analytics

> This shows maturity and control.

---

## 1️⃣5️⃣ Extensibility

Explain:

* How new metrics can be added
* How ranking logic can change
* How system evolves without rewrite

---

## 🧠 Golden Rule

> **Every design decision must have a reason, an alternative, and a justification.**

That's your **HelloInterview-level HLD playbook**.
