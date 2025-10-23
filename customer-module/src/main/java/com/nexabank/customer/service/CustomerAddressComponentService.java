package com.nexabank.customer.service;

import com.nexabank.customer.entity.CustomerAddressComponent;
import com.nexabank.customer.repository.CustomerAddressComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerAddressComponentService {
    
    @Autowired
    private CustomerAddressComponentRepository addressComponentRepository;
    
    public CustomerAddressComponent save(CustomerAddressComponent addressComponent) {
        return addressComponentRepository.save(addressComponent);
    }
    
    public List<CustomerAddressComponent> findByCustomerCustomerId(String customerId) {
        return addressComponentRepository.findByCustomerCustomerId(customerId);
    }
    
    public Optional<CustomerAddressComponent> findByCustomerIdAndType(String customerId, CustomerAddressComponent.AddressComponentType type) {
        return addressComponentRepository.findByCustomerCustomerIdAndAddressComponentType(customerId, type);
    }
    
    public List<CustomerAddressComponent> findByType(CustomerAddressComponent.AddressComponentType type) {
        return addressComponentRepository.findByAddressComponentType(type);
    }
    
    public List<CustomerAddressComponent> searchByAddressValue(String addressValue) {
        return addressComponentRepository.findByAddressValueContaining(addressValue);
    }
    
    public void deleteByCustomerCustomerId(String customerId) {
        addressComponentRepository.deleteByCustomerCustomerId(customerId);
    }
    
    public void deleteById(String id) {
        addressComponentRepository.deleteById(id);
    }
    
    public List<CustomerAddressComponent> findAll() {
        return addressComponentRepository.findAll();
    }
}
