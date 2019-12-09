package org.sendoh.model.validation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sendoh.model.input.Account;
import org.sendoh.model.violation.AuthorizationViolation;

import java.util.EnumSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private Account account;

    private EnumSet<AuthorizationViolation> violations;
}
