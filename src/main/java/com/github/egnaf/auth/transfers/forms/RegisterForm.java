package com.github.egnaf.auth.transfers.forms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterForm {

    private String username;
    private String email;
    private String password;
}
