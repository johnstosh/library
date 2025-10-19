package com.muczynski.library.mapper;

import com.muczynski.library.domain.Settings;
import com.muczynski.library.dto.SettingsDto;
import org.springframework.stereotype.Component;

@Component
public class SettingsMapper {

    public SettingsDto toDto(Settings settings) {
        return new SettingsDto(
                settings.getId(),
                settings.isDarkMode()
        );
    }

    public Settings toEntity(SettingsDto settingsDto) {
        return new Settings(
                settingsDto.getId(),
                settingsDto.isDarkMode()
        );
    }
}