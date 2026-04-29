package com.bynry.stockflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class AlertService {

    private final InventoryRepository inventoryRepo;

    public AlertService(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    @Transactional(readOnly = true)
    public AlertResponse generateLowStockAlerts(Long companyId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        var inventoryRecords = inventoryRepo.findByCompanyIdWithDetails(companyId);
        var alerts = new ArrayList<AlertDTO>();

        for (var inv : inventoryRecords) {
            int recentSalesVolume = inventoryRepo.sumSalesVolumeSince(inv.getId(), thirtyDaysAgo);
            
            if (recentSalesVolume == 0) continue;

            int threshold = getThresholdForType(inv.getProduct().getType());

            if (inv.getQuantity() <= threshold) {
                double dailyVelocity = recentSalesVolume / 30.0;
                
                // Fallback to 999 to prevent divide-by-zero if velocity is functionally 0
                int daysUntilStockout = dailyVelocity > 0 
                    ? (int) (inv.getQuantity() / dailyVelocity) 
                    : 999; 

                var supplierDto = new SupplierDTO(
                    inv.getProduct().getSupplier().getId(),
                    inv.getProduct().getSupplier().getName(),
                    inv.getProduct().getSupplier().getContactEmail()
                );

                alerts.add(new AlertDTO(
                    inv.getProduct().getId(),
                    inv.getProduct().getName(),
                    inv.getProduct().getSku(),
                    inv.getWarehouse().getId(),
                    inv.getWarehouse().getName(),
                    inv.getQuantity(),
                    threshold,
                    daysUntilStockout,
                    supplierDto
                ));
            }
        }

        return new AlertResponse(alerts, alerts.size());
    }

    private int getThresholdForType(String productType) {
        return switch (productType.toLowerCase()) {
            case "fast_moving" -> 100;
            case "bundle" -> 5;
            default -> 20;
        };
    }
}