package com.sme.afs.repository;

import com.sme.afs.model.UserOtpSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserOtpSettingsRepository extends JpaRepository<UserOtpSettings, Long> {
    Optional<UserOtpSettings> findByUserId(Long userId);
}
