package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "file_shares")
public class FileShare {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_shares_seq")
    @SequenceGenerator(name = "file_shares_seq", sequenceName = "file_shares_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @ManyToOne
    @JoinColumn(name = "shared_by", nullable = false)
    private User sharedBy;

    @ManyToOne
    @JoinColumn(name = "shared_with")
    private User sharedWith;

    @ManyToOne
    @JoinColumn(name = "shared_with_group")
    private Group sharedWithGroup;

    private String accessToken;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastAccessed;

    private Integer accessCount = 0;

    private boolean canWrite = false;

    private boolean canShare = false;
}
