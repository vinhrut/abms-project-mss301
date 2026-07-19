package com.abms.finance.config;

import com.abms.finance.entity.ServiceItem;
import com.abms.finance.repository.ServiceItemRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ServiceItemRepository serviceItemRepository;

    @Override
    public void run(String... args) {
        seedServices();
    }

    private void seedServices() {
        if (serviceItemRepository.count() > 0) {
            return;
        }

        serviceItemRepository.save(ServiceItem.builder()
                .serviceId(1)
                .name("Electricity")
                .unitPrice(new BigDecimal("3500.00"))
                .unit("kWh")
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .serviceId(2)
                .name("Water")
                .unitPrice(new BigDecimal("18000.00"))
                .unit("m3")
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .serviceId(3)
                .name("Management Fee")
                .unitPrice(new BigDecimal("15000.00"))
                .unit("m2")
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .serviceId(4)
                .name("Parking Fee")
                .unitPrice(new BigDecimal("1200000.00"))
                .unit("slot")
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .serviceId(5)
                .name("Internet")
                .unitPrice(new BigDecimal("250000.00"))
                .unit("month")
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .serviceId(6)
                .name("Garbage Fee")
                .unitPrice(new BigDecimal("50000.00"))
                .unit("apartment")
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .serviceId(7)
                .name("Late Fee")
                .unitPrice(new BigDecimal("100000.00"))
                .unit("fixed")
                .build());
    }
}
