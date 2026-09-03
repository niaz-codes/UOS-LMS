package com.example.uos_lms.core.ui;

import android.view.View;
import android.widget.PopupMenu;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserStatus;

/** Java port of the Compose UserQuickActionsRow - the shared View/Approve/Reject/Suspend/
 * Activate/Reset Password/Delete popup menu, reused by every user list (Admin All/HOD/Teacher
 * tabs, HOD/Teacher student lists). Delete itself is expected to be confirmed by the caller
 * (via ConfirmDialogHelper) before actually deleting - onDelete here just signals the tap. */
public final class UserQuickActionsMenuHelper {

    public interface Actions {
        void onView();

        void onApprove();

        void onReject();

        void onSuspend();

        void onActivate();

        void onDeleteRequested();

        void onResetPassword();
    }

    private UserQuickActionsMenuHelper() {
    }

    /** isSelf: true when this row is the currently logged-in user's own account - hides every
     * action that could disable or remove it (Reject/Suspend/Activate/Delete), mirroring the
     * backend's own self-protection guard. View and Reset Password stay available either way,
     * since neither can lock the account out. */
    public static void attach(View anchor, User user, boolean isSelf, Actions actions) {
        anchor.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.getMenuInflater().inflate(R.menu.menu_user_quick_actions, popup.getMenu());
            popup.getMenu().findItem(R.id.actionApprove).setVisible(!isSelf && user.getStatus() == UserStatus.PENDING);
            popup.getMenu().findItem(R.id.actionReject).setVisible(!isSelf && user.getStatus() == UserStatus.PENDING);
            popup.getMenu().findItem(R.id.actionSuspend).setVisible(!isSelf && user.getStatus() == UserStatus.APPROVED);
            popup.getMenu().findItem(R.id.actionActivate).setVisible(!isSelf && user.getStatus() == UserStatus.SUSPENDED);
            popup.getMenu().findItem(R.id.actionDelete).setVisible(!isSelf);
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.actionView) actions.onView();
                else if (id == R.id.actionApprove) actions.onApprove();
                else if (id == R.id.actionReject) actions.onReject();
                else if (id == R.id.actionSuspend) actions.onSuspend();
                else if (id == R.id.actionActivate) actions.onActivate();
                else if (id == R.id.actionResetPassword) actions.onResetPassword();
                else if (id == R.id.actionDelete) actions.onDeleteRequested();
                return true;
            });
            popup.show();
        });
    }
}
