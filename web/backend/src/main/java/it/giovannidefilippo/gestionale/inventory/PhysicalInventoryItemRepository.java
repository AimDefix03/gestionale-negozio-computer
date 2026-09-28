package it.giovannidefilippo.gestionale.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

interface PhysicalInventoryItemRepository extends JpaRepository<PhysicalInventoryItem, Long> {
    boolean existsByProductIdAndActiveMarkerTrue(Long productId);
}
