package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "shared_folder_validations")
public class SharedFolderValidation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "config_id", nullable = false, unique = true)
    private SharedFolderConfig config;

    @Column(nullable = false)
    private boolean isValid;

    @Column(nullable = false)
    private LocalDateTime lastCheckedAt;

    private String errorMessage;

    @ManyToOne
    @JoinColumn(name = "checked_by")
    private User checkedBy;

    private Boolean canRead;
    private Boolean canWrite;
    private Boolean canExecute;
    private String permissionCheckError;
}
