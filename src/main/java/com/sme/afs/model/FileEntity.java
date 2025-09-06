package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "files")
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "files_seq")
    @SequenceGenerator(name = "files_seq", sequenceName = "files_seq")
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne
    @JoinColumn(name = "virtual_path_id", nullable = false)
    private VirtualPath virtualPath;

    @Column(nullable = false)
    private Long size;

    private String mimeType;

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

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private String checksum;

    @Column(nullable = false)
    private boolean isDirectory = false;

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @ManyToMany
    @JoinTable(
        name = "file_tag_mappings",
        joinColumns = @JoinColumn(name = "file_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<FileTag> tags = new HashSet<>();
}
