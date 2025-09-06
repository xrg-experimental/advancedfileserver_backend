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
@Table(name = "file_tags")
public class FileTag {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_tags_seq")
    @SequenceGenerator(name = "file_tags_seq", sequenceName = "file_tags_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "tags")
    private Set<FileEntity> files = new HashSet<>();
}
