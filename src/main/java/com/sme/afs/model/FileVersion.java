package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "file_versions")
public class FileVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_versions_seq")
    @SequenceGenerator(name = "file_versions_seq", sequenceName = "file_versions_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private String physicalPath;

    private String checksum;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    private String comment;
}
