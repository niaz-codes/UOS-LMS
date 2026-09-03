package com.example.uos_lms.feature.auth.presentation;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.domain.model.UserRole;

/** Where Splash/Login should send the user next, resolved from the backend account
 * (role + status) rather than anything the user picked in the UI. */
public final class AuthDestination {

    public enum Kind { LOGIN, PENDING, REJECTED, SUSPENDED, DASHBOARD }

    private final Kind kind;
    @Nullable
    private final UserRole role;

    private AuthDestination(Kind kind, @Nullable UserRole role) {
        this.kind = kind;
        this.role = role;
    }

    public static AuthDestination login() {
        return new AuthDestination(Kind.LOGIN, null);
    }

    public static AuthDestination pending() {
        return new AuthDestination(Kind.PENDING, null);
    }

    public static AuthDestination rejected() {
        return new AuthDestination(Kind.REJECTED, null);
    }

    public static AuthDestination suspended() {
        return new AuthDestination(Kind.SUSPENDED, null);
    }

    public static AuthDestination dashboard(UserRole role) {
        return new AuthDestination(Kind.DASHBOARD, role);
    }

    public Kind getKind() {
        return kind;
    }

    @Nullable
    public UserRole getRole() {
        return role;
    }
}
