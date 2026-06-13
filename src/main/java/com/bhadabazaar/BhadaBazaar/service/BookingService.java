package com.bhadabazaar.BhadaBazaar.service;

import com.bhadabazaar.BhadaBazaar.domain.entity.Booking;
import com.bhadabazaar.BhadaBazaar.domain.entity.BookingItem;
import com.bhadabazaar.BhadaBazaar.domain.entity.Item;
import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.BookingStatus;
import com.bhadabazaar.BhadaBazaar.dto.BookingCreateRequest;
import com.bhadabazaar.BhadaBazaar.dto.BookingResponse;
import com.bhadabazaar.BhadaBazaar.dto.ItemImageResponse;
import com.bhadabazaar.BhadaBazaar.dto.ItemResponse;
import com.bhadabazaar.BhadaBazaar.repository.BookingRepository;
import com.bhadabazaar.BhadaBazaar.repository.ItemRepository;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VendorRepository vendorRepository;
    private final ItemRepository itemRepository;
    private final WhatsAppService whatsAppService;

    @Transactional
    public BookingResponse createBooking(String username, BookingCreateRequest request) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        LocalDate fromDate = request.fromDate();
        LocalDate toDate = request.toDate();
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Booking dates are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }
        LocalDate storedToDate = toDate.plusDays(1);

        if (request.itemIds() == null || request.itemIds().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }

        // Lock the item rows FOR UPDATE so concurrent bookings for the same item serialize;
        // this makes the availability check below atomic with the insert.
        List<Item> items = itemRepository.findAllByIdForUpdate(request.itemIds());
        if (items.size() != request.itemIds().size()) {
            throw new RuntimeException("Some items not found");
        }
        // Every booked item must belong to the calling vendor
        boolean allOwned = items.stream()
                .allMatch(item -> item.getVendor().getId().equals(vendor.getId()));
        if (!allOwned) {
            throw new RuntimeException("Some items not found");
        }

        // With the item rows locked, no concurrent booking can slip in between this check and the
        // insert: a competing transaction blocks on the lock until we commit, then sees our booking.
        List<Long> conflicts = bookingRepository.findConflictingItemIds(
                request.itemIds(),
                List.of(BookingStatus.BOOKED, BookingStatus.RETURN_PENDING),
                fromDate,
                toDate);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                    "One or more items are already booked for the selected dates");
        }

        // Server computes the authoritative total from item prices, never trusting the client.
        long days = ChronoUnit.DAYS.between(fromDate, toDate);
        BigDecimal calculatedTotal = items.stream()
                .map(Item::getPricePerDay)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(days));

        BigDecimal discount = request.discount() != null ? request.discount() : BigDecimal.ZERO;
        BigDecimal advancePaid = request.advancePaid() != null ? request.advancePaid() : BigDecimal.ZERO;

        if (discount.signum() < 0 || advancePaid.signum() < 0) {
            throw new IllegalArgumentException("Discount and advance paid must not be negative");
        }
        if (discount.compareTo(calculatedTotal) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed total amount");
        }
        if (advancePaid.compareTo(calculatedTotal.subtract(discount)) > 0) {
            throw new IllegalArgumentException("Advance paid cannot exceed amount due");
        }

        BigDecimal remaining = calculatedTotal.subtract(discount).subtract(advancePaid);

        Booking newBooking = Booking.builder()
                .vendor(vendor)
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .fromDate(fromDate)
                .toDate(storedToDate)
                .totalPrice(calculatedTotal)
                .discount(discount)
                .advancePaid(advancePaid)
                .remainingAmount(remaining)
                .status(BookingStatus.BOOKED)
                .build();

        List<BookingItem> bookingItems = items.stream()
                .map(item -> BookingItem.builder()
                        .booking(newBooking)
                        .item(item)
                        .build())
                .collect(Collectors.toList());
        
        newBooking.setBookingItems(bookingItems);
        
        Booking savedBooking = bookingRepository.save(newBooking);

        // Atomic increment so two concurrent bookings for this vendor can't lose an advance update.
        vendorRepository.addEarnings(vendor.getId(), advancePaid);

        String itemNames = items.stream()
                .map(Item::getName)
                .collect(Collectors.joining(", "));
        String message = String.format(
                "Hello %s, your booking (ID: %d) at %s is confirmed from %s to %s.\nItems: %s\nRemaining Amount: %s and Advance Given: %s",
                request.customerName(), savedBooking.getId(), vendor.getShopName(), fromDate, toDate, itemNames, remaining, advancePaid);
        whatsAppService.sendBookingNotification(request.customerPhone(), message);
        
        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getVendorBookings(String username, BookingStatus status, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return bookingRepository.findVendorBookings(vendor.getId(), status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> searchBookingsByPhone(String username, String phone) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return bookingRepository
        .findByVendorIdAndStatusAndCustomerPhoneContainingIgnoreCase(vendor.getId(), BookingStatus.BOOKED, phone)
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> searchBookingsByDate(String username, LocalDate date, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return bookingRepository.findByVendorIdAndStatusAndFromDate(vendor.getId(), BookingStatus.BOOKED, date, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> searchReturnPendingBeforeDate(String username, LocalDate date, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        LocalDate deadlineWithBuffer = date.plusDays(1);
        return bookingRepository.findReturnPendingBeforeDate(vendor.getId(), deadlineWithBuffer, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void updateBookingStatus(String username, Long bookingId, BookingStatus status) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        Booking booking = bookingRepository.findByIdAndVendorId(bookingId, vendor.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (status == BookingStatus.CLOSED && booking.getStatus() != BookingStatus.CLOSED) {
            // Atomic increment; @Version on Booking guards against two concurrent closes both
            // reaching here — the loser's flush fails with an optimistic-lock error and this whole
            // transaction (including this increment) rolls back, so remainingAmount is added once.
            vendorRepository.addEarnings(vendor.getId(), booking.getRemainingAmount());
        }

        booking.setStatus(status);
        bookingRepository.save(booking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        LocalDate displayToDate = booking.getToDate().minusDays(1);
        return new BookingResponse(
                booking.getId(),
                booking.getCustomerName(),
                booking.getCustomerPhone(),
                booking.getFromDate(),
                displayToDate,
                booking.getTotalPrice(),
                booking.getDiscount(),
                booking.getAdvancePaid(),
                booking.getRemainingAmount(),
                booking.getStatus(),
                booking.getBookingItems().stream()
                        .map(bi -> mapItemToResponse(bi.getItem()))
                        .collect(Collectors.toList())
        );
    }
    
    private ItemResponse mapItemToResponse(Item item) {
         return new ItemResponse(
                item.getId(),
                item.getVendor().getId(),
                item.getItemCode(),
                item.getName(),
                item.getCategory(),
                item.getGender(),
                item.getPricePerDay(),
                item.getIsActive(),
                item.getImages().stream().map(img -> new ItemImageResponse(
                        img.getId(),
                        img.getImageUrl(),
                        img.getImageType()
                )).collect(Collectors.toList())
        );
    }
}
