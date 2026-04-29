# StockFlow - Bynry Backend Case Study

A business-to-business cloud-based platform for managing inventories. The application is an illustration of proper transactions, normalization of database structure, and a well-designed API system to manage items and inventory levels in different warehouse locations.

##  Overview
StockFlow is designed to handle high-concurrency inventory tracking with a strict focus on data integrity, auditability, and performance. 

This repository contains the solutions for the three core phases of the Bynry technical assessment:
1. **Code Review & Debugging** (Python/Flask)
2. **Database Architecture** (PostgreSQL)
3. **API Implementation** (Java/Spring Boot)

## 🛠 Tech Stack
* **Language:** Java 17+ (utilizing Records for immutable DTOs), Python 3.x (Debugging phase)
* **Framework:** Spring Boot (REST APIs), Flask
* **Database:** PostgreSQL (Relational schema with append-only ledger)
* **Data Access:** Spring Data JPA / Hibernate

## Architecture & Key Decisions

### 1. The Append-Only Ledger
Instead of mutating a static quantity column, inventory changes are tracked via an append-only `inventory_ledger`. This provides a mathematically provable B2B audit trail, prevents race conditions during concurrent updates, and allows for accurate historical sales velocity calculations.

### 2. Transactional Integrity & Idempotency
* Enforced strict transaction boundaries (`@Transactional`) to prevent orphaned records.
* Handled database-level composite unique constraints `(product_id, warehouse_id)` to catch race conditions and return HTTP 409 Conflicts, acting as a graceful idempotency safeguard for network retries.
* Financial data (prices) are strictly parsed as `Decimal` to eliminate floating-point arithmetic errors.

### 3. Read Optimization & Scalability
* Addressed the N+1 query problem by utilizing `@EntityGraph` (or `JOIN FETCH`) to batch-load nested Supplier and Warehouse entities.
* The `/alerts/low-stock` endpoint is fully paginated to prevent memory exhaustion when querying large B2B catalogs.
* Read-heavy services are annotated with `@Transactional(readOnly = true)` to bypass Hibernate's dirty-checking overhead.

##  Project │   └── product_controller.py      # Part 1: Refactored Flask endpoint
├── database/
│   └── schema.sql                 # Part 2: PostgreSQL DDL & Indexing
└── src/main/java/com/bynry/stockflow/
    ├── api/
    │   └── LowStockAlertController.java # Part 3: REST API
    ├── service/
    │   └── AlertService.java            # Part 3: Business Logic
    └── model/                           # Entity definitions (assumed)
    
## 🧠 Documented Assumptions & Edge Cases Handled

* **Stockout Projection:** The V1 model uses a simple linear extrapolation (current stock / 30-day average daily velocity) to calculate `days_until_stockout`. Division-by-zero is handled natively via ternary fallback.
* **Failure Defense:** The system gracefully handles missing entity relationships (e.g., deleted warehouses or missing suppliers) by assigning nulls/defaults rather than throwing `NullPointerExceptions` that would crash batch processes.
* **M:N Supplier Mapping:** Bypassed the simplistic 1:N assumption to implement a robust M:N `product_suppliers` association, accounting for real-world scenarios where a single SKU is procured from multiple vendors.

## ⚙️ Setup & Execution
*(Note: As this is a conceptual case study, the code represents core logical blocks rather than a fully wired, executable monolithic application.)*

1. Review `schema.sql` for the relational design and indexing strategy.
2. Review the Spring Boot controller and service layer for the primary business logic.
3. Review `product_controller.py` for Python transaction debugging.
