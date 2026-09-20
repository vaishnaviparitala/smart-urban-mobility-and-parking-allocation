package com.smartcity.parking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByStatus(String status);

    List<ParkingSlot> findByVehicleType(String vehicleType);

    long countByStatus(String status);

    // Nearest slot to the entrance comes first
    List<ParkingSlot> findByStatusAndVehicleTypeOrderByDistanceFromEntranceAsc(String status, String vehicleType);

    /**
     * Atomic status change. The WHERE clause only matches if the slot is still in
     * the expected status, so two users can never reserve the same slot.
     * Returns 1 if the change happened, 0 if someone else got there first.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update ParkingSlot s set s.status = :toStatus where s.id = :id and s.status = :fromStatus")
    int changeStatus(@Param("id") Long id,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus);
}
