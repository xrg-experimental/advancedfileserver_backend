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
 @ToString(exclude = "users")
 @EqualsAndHashCode(exclude = "users")                                                                                                                                                                                       
 @Table(name = "groups")                                                                                                                                                                                                  
 public class Group {                                                                                                                                                                                                     
     @Id                                                                                                                                                                                                                  
     @GeneratedValue(strategy = GenerationType.AUTO)                                                                                                                                                                      
     private Long id;                                                                                                                                                                                                     
                                                                                                                                                                                                                          
     @Column(nullable = false)
     private String name;
                                                                                                                                                                                                                          
     private String description;                                                                                                                                                                                          
                                                                                                                                                                                                                          
     @Column(nullable = false)                                                                                                                                                                                            
     private String basePath;                                                                                                                                                                                             
                                                                                                                                                                                                                          
     @ManyToMany(mappedBy = "groups")                                                                                                                                                                                     
     private Set<User> users = new HashSet<>();                                                                                                                                                                           
                                                                                                                                                                                                                          
     private LocalDateTime createdAt;

     @OneToOne(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
     private GroupPermission permissions;
                                                                                                                                                                                                                          
     @PrePersist                                                                                                                                                                                                          
     protected void onCreate() {                                                                                                                                                                                          
         createdAt = LocalDateTime.now();                                                                                                                                                                                 
     }                                                                                                                                                                                                                    
 }
