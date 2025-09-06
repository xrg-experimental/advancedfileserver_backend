package com.sme.afs.model;                                                                                                                                                                                               
                                                                                                                                                                                                                          
 import jakarta.persistence.*;
 import lombok.*;

 import java.time.LocalDateTime;                                                                                                                                                                                          
 import java.util.HashSet;                                                                                                                                                                                                
 import java.util.Set;                                                                                                                                                                                                    
                                                                                                                                                                                                                          
 @Getter
 @Setter
 @Entity                                                                                                                                                                                                                  
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 @ToString(exclude = "groups")
 @EqualsAndHashCode(exclude = "groups")                                                                                                                                                                                       
 @Table(name = "users")                                                                                                                                                                                                   
 public class User {                                                                                                                                                                                                      
     @Id                                                                                                                                                                                                                  
     @GeneratedValue(strategy = GenerationType.AUTO)                                                                                                                                                                      
     private Long id;                                                                                                                                                                                                     
                                                                                                                                                                                                                          
     @Column(unique = true, nullable = false)                                                                                                                                                                             
     private String username;                                                                                                                                                                                             
                                                                                                                                                                                                                          
     @Column(nullable = false)                                                                                                                                                                                            
     private String password;                                                                                                                                                                                             
                                                                                                                                                                                                                          
     @Column(nullable = false)                                                                                                                                                                                            
     private String email;                                                                                                                                                                                                
     
     @Column(length = 50)
     private String displayName;
                                                                                                                                                                                                                          
     private boolean enabled;
                                                                                                                                                                                                                          
     @Enumerated(EnumType.STRING)                                                                                                                                                                                         
     private UserType userType;
     
     @ElementCollection(fetch = FetchType.EAGER)
     @Enumerated(EnumType.STRING)
     @Builder.Default()
     private Set<Role> roles = new HashSet<>();
     
     @ManyToMany(fetch = FetchType.EAGER)                                                                                                                                                                                 
     @JoinTable(                                                                                                                                                                                                          
         name = "user_groups",                                                                                                                                                                                            
         joinColumns = @JoinColumn(name = "user_id"),                                                                                                                                                                     
         inverseJoinColumns = @JoinColumn(name = "group_id")                                                                                                                                                              
     )
     @Builder.Default()
     private Set<Group> groups = new HashSet<>();
                                                                                                                                                                                                                          
     private LocalDateTime createdAt;                                                                                                                                                                                     
     private LocalDateTime lastLogin;
     
     @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
     private UserOtpSettings otpSettings;
                                                                                                                                                                                                                          
     @PrePersist                                                                                                                                                                                                          
     protected void onCreate() {                                                                                                                                                                                          
         createdAt = LocalDateTime.now();                                                                                                                                                                                 
     }
 }
