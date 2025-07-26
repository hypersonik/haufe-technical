package com.haufe.technical.api.auth;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@Builder
public class HaufeUserDetails implements UserDetails {
    private Long id;
    private Long manufacturerId;
    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Populates the roles. This method is a shortcut for calling
     * {@link #setAuthorities(Collection)}, but automatically prefixes each entry with
     * "ROLE_". This means the following:
     *
     * <code>
     *     builder.roles("USER","ADMIN");
     * </code>
     *
     * is equivalent to
     *
     * <code>
     *     builder.authorities("ROLE_USER","ROLE_ADMIN");
     * </code>
     *
     * <p>
     * This attribute is required, but can also be populated with
     * {@link #setAuthorities(Collection)}.
     * </p>
     * @param roles the roles for this user (i.e. USER, ADMIN, etc). Cannot be null,
     * contain null values or start with "ROLE_"
     * @return the {@link HaufeUserDetails} for method chaining (i.e. to populate
     * additional attributes for this user)
     */
    public HaufeUserDetails roles(String... roles) {
        List<GrantedAuthority> authorities = new ArrayList<>(roles.length);
        for (String role : roles) {
            Assert.isTrue(!role.startsWith("ROLE_"),
                    () -> role + " cannot start with ROLE_ (it is automatically added)");
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        this.authorities = authorities;
        return this;
    }
}
