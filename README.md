# Split Bill REST API — Allo Bank Backend Developer Solution

A production-grade **Spring Boot REST API** for managing shared group expenses and computing optimal debt settlements. Built for the Allo Bank Backend Developer Challenge.

---

## 📋 Personalization Details

- **Candidate GitHub Username**: `fabmeep`
- **Calculated `service_charge_pct`**: `0%`
  - **Computation**: 
    $$\text{ASCII sum} = 'f'(102) + 'a'(97) + 'b'(98) + 'm'(109) + 'e'(101) + 'e'(101) + 'p'(112) = 720$$
    $$\text{service\_charge\_pct} = 720 \pmod{10} = \mathbf{0}$$
- **`service_charge_amount`**: $0\% \times \text{Total Expenses} = \mathbf{0.00}$

> Both fields are computed dynamically at runtime by the `PersonalizationService` and included in every settlement summary response.

---

## 💡 Submission Question & Answer

> **"What was the hardest design decision you made while building this, and what trade-off did you accept?"**

The hardest architectural decision was balancing **exact financial precision during uneven expense splitting** against a **greedy debt settlement algorithm**. To eliminate floating-point drift and 1-cent rounding discrepancies on non-divisible amounts, we used `BigDecimal` and deterministically allocated remainder cents so individual shares always sum to the exact expense. For settlements, we implemented a greedy min-cash-flow algorithm ($O(N \log N)$) that aggregates net balances and directly pairs the largest debtors with the largest creditors. **The trade-off accepted** is that global debt minimization abstracts away direct itemized "who paid for whom" relationships in exchange for minimizing total bank transfers and eliminating circular debts.

---

## 🛠️ Tech Stack & Highlights

- **Language**: Java 21 / 17 LTS
- **Framework**: Spring Boot 3.4.3 (Spring Web, Spring Data JPA, Spring Validation)
- **Database**: In-Memory H2 Database (zero setup, isolated per run)
- **Financial Calculations**: Strict `BigDecimal` (scale 2, `RoundingMode.HALF_UP` / `RoundingMode.DOWN` for remainder distribution)
- **Testing & Quality**: JUnit 5, AssertJ, Mockito, MockMvc, JaCoCo (>90% test coverage)
- **Containerization**: Multi-stage Docker build on Alpine Linux running on port `4110`

---

## 🚀 Getting Started

### Prerequisites

- **Java 17 or Java 21**
- **Maven 3.9+** (or use included `./mvnw` / `mvnw.cmd`)
- **Docker** (optional, for containerized execution)

---

### Option 1: Run Locally with Maven

From the project root:

```bash
# Navigate to the splitbill module
cd splitbill

# Run unit and integration tests with coverage verification
./mvnw clean verify

# Start the application on port 4110
./mvnw spring-boot:run
```

*(On Windows PowerShell: `.\mvnw.cmd spring-boot:run`)*

The server will start at `http://localhost:4110`.

---

### Option 2: Run with Docker

Build and run using the multi-stage Dockerfile from the repository root:

```bash
# Build the Docker image
docker build -t splitbill-app .

# Run container on port 4110
docker run -p 4110:4110 splitbill-app
```

---

## 📡 API Endpoints & Example `curl` Commands

### 1. Create a Bill Group

Creates a bill group with a name, optional description, and an initial list of participants (minimum 2).

**Request**:
```bash
curl -X POST http://localhost:4110/api/v1/groups \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Trip to Bali 2026",
    "description": "Weekend beach vacation with friends",
    "participants": ["Alice", "Bob", "Charlie"]
  }'
```

**Response (201 Created)**:
```json
{
  "success": true,
  "message": "Group created successfully",
  "data": {
    "id": "e458e38d-862d-419b-a0ee-fc0bcf3fbf2a",
    "name": "Trip to Bali 2026",
    "description": "Weekend beach vacation with friends",
    "participants": [
      {
        "id": "1c7a2e0a-2009-4081-9b16-56ffad7d52b1",
        "name": "Alice"
      },
      {
        "id": "bfd609db-da47-49cb-82fe-a42e5d16da0c",
        "name": "Bob"
      },
      {
        "id": "7699ca19-33ad-4eeb-b631-97b77ce468ea",
        "name": "Charlie"
      }
    ],
    "createdAt": "2026-08-15T10:15:30.123Z"
  },
  "timestamp": "2026-08-15T10:15:30.123Z"
}
```

---

### 2. Retrieve Group Details

**Request**:
```bash
curl -X GET http://localhost:4110/api/v1/groups/e458e38d-862d-419b-a0ee-fc0bcf3fbf2a
```

---

### 3. Add an Expense to a Group

Supports 3 split strategies:
- `EQUAL` (default): Splits amount equally across all group members (or specified `participantIds`), cleanly allocating remainder cents.
- `EXACT`: Each member has a specific designated monetary amount.
- `PERCENTAGE`: Each member has a specified percentage (summing to 100%).

#### Example A: Equal Split
Alice pays $\$120.00$ for all 3 participants ($\$40.00$ each).

**Request**:
```bash
curl -X POST http://localhost:4110/api/v1/groups/e458e38d-862d-419b-a0ee-fc0bcf3fbf2a/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Seafood Dinner",
    "amount": 120.00,
    "payerId": "1c7a2e0a-2009-4081-9b16-56ffad7d52b1",
    "splitType": "EQUAL"
  }'
```

**Response (201 Created)**:
```json
{
  "success": true,
  "message": "Expense added successfully",
  "data": {
    "id": "4a719c23-f365-4d2b-b6d4-8393fa8e29a1",
    "description": "Seafood Dinner",
    "amount": 120.00,
    "payer": {
      "id": "1c7a2e0a-2009-4081-9b16-56ffad7d52b1",
      "name": "Alice"
    },
    "splitType": "EQUAL",
    "splits": [
      {
        "participantId": "1c7a2e0a-2009-4081-9b16-56ffad7d52b1",
        "participantName": "Alice",
        "amount": 40.00,
        "percentage": null
      },
      {
        "participantId": "bfd609db-da47-49cb-82fe-a42e5d16da0c",
        "participantName": "Bob",
        "amount": 40.00,
        "percentage": null
      },
      {
        "participantId": "7699ca19-33ad-4eeb-b631-97b77ce468ea",
        "participantName": "Charlie",
        "amount": 40.00,
        "percentage": null
      }
    ],
    "createdAt": "2026-08-15T10:18:00.000Z"
  },
  "timestamp": "2026-08-15T10:18:00.000Z"
}
```

#### Example B: Exact Split
Bob pays $\$60.00$ for Charlie's scuba diving ticket.

**Request**:
```bash
curl -X POST http://localhost:4110/api/v1/groups/e458e38d-862d-419b-a0ee-fc0bcf3fbf2a/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Scuba Diving Ticket for Charlie",
    "amount": 60.00,
    "payerId": "bfd609db-da47-49cb-82fe-a42e5d16da0c",
    "splitType": "EXACT",
    "splits": [
      {
        "participantId": "7699ca19-33ad-4eeb-b631-97b77ce468ea",
        "amount": 60.00
      }
    ]
  }'
```

#### Example C: Percentage Split
Charlie pays $\$100.00$ for hotel room upgrade (50% Alice, 50% Bob).

**Request**:
```bash
curl -X POST http://localhost:4110/api/v1/groups/e458e38d-862d-419b-a0ee-fc0bcf3fbf2a/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Hotel Room Upgrade",
    "amount": 100.00,
    "payerId": "7699ca19-33ad-4eeb-b631-97b77ce468ea",
    "splitType": "PERCENTAGE",
    "splits": [
      {
        "participantId": "1c7a2e0a-2009-4081-9b16-56ffad7d52b1",
        "percentage": 50.00
      },
      {
        "participantId": "bfd609db-da47-49cb-82fe-a42e5d16da0c",
        "percentage": 50.00
      }
    ]
  }'
```

---

### 4. List All Expenses for a Group

**Request**:
```bash
curl -X GET http://localhost:4110/api/v1/groups/e458e38d-862d-419b-a0ee-fc0bcf3fbf2a/expenses
```

---

### 5. Retrieve Settlement Summary (Who Owes Whom)

Calculates the complete breakdown: total group spend, each participant's balance ($\text{Paid} - \text{Owed}$), the simplified debt transfers, and the personalized service charge fields.

**Request**:
```bash
curl -X GET http://localhost:4110/api/v1/groups/e458e38d-862d-419b-a0ee-fc0bcf3fbf2a/settlements
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "groupId": "e458e38d-862d-419b-a0ee-fc0bcf3fbf2a",
    "groupName": "Trip to Bali 2026",
    "totalExpenses": 280.00,
    "service_charge_pct": 0,
    "service_charge_amount": 0.00,
    "participantBalances": [
      {
        "participantId": "1c7a2e0a-2009-4081-9b16-56ffad7d52b1",
        "participantName": "Alice",
        "totalPaid": 120.00,
        "totalShare": 90.00,
        "netBalance": 30.00
      },
      {
        "participantId": "bfd609db-da47-49cb-82fe-a42e5d16da0c",
        "participantName": "Bob",
        "totalPaid": 60.00,
        "totalShare": 90.00,
        "netBalance": -30.00
      },
      {
        "participantId": "7699ca19-33ad-4eeb-b631-97b77ce468ea",
        "participantName": "Charlie",
        "totalPaid": 100.00,
        "totalShare": 100.00,
        "netBalance": 0.00
      }
    ],
    "settlements": [
      {
        "fromParticipantId": "bfd609db-da47-49cb-82fe-a42e5d16da0c",
        "fromParticipantName": "Bob",
        "toParticipantId": "1c7a2e0a-2009-4081-9b16-56ffad7d52b1",
        "toParticipantName": "Alice",
        "amount": 30.00
      }
    ]
  },
  "timestamp": "2026-08-15T10:20:00.000Z"
}
```

---

## 🧪 Testing and Coverage

Run the comprehensive test suite and verify JaCoCo 80%+ threshold:

```bash
cd splitbill
./mvnw clean verify
```

Generated coverage report can be inspected at:
`splitbill/target/site/jacoco/index.html`
