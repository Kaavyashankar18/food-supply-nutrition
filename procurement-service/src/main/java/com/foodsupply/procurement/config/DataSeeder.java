package com.foodsupply.procurement.config;

import com.foodsupply.procurement.entity.FoodUnit;
import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.PurchaseOrder;
import com.foodsupply.procurement.entity.PurchaseOrderItem;
import com.foodsupply.procurement.entity.PurchaseOrderStatus;
import com.foodsupply.procurement.entity.RequestItem;
import com.foodsupply.procurement.entity.RequestSource;
import com.foodsupply.procurement.entity.RequestStatus;
import com.foodsupply.procurement.entity.Supplier;
import com.foodsupply.procurement.entity.SupplierStatus;
import com.foodsupply.procurement.repository.ProcurementRequestRepository;
import com.foodsupply.procurement.repository.PurchaseOrderRepository;
import com.foodsupply.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds suppliers and a few requests / purchase orders in different statuses. It does NOT call other services and
 * deliberately leaves the Toor Dal / Egg / Spinach / Groundnut Oil shortages unprocured for the live demo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final SupplierRepository supplierRepository;
    private final ProcurementRequestRepository requestRepository;
    private final PurchaseOrderRepository orderRepository;

    private record Line(long foodItemId, String name, FoodUnit unit, String qty, String price) {
    }

    @Override
    public void run(String... args) {
        if (supplierRepository.count() > 0 || requestRepository.count() > 0 || orderRepository.count() > 0) {
            log.info("Procurement data already present - seeding skipped");
            return;
        }
        Random random = new Random(42);
        int year = LocalDate.now().getYear();

        List<Supplier> suppliers = new ArrayList<>();
        suppliers.add(supplier("Chennai Agro Foods Pvt Ltd", "Ramesh Kumar", "+91 98400 11201",
                "sales@chennaiagro.example.in", "45, Mint Street, Sowcarpet, Chennai 600079", SupplierStatus.ACTIVE, random));
        suppliers.add(supplier("Sri Murugan Pulses & Traders", "Murugan Selvam", "+91 98410 22302",
                "orders@srimurugan.example.in", "12, Koyambedu Wholesale Market, Chennai 600092", SupplierStatus.ACTIVE, random));
        suppliers.add(supplier("Southern Dairy Cooperative", "Lakshmi Narayanan", "+91 94440 33403",
                "supply@southerndairy.example.in", "Ambattur Industrial Estate, Chennai 600058", SupplierStatus.ACTIVE, random));
        suppliers.add(supplier("Tamil Nadu Fresh Produce Collective", "Karthik Rajan", "+91 99620 44504",
                "fresh@tnproduce.example.in", "Block C, Koyambedu Fruit and Vegetable Market, Chennai 600107", SupplierStatus.ACTIVE, random));
        suppliers.add(supplier("Annapoorna Oils & Edibles", "Sundaram Iyer", "+91 98940 55605",
                "care@annapoornaoils.example.in", "88, Poonamallee High Road, Chennai 600084", SupplierStatus.ACTIVE, random));
        suppliers.add(supplier("Madurai Spice House", "Meenakshi Sundari", "+91 97900 66706",
                "info@maduraispice.example.in", "21, West Masi Street, Madurai 625001", SupplierStatus.INACTIVE, random));
        suppliers = supplierRepository.saveAll(suppliers);
        Supplier agro = suppliers.get(0);
        Supplier dairy = suppliers.get(2);
        Supplier produce = suppliers.get(3);

        // PR-0001: completed (fully received)
        ProcurementRequest r1 = requestRepository.save(request(year, 1, RequestStatus.COMPLETED,
                "Quarterly staple replenishment", "store.manager", 30, -20, null,
                List.of(new Line(1, "Rice (Raw)", FoodUnit.KG, "500", "42.00"),
                        new Line(2, "Wheat Flour (Atta)", FoodUnit.KG, "300", "38.00"))));
        orderRepository.save(order(year, 1, agro, r1, 28, -21, PurchaseOrderStatus.RECEIVED, "1"));

        // PR-0002: ordered, PO sent, nothing received yet
        ProcurementRequest r2 = requestRepository.save(request(year, 2, RequestStatus.ORDERED,
                "Weekly fresh vegetables", "store.manager", 2, 2, null,
                List.of(new Line(10, "Potato", FoodUnit.KG, "200", "28.00"),
                        new Line(11, "Onion", FoodUnit.KG, "100", "32.00"),
                        new Line(12, "Tomato", FoodUnit.KG, "80", "30.00"))));
        orderRepository.save(order(year, 2, produce, r2, 1, 2, PurchaseOrderStatus.SENT, "0"));

        // PR-0003: ordered, PO partially received
        ProcurementRequest r3 = requestRepository.save(request(year, 3, RequestStatus.ORDERED,
                "Milk for breakfast programme", "procurement.officer", 6, 1, null,
                List.of(new Line(21, "Milk (Toned)", FoodUnit.LITRE, "600", "54.00"))));
        orderRepository.save(order(year, 3, dairy, r3, 4, 1, PurchaseOrderStatus.PARTIALLY_RECEIVED, "0.5"));

        // PR-0004: approved, waiting for a PO
        requestRepository.save(request(year, 4, RequestStatus.APPROVED,
                "Sugar and salt restock", "procurement.officer", 3, 10, "Approved within monthly budget",
                List.of(new Line(29, "Sugar", FoodUnit.KG, "100", "44.00"),
                        new Line(31, "Iodised Salt", FoodUnit.KG, "50", "22.00"))));

        // PR-0005: submitted, pending approval
        requestRepository.save(request(year, 5, RequestStatus.SUBMITTED,
                "Sunflower oil for community kitchens", "store.manager", 1, 7, null,
                List.of(new Line(27, "Sunflower Oil", FoodUnit.LITRE, "200", "128.00"))));

        // PR-0006: draft
        requestRepository.save(request(year, 6, RequestStatus.DRAFT,
                "Bananas for mid-day meal", "field.officer", 0, 5, null,
                List.of(new Line(17, "Banana", FoodUnit.PIECE, "2000", "6.00"))));

        // PR-0007: rejected
        requestRepository.save(request(year, 7, RequestStatus.REJECTED,
                "Exotic fruit purchase", "field.officer", 12, 14, "Not within the programme budget",
                List.of(new Line(18, "Apple", FoodUnit.PIECE, "500", "14.00"))));

        log.info("Seeded procurement-service: {} suppliers, {} requests, {} purchase orders",
                supplierRepository.count(), requestRepository.count(), orderRepository.count());
    }

    private Supplier supplier(String name, String contact, String phone, String email, String address,
                              SupplierStatus status, Random random) {
        return Supplier.builder()
                .name(name).contactPerson(contact).phone(phone).email(email).address(address)
                .status(status)
                .rating(BigDecimal.valueOf(35 + random.nextInt(14), 1))
                .build();
    }

    private ProcurementRequest request(int year, int seq, RequestStatus status, String reason, String requestedBy,
                                       int createdDaysAgo, int needByOffsetDays, String remarks, List<Line> lines) {
        ProcurementRequest r = ProcurementRequest.builder()
                .requestNumber(String.format("PR-%d-%04d", year, seq))
                .status(status)
                .source(RequestSource.MANUAL)
                .reason(reason)
                .requestedBy(requestedBy)
                .createdOn(LocalDate.now().minusDays(createdDaysAgo))
                .needByDate(LocalDate.now().plusDays(needByOffsetDays))
                .decisionRemarks(remarks)
                .build();
        BigDecimal total = BigDecimal.ZERO;
        for (Line l : lines) {
            BigDecimal qty = new BigDecimal(l.qty());
            BigDecimal price = new BigDecimal(l.price());
            r.addItem(RequestItem.builder()
                    .foodItemId(l.foodItemId())
                    .foodItemName(l.name())
                    .unit(l.unit())
                    .quantity(qty)
                    .estimatedUnitPrice(price)
                    .build());
            total = total.add(qty.multiply(price));
        }
        r.setEstimatedCost(total.setScale(2, RoundingMode.HALF_UP));
        return r;
    }

    private PurchaseOrder order(int year, int seq, Supplier supplier, ProcurementRequest request,
                                int orderedDaysAgo, int expectedOffsetDays, PurchaseOrderStatus status,
                                String receivedFactor) {
        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(String.format("PO-%d-%04d", year, seq))
                .supplierId(supplier.getId())
                .supplierName(supplier.getName())
                .requestId(request.getId())
                .requestNumber(request.getRequestNumber())
                .orderDate(LocalDate.now().minusDays(orderedDaysAgo))
                .expectedDeliveryDate(LocalDate.now().plusDays(expectedOffsetDays))
                .status(status)
                .build();
        BigDecimal factor = new BigDecimal(receivedFactor);
        for (RequestItem ri : request.getItems()) {
            po.addItem(PurchaseOrderItem.builder()
                    .foodItemId(ri.getFoodItemId())
                    .foodItemName(ri.getFoodItemName())
                    .unit(ri.getUnit())
                    .orderedQty(ri.getQuantity())
                    .receivedQty(ri.getQuantity().multiply(factor).setScale(3, RoundingMode.HALF_UP))
                    .unitPrice(ri.getEstimatedUnitPrice())
                    .build());
        }
        return po;
    }
}
