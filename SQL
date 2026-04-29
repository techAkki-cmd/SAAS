
CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE warehouses (
    id SERIAL PRIMARY KEY,
    company_id INT REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255)
);

CREATE TABLE suppliers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    product_type VARCHAR(50) NOT NULL -- e.g., 'standard', 'bundle'
);

-- Deeply modeled M:N Supplier Relationship
CREATE TABLE product_suppliers (
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    supplier_id INT REFERENCES suppliers(id) ON DELETE CASCADE,
    lead_time_days INT, -- Added for real-world procurement logic
    PRIMARY KEY (product_id, supplier_id)
);

-- M:N Bill of Materials for Bundles
CREATE TABLE product_bundles (
    bundle_id INT REFERENCES products(id) ON DELETE CASCADE,
    component_id INT REFERENCES products(id) ON DELETE RESTRICT,
    component_quantity INT NOT NULL CHECK (component_quantity > 0),
    PRIMARY KEY (bundle_id, component_id)
);

CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    product_id INT REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id INT REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity INT NOT NULL DEFAULT 0,
    UNIQUE (product_id, warehouse_id)
);

-- Append-only ledger
CREATE TABLE inventory_ledger (
    id SERIAL PRIMARY KEY,
    inventory_id INT REFERENCES inventory(id) ON DELETE RESTRICT,
    change_amount INT NOT NULL, 
    transaction_type VARCHAR(50) NOT NULL, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexing Strategy Implementation
CREATE INDEX idx_inventory_product ON inventory(product_id);
-- Composite index specifically optimized for the Part 3 API query
CREATE INDEX idx_ledger_inventory_time ON inventory_ledger(inventory_id, created_at DESC);