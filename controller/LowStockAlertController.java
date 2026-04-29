package com.bynry.stockflow.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

record SupplierDTO(Long id, String name, String contactEmail) {}

record AlertDTO(Long productId, String productName, String sku, Long warehouseId, 
                String warehouseName, Integer currentStock, Integer threshold, 
                Integer daysUntilStockout, SupplierDTO supplier) {}
                
// API Shaped with pagination metadata for frontend scalability
record AlertResponse(List<AlertDTO> alerts, int totalAlerts, int currentPage, boolean hasNext) {}

@RestController
@RequestMapping("/api/companies")
public class LowStockAlertController {
    
    private static final Logger logger = LoggerFactory.getLogger(LowStockAlertController.class);
    private final AlertService alertService;

    public LowStockAlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/{companyId}/alerts/low-stock")
    public ResponseEntity<?> getLowStockAlerts(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            AlertResponse response = alertService.generateLowStockAlerts(companyId, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching alerts for companyId: {}", companyId, e);
            return ResponseEntity.internalServerError().body("An error occurred while processing low stock alerts.");
        }
    }
}
//ERROR BECAUSE POM FILE IS MISSING
