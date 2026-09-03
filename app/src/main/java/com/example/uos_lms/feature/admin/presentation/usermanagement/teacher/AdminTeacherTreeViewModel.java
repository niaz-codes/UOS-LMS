package com.example.uos_lms.feature.admin.presentation.usermanagement.teacher;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.feature.admin.presentation.usermanagement.common.AdminRoleTreeViewModel;
import com.example.uos_lms.feature.auth.data.AuthDataSource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminTeacherTreeViewModel extends AdminRoleTreeViewModel {

    @Inject
    public AdminTeacherTreeViewModel(ApiUniversityDataSource universityDataSource, ApiUserDataSource userDataSource, AuthDataSource authDataSource) {
        super(universityDataSource, userDataSource, authDataSource, UserRole.TEACHER);
    }

    @Override
    protected String roleLabel() {
        return "Teacher";
    }
}
