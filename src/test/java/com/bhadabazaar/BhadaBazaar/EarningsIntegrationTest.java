// package com.bhadabazaar.BhadaBazaar;

// import com.bhadabazaar.BhadaBazaar.domain.entity.Item;
// import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
// import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
// import com.bhadabazaar.BhadaBazaar.domain.enums.GenderType;
// import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
// import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
// import com.bhadabazaar.BhadaBazaar.domain.enums.VendorStatus;
// import com.bhadabazaar.BhadaBazaar.dto.BookingCreateRequest;
// import com.bhadabazaar.BhadaBazaar.dto.BookingResponse;
// import com.bhadabazaar.BhadaBazaar.repository.BookingRepository;
// import com.bhadabazaar.BhadaBazaar.repository.ItemRepository;
// import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
// import com.bhadabazaar.BhadaBazaar.service.BookingService;
// import com.bhadabazaar.BhadaBazaar.service.VendorService;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.util.List;

// import static org.assertj.core.api.Assertions.assertThat;

// public class EarningsIntegrationTest extends AbstractIntegrationTest {

//     @Autowired
//     private VendorRepository vendorRepository;

//     @Autowired
//     private ItemRepository itemRepository;

//     @Autowired
//     private BookingService bookingService;

//     @Autowired
//     private VendorService vendorService;
    
//     @Autowired
//     private BookingRepository bookingRepository;

//     @Test
//     void testEarningsFlow() {
//         // 1. Setup Vendor and Item
//         Vendor vendor = Vendor.builder()
//                 .username("earnings_vendor")
//                 .passwordHash("hash")
//                 .vendorName("Earnings Vendor")
//                 .shopName("Earnings Shop")
//                 .city("Earnings City")
//                 .primaryPhone("9999999999")
//                 .genderServed(GenderType.MEN)
//                 .categories(new String[]{ItemCategory.SUITS.name()})
//                 .status(VendorStatus.APPROVED)
//                 .yearlyPrice(BigDecimal.TEN)
//                 .subscriptionStartDate(LocalDate.now())
//                 .earnings(BigDecimal.ZERO)
//                 .build();
//         vendorRepository.save(vendor);

//         Item item = Item.builder()
//                 .vendor(vendor)
//                 .itemCode("SUIT1")
//                 .name("Suit 1")
//                 .category(ItemCategory.SUITS)
//                 .gender(ItemGenderType.MEN)
//                 .pricePerDay(new BigDecimal("500.00"))
//                 .isActive(true)
//                 .isDeleted(false)
//                 .build();
//         itemRepository.save(item);

//         // 2. Create Booking
//         // Total: 1000, Discount: 0, Advance: 200, Remaining: 800
//         BookingCreateRequest request = new BookingCreateRequest(
//                 "Customer",
//                 "8888888888",
//                 LocalDate.now().plusDays(1),
//                 LocalDate.now().plusDays(3),
//                 new BigDecimal("1000.00"),
//                 BigDecimal.ZERO,
//                 new BigDecimal("200.00"),
//                 List.of(item.getId())
//         );

//         BookingResponse booking = bookingService.createBooking("earnings_vendor", request);

//         // 3. Verify Earnings after Booking (Advance only)
//         BigDecimal earningsAfterBooking = vendorService.getEarnings("earnings_vendor");
//         assertThat(earningsAfterBooking).isEqualByComparingTo("200.00");

//         // 4. Close Booking (Simulate Return and Payment of Remaining)
//         bookingService.updateBookingStatus(booking.id(), BookingStatus.CLOSED);

//         // 5. Verify Earnings after Close (Advance + Remaining)
//         BigDecimal earningsAfterClose = vendorService.getEarnings("earnings_vendor");
//         assertThat(earningsAfterClose).isEqualByComparingTo("1000.00"); // 200 + 800

//         // 6. Reset Earnings
//         vendorService.resetEarnings("earnings_vendor");
//         assertThat(vendorService.getEarnings("earnings_vendor")).isEqualByComparingTo("0.00");
//     }
// }
