package com.example.sbp.security;

import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@ToString
@Getter
public class CustomUserDetails implements UserDetails {
    private final String username;
    private final String password;
    private final String role;
    private final Set<Privilege> privileges;
    private final Long accountId;
    private final String phoneNumber;
    private final int tokenVersion;

    public CustomUserDetails(String username, String password, String role, Set<Privilege> privileges,
                             Long accountId, String phoneNumber, int tokenVersion) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.privileges = privileges;
        this.accountId = accountId;
        this.phoneNumber = phoneNumber;
        this.tokenVersion = tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        var authorities = new ArrayList<GrantedAuthority>();
        privileges.forEach(privilege -> authorities.add(new SimpleGrantedAuthority(privilege.name())));
        return authorities;
    }

    public boolean hasPrivilege(Privilege privilege) {
        return privileges.contains(privilege);
    }
}
