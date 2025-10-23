async function loadSettings() {
    try {
        const user = await fetchData('/api/user-settings');
        document.getElementById('user-name').value = user.username || '';
        document.getElementById('xai-api-key').value = user.xaiApiKey || '';
        clearSettingsMessages();
    } catch (error) {
        showSettingsError('Failed to load settings: ' + error.message);
    }
}

async function saveSettings(event) {
    event.preventDefault();
    clearSettingsMessages();

    const username = document.getElementById('user-name').value.trim();
    const password = document.getElementById('user-password').value;
    const xaiApiKey = document.getElementById('xai-api-key').value.trim();

    if (!username) {
        showSettingsError('Name is required.');
        return;
    }

    const payload = {
        username,
        xaiApiKey
    };

    if (password) {
        payload.password = password;
    }

    try {
        await putData('/api/user-settings', payload);
        showSettingsSuccess('Settings saved successfully!');
        document.getElementById('user-password').value = ''; // Clear password field after save
    } catch (error) {
        showSettingsError('Failed to save settings: ' + error.message);
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