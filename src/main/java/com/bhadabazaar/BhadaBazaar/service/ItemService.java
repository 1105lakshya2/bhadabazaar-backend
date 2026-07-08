package com.bhadabazaar.BhadaBazaar.service;
import com.bhadabazaar.BhadaBazaar.exception.BusinessException;

import com.bhadabazaar.BhadaBazaar.domain.entity.Item;
import com.bhadabazaar.BhadaBazaar.domain.entity.ItemImage;
import com.bhadabazaar.BhadaBazaar.domain.entity.Vendor;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemCategory;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemGenderType;
import com.bhadabazaar.BhadaBazaar.domain.enums.ItemImageType;
import com.bhadabazaar.BhadaBazaar.domain.enums.SubscriptionTier;
import com.bhadabazaar.BhadaBazaar.dto.ItemCreateRequest;
import com.bhadabazaar.BhadaBazaar.dto.ItemImageResponse;
import com.bhadabazaar.BhadaBazaar.dto.ItemResponse;
import com.bhadabazaar.BhadaBazaar.repository.ItemImageRepository;
import com.bhadabazaar.BhadaBazaar.repository.ItemRepository;
import com.bhadabazaar.BhadaBazaar.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final VendorRepository vendorRepository;
    private final ItemImageRepository itemImageRepository;
    private final CloudflareImageService cloudinaryService;

    @Value("${spring.servlet.multipart.location:uploads}")
    private String uploadDir;

    @Transactional
    public ItemResponse createItem(String username, ItemCreateRequest request) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Vendor not found"));

        // Enforce the vendor's subscription item cap (counts only live, non-deleted items).
        SubscriptionTier tier = vendor.getSubscriptionTier();
        if (!tier.isUnlimited()) {
            long currentCount = itemRepository.countByVendorIdAndIsDeletedFalse(vendor.getId());
            if (currentCount >= tier.getMaxItems()) {
                throw new IllegalArgumentException(
                        "Item limit reached for your " + tier + " plan (max " + tier.getMaxItems()
                                + " items). Upgrade your plan to add more.");
            }
        }

        // item_code must be unique within this vendor's active items (names are not unique).
        if (itemRepository.existsByVendorIdAndItemCodeAndIsDeletedFalse(vendor.getId(), request.itemCode())) {
            throw new IllegalArgumentException("Item code already exists");
        }

        Item item = Item.builder()
                .vendor(vendor)
                .itemCode(request.itemCode())
                .name(request.name())
                .category(request.category())
                .gender(request.gender())
                .pricePerDay(request.pricePerDay())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        try {
            item = itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent creates can both pass the check above; the partial unique index is the
            // real guard. Translate the loser's violation into a clean message.
            throw new IllegalArgumentException("Item code already exists");
        }
        return mapToResponse(item);
    }

    @Transactional
    public ItemResponse updateItem(String username, Long itemId, ItemCreateRequest request) {
        Item item = findOwnedItem(username, itemId);

        // Reject a code that collides with another active item of the same vendor (the item's own
        // current code is fine).
        if (itemRepository.existsByVendorIdAndItemCodeAndIsDeletedFalseAndIdNot(
                item.getVendor().getId(), request.itemCode(), item.getId())) {
            throw new IllegalArgumentException("Item code already exists");
        }

        item.setItemCode(request.itemCode());
        item.setName(request.name());
        item.setCategory(request.category());
        item.setGender(request.gender());
        item.setPricePerDay(request.pricePerDay());
        if (request.isActive() != null) {
            item.setIsActive(request.isActive());
        }

        try {
            item = itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Item code already exists");
        }
        return mapToResponse(item);
    }

    @Transactional
    public void deleteItem(String username, Long itemId) {
        Item item = findOwnedItem(username, itemId);
        for (ItemImage img : item.getImages()) {
            if (img.getImagePublicId() != null) {
                cloudinaryService.deleteFile(img.getImagePublicId());
            }
        }
        item.setIsDeleted(true);
        itemRepository.save(item);
    }

    @Transactional
    public void uploadImages(String username, Long itemId, java.util.List<MultipartFile> files) {
        Item item = findOwnedItem(username, itemId);

        if (files == null || files.isEmpty() || files.size() > 4) {
            throw new IllegalArgumentException("Number of images must be between 1 and 4");
        }

        java.util.List<ItemImage> existingImages = item.getImages().stream()
                .sorted(java.util.Comparator.comparing(img -> img.getSortOrder() == null ? 0 : img.getSortOrder()))
                .collect(java.util.stream.Collectors.toList());

        int existingCount = existingImages.size();
        int newCount = files.size();
        int minCount = Math.min(existingCount, newCount);

        for (int i = 0; i < minCount; i++) {
            ItemImage existing = existingImages.get(i);
            if (existing.getImagePublicId() != null) {
                cloudinaryService.deleteFile(existing.getImagePublicId());
            }
            java.util.Map<String, String> upload = cloudinaryService.uploadFile(files.get(i));
            existing.setImageUrl(upload.get("url"));
            existing.setImagePublicId(upload.get("public_id"));
        }

        if (newCount < existingCount) {
            for (int i = existingCount - 1; i >= newCount; i--) {
                ItemImage toRemove = existingImages.get(i);
                if (toRemove.getImagePublicId() != null) {
                    cloudinaryService.deleteFile(toRemove.getImagePublicId());
                }
                itemImageRepository.delete(toRemove);
                existingImages.remove(i);
                item.getImages().remove(toRemove);
            }
        } else if (newCount > existingCount) {
            for (int i = existingCount; i < newCount; i++) {
                java.util.Map<String, String> upload = cloudinaryService.uploadFile(files.get(i));
                ItemImage image = ItemImage.builder()
                        .item(item)
                        .imageUrl(upload.get("url"))
                        .imagePublicId(upload.get("public_id"))
                        .imageType(ItemImageType.SECONDARY)
                        .sortOrder(i)
                        .build();
                itemImageRepository.save(image);
                existingImages.add(image);
                item.getImages().add(image);
            }
        }

        for (int i = 0; i < existingImages.size(); i++) {
            ItemImage img = existingImages.get(i);
            img.setSortOrder(i);
            img.setImageType(i == 0 ? ItemImageType.MAIN : ItemImageType.SECONDARY);
            itemImageRepository.save(img);
        }
    }
    
    @Transactional
    public void deleteImage(String username, Long itemId, Long imageId) {
        Item item = findOwnedItem(username, itemId);
        ItemImage image = itemImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("Image not found"));
        if (!image.getItem().getId().equals(item.getId())) {
             throw new BusinessException("Image does not belong to item");
        }
        if (image.getImagePublicId() != null) {
            cloudinaryService.deleteFile(image.getImagePublicId());
        }
        itemImageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> getVendorItems(String username, ItemCategory category, ItemGenderType gender, String searchByName, String searchByID, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        return itemRepository.findVendorItems(vendor.getId(), category, gender, searchByName, searchByID, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> getAvailableItems(Long vendorId, LocalDate from, LocalDate to, String categoryStr, String genderStr, String searchByName, String searchByID, Pageable pageable) {
         // Convert strings to Enums if necessary or pass as is if Repo expects String/Enum
         // Repo expects String for flexibility in native query or I can convert.
         // Native query parameters are Strings.
         return itemRepository.findAvailableItems(vendorId, from, to, categoryStr, genderStr, searchByName,searchByID, pageable)
                 .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public ItemResponse getItemDetails(Long itemId) {
        Item item = itemRepository.findByIdAndIsDeletedFalseAndIsActiveTrue(itemId)
                .orElseThrow(() -> new BusinessException("Item not found"));
        return mapToResponse(item);
    }

    @Transactional(readOnly = true)
    public ItemResponse getVendorItemDetails(String username, Long itemId) {
        return mapToResponse(findOwnedItem(username, itemId));
    }

    private Item findOwnedItem(String username, Long itemId) {
        Vendor vendor = vendorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        return itemRepository.findByIdAndVendorId(itemId, vendor.getId())
                .orElseThrow(() -> new BusinessException("Item not found"));
    }
    
    @Transactional(readOnly = true)
    public ItemResponse getItemByCodeForVendor(Long vendorId, String itemCode) {
        Item item = itemRepository.findByVendorIdAndItemCode(vendorId, itemCode)
                .orElseThrow(() -> new BusinessException("Item not found"));
        return mapToResponse(item);
    }

    private ItemResponse mapToResponse(Item item) {
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
