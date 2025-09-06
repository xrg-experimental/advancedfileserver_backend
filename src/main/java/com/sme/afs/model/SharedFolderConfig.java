package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "shared_folder_configs")
public class SharedFolderConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024, unique = true)
    private String path;

    @Column(nullable = false)
    private boolean isBasePath = false;

    @Column(nullable = false)
    private boolean isTempPath = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    @OneToOne(mappedBy = "config", cascade = CascadeType.ALL)
    private SharedFolderValidation validation;
}
