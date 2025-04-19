package com.rentdi.entity;

import com.rentdi.entity.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private RoleName name;
    
//    // Explicit getter method for name
    public RoleName getName() {
        return name;
    }
} 