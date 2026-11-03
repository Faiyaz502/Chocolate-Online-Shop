package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.address.AddressRequest;
import com.example.chocolateShop.dtos.address.AddressResponse;
import com.example.chocolateShop.entity.Address;
import com.example.chocolateShop.entity.User;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.repository.AddressRepository;
import com.example.chocolateShop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final Logger log = LoggerFactory.getLogger(AddressService.class);

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressResponse> getAllForUser(Long userId) {
        log.info("Fetching all addresses for userId={}", userId);

        List<AddressResponse> addresses = addressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        log.info("Found {} address(es) for userId={}", addresses.size(), userId);

        return addresses;
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        log.info("Creating address for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found. userId={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault())
                || addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty();

        Address address = Address.builder()
                .user(user)
                .label(request.label())
                .recipientName(request.recipientName())
                .phone(request.phone())
                .line1(request.line1())
                .line2(request.line2())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(makeDefault)
                .build();

        address = addressRepository.save(address);

        if (makeDefault) {
            log.info("Setting address {} as default for userId={}", address.getId(), userId);
            addressRepository.clearDefaultForOtherAddresses(userId, address.getId());
        }

        log.info("Address created successfully. addressId={}, userId={}",
                address.getId(), userId);

        return toResponse(address);
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        log.info("Updating addressId={} for userId={}", addressId, userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> {
                    log.warn("Address not found. addressId={}, userId={}", addressId, userId);
                    return new ResourceNotFoundException("Address not found with id: " + addressId);
                });

        address.setRecipientName(request.recipientName());
        address.setPhone(request.phone());
        address.setLabel(request.label());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());

        if (Boolean.TRUE.equals(request.isDefault())) {
            log.info("Setting addressId={} as default for userId={}", addressId, userId);
            address.setDefault(true);
            addressRepository.clearDefaultForOtherAddresses(userId, address.getId());
        }

        log.info("Address updated successfully. addressId={}, userId={}",
                addressId, userId);

        return toResponse(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        log.info("Deleting addressId={} for userId={}", addressId, userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> {
                    log.warn("Address not found. addressId={}, userId={}", addressId, userId);
                    return new ResourceNotFoundException("Address not found with id: " + addressId);
                });

        addressRepository.delete(address);

        log.info("Address deleted successfully. addressId={}, userId={}",
                addressId, userId);
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .build();
    }
}
