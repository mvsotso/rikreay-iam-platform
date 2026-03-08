package com.iam.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private List<String> realmRoles;
    private Map<String, List<String>> clientRoles;
    private List<String> groups;
    private String organization;
}
