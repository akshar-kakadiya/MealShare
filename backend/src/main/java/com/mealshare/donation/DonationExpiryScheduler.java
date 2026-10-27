package com.mealshare.donation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DonationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DonationExpiryScheduler.class);

    private final DonationExpiryService donationExpiryService;

    public DonationExpiryScheduler(DonationExpiryService donationExpiryService) {
        this.donationExpiryService = donationExpiryService;
    }

    @Scheduled(cron = "${mealshare.donation.expiry-cron:0 */5 * * * *}")
    public void runExpiryScan() {
        try {
            int count = donationExpiryService.expireOverdueDonations();
            if (count > 0) {
                log.info("Scheduled donation expiry scan completed: {} donations expired.", count);
            }
        } catch (Exception e) {
            log.error("Error occurred during scheduled donation expiry scan", e);
        }
    }
}
