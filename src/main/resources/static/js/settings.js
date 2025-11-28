// (c) Copyright 2025 by Muczynski


async function loadSettings() {
    try {
        const user = await fetchData('/api/user-settings');
        document.getElementById('user-name').value = user.username || '';
        document.getElementById('xai-api-key').value = user.xaiApiKey || '';

        // Update Google Photos OAuth status
        const hasGooglePhotosAuth = user.googlePhotosApiKey && user.googlePhotosApiKey.trim() !== '';
        updateGooglePhotosStatus(hasGooglePhotosAuth);

        // Store in hidden input for reference
        const googlePhotosInput = document.getElementById('google-photos-api-key');
        if (googlePhotosInput) {
            googlePhotosInput.value = user.googlePhotosApiKey || '';
        }

        // Load Google Photos Album ID
        const albumIdInput = document.getElementById('google-photos-album-id');
        if (albumIdInput) {
            albumIdInput.value = user.googlePhotosAlbumId || '';
        }

        // Load Library Card Design
        const cardDesignSelect = document.getElementById('settings-card-design');
        if (cardDesignSelect) {
            cardDesignSelect.value = user.libraryCardDesign || 'CLASSICAL_DEVOTION';
        }

        clearSettingsMessages();

        // Check for OAuth callback messages
        checkOAuthMessages();
    } catch (error) {
        showSettingsError('Failed to load settings: ' + error.message);
    }
}

function updateGooglePhotosStatus(isAuthorized) {
    const authorizedBadge = document.getElementById('google-photos-authorized');
    const notAuthorizedBadge = document.getElementById('google-photos-not-authorized');
    const authorizeBtn = document.getElementById('authorize-google-photos-btn');
    const revokeBtn = document.getElementById('revoke-google-photos-btn');

    if (isAuthorized) {
        authorizedBadge.style.display = 'inline';
        notAuthorizedBadge.style.display = 'none';
        authorizeBtn.style.display = 'none';
        revokeBtn.style.display = 'inline';
    } else {
        authorizedBadge.style.display = 'none';
        notAuthorizedBadge.style.display = 'inline';
        authorizeBtn.style.display = 'inline';
        revokeBtn.style.display = 'none';
    }
}

function authorizeGooglePhotos() {
    // Redirect to OAuth authorization endpoint with current origin
    const origin = window.location.origin;
    window.location.href = '/api/oauth/google/authorize?origin=' + encodeURIComponent(origin);
}

async function revokeGooglePhotos() {
    if (!confirm('Are you sure you want to revoke Google Photos access?')) {
        return;
    }

    const btn = document.getElementById('revoke-google-photos-btn');
    showButtonSpinner(btn, 'Revoking...');

    try {
        await postData('/api/oauth/google/revoke', {});
        showSettingsSuccess('Google Photos access revoked');
        updateGooglePhotosStatus(false);
    } catch (error) {
        showSettingsError('Failed to revoke access: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Revoke Access');
    }
}

function checkOAuthMessages() {
    const urlParams = new URLSearchParams(window.location.search);
    const oauthSuccess = urlParams.get('oauth_success');
    const oauthError = urlParams.get('oauth_error');

    if (oauthSuccess) {
        showSettingsSuccess('Google Photos authorized successfully!');
        // Reload settings to update status
        setTimeout(() => loadSettings(), 500);
        // Clean up URL
        window.history.replaceState({}, document.title, window.location.pathname + '#settings');
    } else if (oauthError) {
        showSettingsError('OAuth error: ' + oauthError);
        // Clean up URL
        window.history.replaceState({}, document.title, window.location.pathname + '#settings');
    }
}

async function saveSettings(event) {
    event.preventDefault();
    clearSettingsMessages();

    const username = document.getElementById('user-name').value.trim();
    const password = document.getElementById('user-password').value;
    const xaiApiKey = document.getElementById('xai-api-key').value.trim();
    const googlePhotosInput = document.getElementById('google-photos-api-key');
    const googlePhotosApiKey = googlePhotosInput ? googlePhotosInput.value.trim() : '';
    const albumIdInput = document.getElementById('google-photos-album-id');
    const googlePhotosAlbumId = albumIdInput ? albumIdInput.value.trim() : '';
    const cardDesignSelect = document.getElementById('settings-card-design');
    const libraryCardDesign = cardDesignSelect ? cardDesignSelect.value : 'CLASSICAL_DEVOTION';

    if (!username) {
        showSettingsError('Name is required.');
        return;
    }

    const payload = {
        username,
        xaiApiKey,
        googlePhotosApiKey,
        googlePhotosAlbumId,
        libraryCardDesign
    };

    if (password) {
        payload.password = await hashPassword(password);
    }

    const btn = document.querySelector('#settings-form button[type="submit"]');
    showButtonSpinner(btn, 'Saving...');

    try {
        await putData('/api/user-settings', payload);
        showSettingsSuccess('Settings saved successfully!');
        document.getElementById('user-password').value = ''; // Clear password field after save

        // Also update the library card section's radio buttons if visible
        const radioButton = document.getElementById(`design-${libraryCardDesign.toLowerCase().replace(/_/g, '-')}`);
        if (radioButton) {
            radioButton.checked = true;
        }
    } catch (error) {
        showSettingsError('Failed to save settings: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Save User Settings');
    }
}

function showSettingsError(message) {
    const errorEl = document.getElementById('settings-error');
    errorEl.textContent = message;
    errorEl.style.display = 'block';
}

function showSettingsSuccess(message) {
    const successEl = document.getElementById('settings-success');
    successEl.textContent = message;
    successEl.style.display = 'block';
}

function clearSettingsMessages() {
    document.getElementById('settings-error').style.display = 'none';
    document.getElementById('settings-success').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', () => {
    const settingsForm = document.getElementById('settings-form');
    if (settingsForm) {
        settingsForm.addEventListener('submit', saveSettings);
    }
});

// Expose functions globally for HTML onclick/onsubmit handlers and sections.js
window.loadSettings = loadSettings;
window.saveSettings = saveSettings;
window.authorizeGooglePhotos = authorizeGooglePhotos;
window.revokeGooglePhotos = revokeGooglePhotos;