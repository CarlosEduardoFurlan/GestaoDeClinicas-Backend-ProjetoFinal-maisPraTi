package br.com.clinica.service;

import br.com.clinica.exception.BusinessException;
import br.com.clinica.security.Role;

final class ProfileValidation {
    private ProfileValidation() {}

    static void validate(String profile) {
        if (profile != null) {
            for (Role role : Role.values()) {
                if (role.name().equals(profile)) return;
            }
        }
        throw new BusinessException("Perfil inválido ou não aprovado pelo sistema.");
    }
}
