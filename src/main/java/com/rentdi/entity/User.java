package com.rentdi.entity;

import com.rentdi.entity.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Getter
    @Setter
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Getter
    @Setter
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Getter
    @Setter
    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(nullable = false)
    private String password;

    @Getter
    @Setter
    @Column(name = "profile_image")
    private String profileImage;

    @Getter
    @Setter
    @Column(name = "phone_number")
    private String phoneNumber;

    @Getter
    @Setter
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Getter
    @Setter
    @Column(name = "is_logged_in", nullable = false)
    private Boolean isLoggedIn = false;
    
    @Getter
    @Setter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> {
                    RoleName roleName = role.getName();
                    return new SimpleGrantedAuthority("ROLE_" + roleName.name());
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
} 