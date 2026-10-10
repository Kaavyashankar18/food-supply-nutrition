package com.foodsupply.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.foodsupply.inventory.dto.QuantityRequest;
import com.foodsupply.inventory.dto.ReferenceRequest;
import com.foodsupply.inventory.dto.ReservationResultResponse;
import com.foodsupply.inventory.dto.ReserveRequest;
import com.foodsupply.inventory.entity.QualityStatus;
import com.foodsupply.inventory.entity.Reservation;
import com.foodsupply.inventory.entity.ReservationStatus;
import com.foodsupply.inventory.entity.StockBatch;
import com.foodsupply.inventory.exception.ShortageException;
import com.foodsupply.inventory.repository.ReservationRepository;
import com.foodsupply.inventory.repository.StockBatchRepository;
import com.foodsupply.inventory.repository.StockItemRepository;
import com.foodsupply.inventory.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockItemRepository itemRepository;

    @Mock
    private StockBatchRepository batchRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private StockServiceImpl service;

    private StockBatch batch(long id, String number, double qty, int expiryInDays) {
        return StockBatch.builder().id(id).foodItemId(1L).batchNumber(number)
                .quantityReceived(BigDecimal.valueOf(qty)).quantityAvailable(BigDecimal.valueOf(qty))
                .quantityReserved(BigDecimal.ZERO).receivedDate(LocalDate.now().minusDays(10))
                .expiryDate(LocalDate.now().plusDays(expiryInDays)).qualityStatus(QualityStatus.APPROVED).build();
    }

    private ReserveRequest reserveRequest(double qty) {
        return new ReserveRequest("distribution", 7L,
                List.of(new QuantityRequest(1L, BigDecimal.valueOf(qty))));
    }

    @Test
    void reserveAllocatesEarliestExpiryFirst() {
        StockBatch late = batch(1L, "LATE", 100, 20);
        StockBatch early = batch(2L, "EARLY", 10, 5);
        when(batchRepository.findAllocatable(eq(1L), eq(QualityStatus.APPROVED), any(LocalDate.class)))
                .thenReturn(List.of(late, early));

        ReservationResultResponse res = service.reserve(reserveRequest(30));

        assertEquals("DISTRIBUTION", res.referenceType());
        assertEquals(2, res.allocations().size());
        assertEquals("EARLY", res.allocations().get(0).batchNumber());
        assertEquals(0, new BigDecimal("10").compareTo(res.allocations().get(0).quantity()));
        assertEquals("LATE", res.allocations().get(1).batchNumber());
        assertEquals(0, new BigDecimal("20").compareTo(res.allocations().get(1).quantity()));
        assertEquals(0, new BigDecimal("20").compareTo(late.getQuantityReserved()));
    }

    @Test
    void reserveIsAllOrNothingWhenShort() {
        StockBatch only = batch(1L, "ONLY", 10, 5);
        when(batchRepository.findAllocatable(eq(1L), eq(QualityStatus.APPROVED), any(LocalDate.class)))
                .thenReturn(List.of(only));

        ShortageException ex = assertThrows(ShortageException.class, () -> service.reserve(reserveRequest(25)));

        assertEquals(1, ex.getShortages().size());
        assertEquals(0, new BigDecimal("15").compareTo(ex.getShortages().get(0).shortage()));
        verify(reservationRepository, never()).save(any(Reservation.class));
        assertEquals(0, BigDecimal.ZERO.compareTo(only.getQuantityReserved()));
    }

    @Test
    void dispatchReducesOnHandAndReserved() {
        StockBatch b = batch(5L, "B5", 50, 30);
        b.setQuantityReserved(new BigDecimal("12"));
        Reservation r = Reservation.builder().id(1L).referenceType("DISTRIBUTION").referenceId(7L).batchId(5L)
                .foodItemId(1L).quantity(new BigDecimal("12")).status(ReservationStatus.ACTIVE).build();
        when(reservationRepository.findByReferenceTypeAndReferenceIdAndStatus("DISTRIBUTION", 7L,
                ReservationStatus.ACTIVE)).thenReturn(List.of(r));
        when(batchRepository.findAllByIdForUpdate(any())).thenReturn(List.of(b));

        ReservationResultResponse res = service.dispatch(new ReferenceRequest("distribution", 7L));

        assertEquals("DISPATCHED", res.action());
        assertEquals(0, new BigDecimal("38").compareTo(b.getQuantityAvailable()));
        assertEquals(0, BigDecimal.ZERO.compareTo(b.getQuantityReserved()));
        assertTrue(r.getStatus() == ReservationStatus.DISPATCHED);
    }
}
