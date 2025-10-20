document.addEventListener('DOMContentLoaded', function () {
    const settingsForm = document.getElementById('settings-form');
    const usernameInput = document.getElementById('settings-username');
    const passwordInput = document.getElementById('settings-password');
    const xaiApiKeyInput = document.getElementById('settings-xai-api-key');

    async function fetchUserSettings() {
        try {
            const response = await fetch('/api/users/me');
            if (response.ok) {
                const user = await response.json();
                usernameInput.value = user.username;
                xaiApiKeyInput.value = user.xaiApiKey;
            } else {
                console.error('Failed to fetch user settings');
            }
        } catch (error) {
            console.error('Error fetching user settings:', error);
        }
    }

    settingsForm.addEventListener('submit', async function (event) {
        event.preventDefault();
        const settings = {
            username: usernameInput.value,
            password: passwordInput.value,
            xaiApiKey: xaiApiKeyInput.value
        };

        const xhr = new XMLHttpRequest();
        xhr.open('PUT', '/api/users/settings');
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.onload = function () {
            if (xhr.status === 200) {
                alert('Settings updated successfully');
                fetchUserSettings();
            } else {
                const error = JSON.parse(xhr.responseText);
                console.error('Failed to update settings:', error);
                alert(`Failed to update settings: ${error.message}`);
            }
        };
        xhr.onerror = function () {
            console.error('Error updating settings:', xhr.statusText);
            alert('An error occurred while updating settings.');
        };
        xhr.send(JSON.stringify(settings));
    });

    window.showSettings = function (event) {
        showSection('settings', event);
        fetchUserSettings();
    }
});