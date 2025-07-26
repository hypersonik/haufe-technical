package com.haufe.technical.api.utils;

import com.haufe.technical.api.auth.HaufeUserDetails;
import com.haufe.technical.api.exception.ApiException;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Optional;

@UtilityClass
public class ReactiveSecurityUtils {
    /**
     * Retrieves the current user's details from the security context.
     *
     * @return a Mono containing the {@link HaufeUserDetails} of the authenticated user.
     */
    public static Mono<HaufeUserDetails> getUserDetails() {
        return ReactiveSecurityContextHolder.getContext()
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required")))
                .map(SecurityContext::getAuthentication)
                .map(authentication -> (HaufeUserDetails) authentication.getPrincipal());
    }

    /**
     * Retrieves the current user's manufacturer ID from the security context.
     * Can be null if the user is not a manufacturer.
     *
     * @return a Mono containing the manufacturer ID of the authenticated user.
     */
    public static Mono<Optional<Long>> getManufacturerId() {
        return getUserDetails()
                .map(userDetails -> Optional.ofNullable(userDetails.getManufacturerId()));
    }
}
