from flask import request, jsonify
from sqlalchemy.exc import IntegrityError
from decimal import Decimal, InvalidOperation
import logging

@app.route('/api/products', methods=['POST'])
def create_product():
    data = request.json
    
    required_keys = ['name', 'sku', 'price', 'warehouse_id', 'initial_quantity']
    if not data or not all(k in data for k in required_keys):
        return jsonify({"error": "Missing required fields"}), 400
        
    try:
        # Note: Deliberately excluding warehouse_id from Product creation.
        # The M:N relationship is handled purely via the Inventory bridge.
        product = Product(
            name=data['name'],
            sku=data['sku'],
            # Cast to string first before Decimal to prevent float precision loss from JSON
            price=Decimal(str(data['price'])), 
            description=data.get('description')
        )
        
        db.session.add(product)
        db.session.flush() 
        
        inventory = Inventory(
            product_id=product.id,
            warehouse_id=data['warehouse_id'],
            quantity=int(data['initial_quantity'])
        )
        
        db.session.add(inventory)
        db.session.commit()
        
        return jsonify({
            "message": "Product created successfully", 
            "product_id": product.id
        }), 201

    except IntegrityError:
        db.session.rollback()
        # Log the specific SKU attempt for easier debugging in production
        logging.warning(f"Integrity violation on product creation (SKU: {data.get('sku')})")
        return jsonify({"error": "SKU already exists or invalid warehouse ID"}), 409
        
    except (ValueError, InvalidOperation):
        db.session.rollback()
        return jsonify({"error": "Invalid data format for numeric fields"}), 400
        
    except Exception as e:
        db.session.rollback()
        logging.exception("Unexpected error during product creation")
        return jsonify({"error": "Internal server error"}), 500