/**
 * Global Settings Management (Librarian-only)
 */

// Load global settings on page load
async function loadGlobalSettings() {
    try {
        const response = await fetch('/api/global-settings', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok) {
            const settings = await response.json();
            displayGlobalSettings(settings);
        } else {
            console.error('Failed to load global settings:', response.status);
        }
    } catch (error) {
        console.error('Error loading global settings:', error);
    }
}

// Display global settings in the UI
function displayGlobalSettings(settings) {
    // Display Client ID (read-only)
    const clientIdElement = document.getElementById('global-client-id');
    if (clientIdElement) {
        clientIdElement.textContent = settings.googleClientId || '(not configured)';
    }

    // Display Redirect URI (read-only)
    const redirectUriElement = document.getElementById('global-redirect-uri');
    if (redirectUriElement) {
        const redirectUri = settings.redirectUri || window.location.origin + '/api/oauth/google/callback';
        redirectUriElement.textContent = redirectUri;
    }

    // Display partial Client Secret
    const secretPartialElement = document.getElementById('global-secret-partial');
    if (secretPartialElement) {
        secretPartialElement.textContent = settings.googleClientSecretPartial || '(not configured)';
    }

    // Display Client Secret validation
    const secretValidationElement = document.getElementById('global-secret-validation');
    if (secretValidationElement) {
        secretValidationElement.textContent = settings.googleClientSecretValidation || '';

        // Color code the validation message
        if (settings.googleClientSecretValidation === 'Valid') {
            secretValidationElement.className = 'text-success';
        } else if (settings.googleClientSecretValidation && settings.googleClientSecretValidation.startsWith('Warning')) {
            secretValidationElement.className = 'text-warning';
        } else {
            secretValidationElement.className = 'text-danger';
        }
    }

    // Display last updated timestamp
    const lastUpdatedElement = document.getElementById('global-secret-updated-at');
    if (lastUpdatedElement) {
        if (settings.googleClientSecretUpdatedAt) {
            const date = new Date(settings.googleClientSecretUpdatedAt);
            lastUpdatedElement.textContent = formatRelativeTime(date);
            lastUpdatedElement.title = date.toLocaleString();
        } else {
            lastUpdatedElement.textContent = '(never)';
        }
    }

    // Display configured status
    const configuredElement = document.getElementById('global-secret-configured');
    if (configuredElement) {
        if (settings.googleClientSecretConfigured) {
            configuredElement.innerHTML = '<span class="badge bg-success">Configured</span>';
        } else {
            configuredElement.innerHTML = '<span class="badge bg-warning">Not Configured</span>';
        }
    }

    // Display SSO Client ID
    const ssoClientIdElement = document.getElementById('global-sso-client-id');
    if (ssoClientIdElement) {
        ssoClientIdElement.textContent = settings.googleSsoClientId || '(not configured)';
    }

    // Display SSO Client ID configured status
    const ssoClientIdConfiguredElement = document.getElementById('global-sso-client-id-configured');
    if (ssoClientIdConfiguredElement) {
        if (settings.googleSsoClientIdConfigured) {
            ssoClientIdConfiguredElement.innerHTML = ' <span class="badge bg-success">Configured</span>';
        } else {
            ssoClientIdConfiguredElement.innerHTML = ' <span class="badge bg-warning">Not Configured</span>';
        }
    }

    // Display SSO Client Secret partial
    const ssoSecretPartialElement = document.getElementById('global-sso-secret-partial');
    if (ssoSecretPartialElement) {
        ssoSecretPartialElement.textContent = settings.googleSsoClientSecretPartial || '(not configured)';
    }

    // Display SSO Client Secret configured status
    const ssoSecretConfiguredElement = document.getElementById('global-sso-secret-configured');
    if (ssoSecretConfiguredElement) {
        if (settings.googleSsoClientSecretConfigured) {
            ssoSecretConfiguredElement.innerHTML = ' <span class="badge bg-success">Configured</span>';
        } else {
            ssoSecretConfiguredElement.innerHTML = ' <span class="badge bg-warning">Not Configured</span>';
        }
    }

    // Display SSO credentials last updated timestamp
    const ssoUpdatedElement = document.getElementById('global-sso-updated-at');
    if (ssoUpdatedElement) {
        if (settings.googleSsoCredentialsUpdatedAt) {
            const date = new Date(settings.googleSsoCredentialsUpdatedAt);
            ssoUpdatedElement.textContent = formatRelativeTime(date);
            ssoUpdatedElement.title = date.toLocaleString();
        } else {
            ssoUpdatedElement.textContent = '(never)';
        }
    }
}

// Save global settings (librarian-only)
async function saveGlobalSettings(event) {
    event.preventDefault();

    // Hide previous messages
    const errorDiv = document.getElementById('global-settings-error');
    const successDiv = document.getElementById('global-settings-success');
    if (errorDiv) errorDiv.style.display = 'none';
    if (successDiv) successDiv.style.display = 'none';

    const newSecret = document.getElementById('global-client-secret').value.trim();

    if (!newSecret) {
        showGlobalSettingsError('Please enter a Client Secret');
        return;
    }

    try {
        const response = await fetch('/api/global-settings', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                googleClientSecret: newSecret
            })
        });

        if (response.ok) {
            const updatedSettings = await response.json();
            displayGlobalSettings(updatedSettings);

            // Clear the input field
            document.getElementById('global-client-secret').value = '';

            // Show success message
            showGlobalSettingsSuccess('Global Client Secret updated successfully!');
        } else if (response.status === 403) {
            showGlobalSettingsError('Permission denied. Only librarians can update global settings.');
        } else {
            showGlobalSettingsError('Failed to update global settings. Please try again.');
        }
    } catch (error) {
        console.error('Error saving global settings:', error);
        showGlobalSettingsError('Error saving global settings. Please try again.');
    }
}

// Show success message
function showGlobalSettingsSuccess(message) {
    const successDiv = document.getElementById('global-settings-success');
    if (successDiv) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
    }
}

// Show error message
function showGlobalSettingsError(message) {
    const errorDiv = document.getElementById('global-settings-error');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

// Save Google SSO settings (librarian-only)
async function saveGlobalSsoSettings(event) {
    event.preventDefault();

    // Hide previous messages
    const errorDiv = document.getElementById('global-sso-settings-error');
    const successDiv = document.getElementById('global-sso-settings-success');
    if (errorDiv) errorDiv.style.display = 'none';
    if (successDiv) successDiv.style.display = 'none';

    const clientId = document.getElementById('global-sso-client-id-input').value.trim();
    const clientSecret = document.getElementById('global-sso-client-secret-input').value.trim();

    if (!clientId && !clientSecret) {
        showGlobalSsoSettingsError('Please enter at least one SSO credential to update');
        return;
    }

    try {
        const body = {};
        if (clientId) body.googleSsoClientId = clientId;
        if (clientSecret) body.googleSsoClientSecret = clientSecret;

        const response = await fetch('/api/global-settings', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify(body)
        });

        if (response.ok) {
            const updatedSettings = await response.json();
            displayGlobalSettings(updatedSettings);

            // Clear the input fields
            document.getElementById('global-sso-client-id-input').value = '';
            document.getElementById('global-sso-client-secret-input').value = '';

            // Show success message
            showGlobalSsoSettingsSuccess('Google SSO credentials updated successfully!');
        } else if (response.status === 403) {
            showGlobalSsoSettingsError('Permission denied. Only librarians can update global settings.');
        } else {
            showGlobalSsoSettingsError('Failed to update SSO credentials. Please try again.');
        }
    } catch (error) {
        console.error('Error saving SSO settings:', error);
        showGlobalSsoSettingsError('Error saving SSO credentials. Please try again.');
    }
}

// Show SSO settings success message
function showGlobalSsoSettingsSuccess(message) {
    const successDiv = document.getElementById('global-sso-settings-success');
    if (successDiv) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
    }
}

// Show SSO settings error message
function showGlobalSsoSettingsError(message) {
    const errorDiv = document.getElementById('global-sso-settings-error');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

// Expose functions globally for HTML onclick handlers
window.saveGlobalSsoSettings = saveGlobalSsoSettings;

// Format relative time (e.g., "5 minutes ago", "2 hours ago")
function formatRelativeTime(date) {
    const now = new Date();
    const diffMs = now - date;
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 60) {
        return 'just now';
    } else if (diffMin < 60) {
        return `${diffMin} minute${diffMin !== 1 ? 's' : ''} ago`;
    } else if (diffHour < 24) {
        return `${diffHour} hour${diffHour !== 1 ? 's' : ''} ago`;
    } else if (diffDay < 30) {
        return `${diffDay} day${diffDay !== 1 ? 's' : ''} ago`;
    } else {
        return date.toLocaleDateString();
    }
}

// Initialize global settings on page load
document.addEventListener('DOMContentLoaded', () => {
    // Only load if we're on a page with global settings section
    if (document.getElementById('global-settings-section')) {
        loadGlobalSettings();
    }
});
