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
@Table(name = "virtual_paths",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"virtual_path"})
    },
    indexes = {
        @Index(name = "idx_virtual_path", columnList = "virtual_path"),
        @Index(name = "idx_physical_path", columnList = "physical_path")
    })
public class VirtualPath {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virtual_paths_seq")
    @SequenceGenerator(name = "virtual_paths_seq", sequenceName = "virtual_paths_seq", allocationSize = 50)
    private Long id;

    @Column(name = "virtual_path", nullable = false, length = 1024)
    private String virtualPath;

    @Column(name = "physical_path", nullable = false, length = 1024)
    private String physicalPath;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private VirtualPath parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private Set<VirtualPath> children = new HashSet<>();

    @Column(nullable = false)
    private boolean isDirectory;

    public void addChild(VirtualPath child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(VirtualPath child) {
        children.remove(child);
        child.setParent(null);
    }

    public String getParentPath() {
        int lastSlash = virtualPath.lastIndexOf('/');
        return lastSlash > 0 ? virtualPath.substring(0, lastSlash) : "/";
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isAncestorOf(VirtualPath other) {
        VirtualPath current = other;
        while (current != null) {
            if (current.equals(this)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    @ManyToOne
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @OneToOne(mappedBy = "virtualPath")
    private FileEntity fileEntity;
}
