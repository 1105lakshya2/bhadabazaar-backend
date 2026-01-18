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
        LocalDate storedToDate = toDate.plusDays(2);

        List<Item> items = itemRepository.findAllById(request.itemIds());
        if (items.size() != request.itemIds().size()) {
            throw new RuntimeException("Some items not found");
        }

        BigDecimal calculatedTotal = request.totalAmount();
        
        BigDecimal remaining = calculatedTotal.subtract(request.advancePaid()).subtract(request.discount());

        Booking newBooking = Booking.builder()
                .vendor(vendor)
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .fromDate(fromDate)
                .toDate(storedToDate)
                .totalPrice(calculatedTotal)
                .discount(request.discount())
                .advancePaid(request.advancePaid())
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

        String itemNames = items.stream()
                .map(Item::getName)
                .collect(Collectors.joining(", "));
        String message = String.format(
                "Hello %s, your booking (ID: %d) at %s is confirmed from %s to %s.\nItems: %s\nRemaining Amount: %s and Advance Given: %s",
                request.customerName(), savedBooking.getId(), vendor.getShopName(), fromDate, toDate, itemNames, remaining, request.advancePaid());
        whatsAppService.sendBookingNotification(request.customerPhone(), message);
        
        return mapToResponse(savedBooking);
    }

    public Page<BookingResponse> getVendorBookings(String username, BookingStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return bookingRepository.findVendorBookings(vendor.getId(), status, from, to, pageable)
                .map(this::mapToResponse);
    }

    public List<BookingResponse> searchBookingsByPhone(String username, String phone) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return bookingRepository
        .findByVendorIdAndCustomerPhoneContainingIgnoreCase(vendor.getId(), phone)
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
    }

    public Page<BookingResponse> searchBookingsByDate(String username, LocalDate date, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        LocalDate datePlusTwo = date.plusDays(2);
        return bookingRepository.findVendorBookingsByDate(vendor.getId(), date, datePlusTwo, pageable)
                .map(this::mapToResponse);
    }

    public Page<BookingResponse> searchReturnPendingBeforeDate(String username, LocalDate date, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        LocalDate deadlineWithBuffer = date.plusDays(2);
        return bookingRepository.findReturnPendingBeforeDate(vendor.getId(), deadlineWithBuffer, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public void moveBookingsToReturnPendingDaily() {
        LocalDate today = LocalDate.now();
        LocalDate bufferedDate = today.plusDays(2);
        List<Booking> bookings = bookingRepository.findByStatusAndToDate(BookingStatus.BOOKED, bufferedDate);
        if (bookings.isEmpty()) {
            return;
        }
        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.RETURN_PENDING);
        }
        bookingRepository.saveAll(bookings);
    }

    @Transactional
    public void updateBookingStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(status);
        bookingRepository.save(booking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        LocalDate displayToDate = booking.getToDate().minusDays(2);
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
