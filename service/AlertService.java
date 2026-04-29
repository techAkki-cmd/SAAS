package com.bynry.stockflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class AlertService {

    private final InventoryRepository inventoryRepo;

    public AlertService(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    @Transactional(readOnly = true)
    public AlertResponse generateLowStockAlerts(Long companyId, int page, int size) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // Pagination applied to prevent OutOfMemory errors on large B2B datasets
        var pageable = PageRequest.of(page, size);
        
        // Assumes repository method uses @EntityGraph or JOIN FETCH to prevent N+1 queries
        var inventoryPage = inventoryRepo.findByCompanyIdWithDetails(companyId, pageable);
        var alerts = new ArrayList<AlertDTO>();

        for (var inv : inventoryPage.getContent()) {
            // Note: In a highly scaled system, this loop query should be batched 
            // or pulled from a materialized view/Redis cache to avoid DB hammering.
            int recentSalesVolume = inventoryRepo.sumSalesVolumeSince(inv.getId(), thirtyDaysAgo);
            
            if (recentSalesVolume == 0) continue;

            int threshold = getThresholdForType(inv.getProduct().getType());

            if (inv.getQuantity() <= threshold) {
                double dailyVelocity = recentSalesVolume / 30.0;
                
                // Fallback to 999 to prevent divide-by-zero if velocity is functionally 0
                int daysUntilStockout = dailyVelocity > 0 
                    ? (int) (inv.getQuantity() / dailyVelocity) 
                    : 999; 

                // Failure Scenario Defense: Handle unassigned or deleted suppliers gracefully
                SupplierDTO supplierDto = null;
                if (inv.getProduct().getSupplier() != null) {
                    supplierDto = new SupplierDTO(
                        inv.getProduct().getSupplier().getId(),
                        inv.getProduct().getSupplier().getName(),
                        inv.getProduct().getSupplier().getContactEmail()
                    );
                }

                // Failure Scenario Defense: Handle missing warehouse references
                String warehouseName = inv.getWarehouse() != null ? inv.getWarehouse().getName() : "Unknown Location";
                Long warehouseId = inv.getWarehouse() != null ? inv.getWarehouse().getId() : null;

                alerts.add(new AlertDTO(
                    inv.getProduct().getId(),
                    inv.getProduct().getName(),
                    inv.getProduct().getSku(),
                    warehouseId,
                    warehouseName,
                    inv.getQuantity(),
                    threshold,
                    daysUntilStockout,
                    supplierDto
                ));
            }
        }

        return new AlertResponse(alerts, alerts.size(), page, inventoryPage.hasNext());
    }

    private int getThresholdForType(String productType) {
        return switch (productType.toLowerCase()) {
            case "fast_moving" -> 100;
            case "bundle" -> 5;
            default -> 20;
        };
    }
}
//ERROR BECAUSE POM FILE IS MISSING
