package br.com.clinica.dto.response;

public record AddressResponseDTO(
        String address,
        Integer number,
        String complement,
        String district,
        String city,
        String uf,
        String zipCode
) {
}
