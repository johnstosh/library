package com.muczynski.library.service;

import com.muczynski.library.domain.Settings;
import com.muczynski.library.dto.SettingsDto;
import com.muczynski.library.mapper.SettingsMapper;
import com.muczynski.library.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final SettingsMapper settingsMapper;

    @Autowired
    public SettingsService(SettingsRepository settingsRepository, SettingsMapper settingsMapper) {
        this.settingsRepository = settingsRepository;
        this.settingsMapper = settingsMapper;
    }

    public SettingsDto getSettings() {
        return settingsRepository.findAll().stream()
                .map(settingsMapper::toDto)
                .findFirst()
                .orElse(new SettingsDto(1L, false));
    }

    public SettingsDto updateSettings(SettingsDto settingsDto) {
        Settings settings = settingsMapper.toEntity(settingsDto);
        settings = settingsRepository.save(settings);
        return settingsMapper.toDto(settings);
    }
}