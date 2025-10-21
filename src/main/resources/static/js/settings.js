async function loadSettings() {
    try {
        const user = await fetchData('/api/users/me');
        const apiKeyInput = document.getElementById('xai-api-key');
        apiKeyInput.value = user.xaiApiKey || '';
        clearError('settings');
    } catch (error) {
        showError('settings', 'Failed to load settings: ' + error.message);
    }
}

async function saveSettings() {
    const apiKey = document.getElementById('xai-api-key').value.trim();
    if (!apiKey) {
        showError('settings', 'XAI API key is required.');
        return;
    }
    // Basic validation: xAI keys are typically 32+ chars, base64-like
    if (apiKey.length < 32 || !/^[A-Za-z0-9_-]+$/.test(apiKey)) {
        showError('settings', 'Invalid XAI API key format.');
        return;
    }
    try {
        const user = await fetchData('/api/users/me');
        await putData(`/api/users/${user.id}/apikey`, { xaiApiKey: apiKey });
        showBulkSuccess('xai-api-key'); // Reuse success style
        clearError('settings');
    } catch (error) {
        showError('settings', 'Failed to save settings: ' + error.message);
    }
}

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('settings-section')) {
        loadSettings();
    }
});
