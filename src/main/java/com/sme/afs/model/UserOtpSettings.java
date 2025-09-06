package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "user_otp_settings")
public class UserOtpSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private boolean otpEnabled;
    
    @Column(length = 32)
    private String otpSecret;
    
    @Column(nullable = false)
    private boolean required;
}
