package com.example.uos_lms.feature.auth.presentation;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.UserRole;

/** Turns a resolved [AuthDestination] into the matching Navigation Component destination,
 * clearing the fragment we're leaving from the back stack - mirrors the old Compose
 * NavHost's popUpTo(currentRoute){inclusive=true} behavior on Splash/Login. */
public final class AuthNavigator {

    private AuthNavigator() {
    }

    public static void navigate(NavController navController, AuthDestination destination, int popUpToInclusiveOf) {
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(popUpToInclusiveOf, true)
                .build();

        switch (destination.getKind()) {
            case LOGIN:
                navController.navigate(R.id.loginFragment, null, options);
                break;
            case PENDING:
                navController.navigate(R.id.pendingApprovalFragment, null, options);
                break;
            case REJECTED:
                navController.navigate(R.id.rejectedFragment, null, options);
                break;
            case SUSPENDED:
                navController.navigate(R.id.suspendedFragment, null, options);
                break;
            case DASHBOARD:
                // Every role's home is a real Fragment destination - the whole app is native now.
                if (destination.getRole() == UserRole.ADMIN) {
                    navController.navigate(R.id.adminHomeFragment, null, options);
                } else if (destination.getRole() == UserRole.HOD) {
                    navController.navigate(R.id.hodDashboardFragment, null, options);
                } else if (destination.getRole() == UserRole.TEACHER) {
                    navController.navigate(R.id.teacherDashboardFragment, null, options);
                } else if (destination.getRole() == UserRole.STUDENT) {
                    navController.navigate(R.id.studentDashboardFragment, null, options);
                }
                break;
        }
    }

    /** Status screens' "Back to Login" - clears the whole back stack, not just the current entry. */
    public static void navigateToLoginClearingStack(NavController navController) {
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        navController.navigate(R.id.loginFragment, null, options);
    }
}
