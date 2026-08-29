/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.dto.GlobalSettingsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for GlobalSettings entity to GlobalSettingsDto conversion.
 * Some fields require additional business logic and are set by the service layer.
 */
@Mapper(componentModel = "spring")
public interface GlobalSettingsMapper {

    /**
     * Convert GlobalSettings entity to DTO.
     * Note: Many fields require additional business logic (effective values, partial secrets,
     * validation) and are marked to be set by the service layer.
     */
    @Mapping(target = "googleClientSecret", ignore = true) // Never expose full secret
    @Mapping(target = "googleClientSecretPartial", ignore = true) // Set by service
    @Mapping(target = "googleClientSecretConfigured", ignore = true) // Set by service
    @Mapping(target = "googleClientSecretValidation", ignore = true) // Set by service
    @Mapping(target = "googleSsoClientSecret", ignore = true) // Never expose full secret
    @Mapping(target = "googleSsoClientSecretPartial", ignore = true) // Set by service
    @Mapping(target = "googleSsoClientSecretConfigured", ignore = true) // Set by service
    @Mapping(target = "googleSsoClientIdConfigured", ignore = true) // Set by service
    @Mapping(target = "googleSsoClientSecretValidation", ignore = true) // Set by service
    @Mapping(target = "smtpPassword", ignore = true)
    @Mapping(target = "smtpPasswordPartial", ignore = true)
    @Mapping(target = "smtpPasswordConfigured", ignore = true)
    @Mapping(target = "sendGridApiKey", ignore = true)
    @Mapping(target = "sendGridApiKeyPartial", ignore = true)
    @Mapping(target = "sendGridApiKeyConfigured", ignore = true)
    @Mapping(target = "webhookBearerToken", ignore = true)
    @Mapping(target = "webhookBearerTokenPartial", ignore = true)
    @Mapping(target = "webhookBearerTokenConfigured", ignore = true)
    @Mapping(target = "emailMethodConfigured", ignore = true)
    @Mapping(target = "emailMethodStatus", ignore = true)
    GlobalSettingsDto toDto(GlobalSettings globalSettings);
}
