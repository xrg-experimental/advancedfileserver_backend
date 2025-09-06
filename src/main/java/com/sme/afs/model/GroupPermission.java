package com.sme.afs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "group_permissions")
public class GroupPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_permissions_seq")
    @SequenceGenerator(name = "group_permissions_seq", sequenceName = "group_permissions_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    private boolean canRead = true;
    private boolean canWrite = false;
    private boolean canDelete = false;
    private boolean canShare = false;
    private boolean canUpload = false;
}
