package com.bhadabazaar.BhadaBazaar;

import com.bhadabazaar.BhadaBazaar.domain.entity.Item;
import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import com.bhadabazaar.BhadaBazaar.domain.enums.GenderType;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
import com.bhadabazaar.BhadaBazaar.dto.BookingCreateRequest;
import com.bhadabazaar.BhadaBazaar.dto.BookingResponse;
import com.bhadabazaar.BhadaBazaar.dto.VendorDashboardStats;
import com.bhadabazaar.BhadaBazaar.repository.ItemRepository;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import com.bhadabazaar.BhadaBazaar.service.BookingService;
import com.bhadabazaar.BhadaBazaar.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VendorStatsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VendorService vendorService;

    @Test
    void testVendorStats() {
        // 1. Setup Vendor
        String username = "stats_vendor";
        Vendor vendor = Vendor.builder()
                .username(username)
                .passwordHash("hash")
                .vendorName("Stats Vendor")
                .shopName("Stats Shop")
                .city("Stats City")
                .primaryPhone("7777777777")
                .genderServed(GenderType.MEN)
                .categories(new String[]{ItemCategory.SUITS.name()})
                .status(VendorStatus.APPROVED)
                .yearlyPrice(BigDecimal.TEN)
                .subscriptionStartDate(LocalDate.now())
                .earnings(BigDecimal.ZERO)
                .build();
        vendorRepository.save(vendor);

        // 2. Setup Items (2 active, 1 deleted)
        Item item1 = createItem(vendor, "ITEM1", false);
        Item item2 = createItem(vendor, "ITEM2", false);
        Item item3 = createItem(vendor, "ITEM3", true); // deleted

        // 3. Create Bookings
        // Booking 1: BOOKED
        createBooking(username, item1);
        
        // Booking 2: RETURN_PENDING
        BookingResponse b2 = createBooking(username, item2);
        bookingService.updateBookingStatus(b2.id(), BookingStatus.RETURN_PENDING);

        // 4. Verify Stats
        // Bookings: 1 (BOOKED) - Note: b2 is now RETURN_PENDING, so not counted as BOOKED
        // Returns: 1 (RETURN_PENDING)
        // Items: 2 (Active/Non-deleted)
        
        VendorDashboardStats stats = vendorService.getVendorStats(username);
        
        assertThat(stats.bookingsCount()).isEqualTo(1);
        assertThat(stats.returnsCount()).isEqualTo(1);
        assertThat(stats.itemsCount()).isEqualTo(2);
    }

    private Item createItem(Vendor vendor, String code, boolean isDeleted) {
        Item item = Item.builder()
                .vendor(vendor)
                .itemCode(code)
                .name("Item " + code)
                .category(ItemCategory.SUITS)
                .gender(ItemGenderType.MEN)
                .pricePerDay(new BigDecimal("100.00"))
                .isActive(true)
                .isDeleted(isDeleted)
                .build();
        return itemRepository.save(item);
    }

    private BookingResponse createBooking(String username, Item item) {
        BookingCreateRequest request = new BookingCreateRequest(
                "Customer",
                "9999999999",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                new BigDecimal("300.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                List.of(item.getId())
        );
        return bookingService.createBooking(username, request);
    }
}
