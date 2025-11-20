// (c) Copyright 2025 by Muczynski - Utilities Module

let isLibrarian = false; // Global variable, accessible via window.isLibrarian

export function getCookie(name) {
    let cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.substring(0, name.length + 1) === (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

export async function fetchData(url, options = {}) {
    const { suppress401Redirect = false, method = 'GET', body = null } = options;
    const token = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    const fetchOptions = {
        method: method,
        headers: headers
    };

    // Only include body for POST, PUT, PATCH methods
    if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        fetchOptions.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(url, fetchOptions);

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            if (!suppress401Redirect) {
                window.location.href = '/';
            }
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            let errorMsg = `HTTP error status: ${response.status}`;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                try {
                    const errorJson = await response.json();
                    errorMsg = errorJson.error || errorMsg;
                } catch (e) {
                    console.error('Could not parse JSON error response', e);
                    const errorText = await response.text();
                    errorMsg = errorText || errorMsg;
                }
            } else {
                const errorText = await response.text();
                errorMsg = errorText || errorMsg;
            }
            throw new Error(errorMsg);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else {
            return null;
        }
    } catch (error) {
        console.error('Fetch error:', error);
        throw error;
    }
}

export async function postData(url, data, isFormData = false, includeCsrf = true) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {};
    if (!isFormData) {
        headers['Content-Type'] = 'application/json';
    }
    if (includeCsrf && token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: headers,
            body: isFormData ? data : JSON.stringify(data)
        });

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            window.location.href = '/';
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            let errorMsg = `HTTP error status: ${response.status}`;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                try {
                    const errorJson = await response.json();
                    errorMsg = errorJson.error || errorMsg;
                } catch (e) {
                    console.error('Could not parse JSON error response', e);
                    const errorText = await response.text();
                    errorMsg = errorText || errorMsg;
                }
            } else {
                const errorText = await response.text();
                errorMsg = errorText || errorMsg;
            }
            throw new Error(errorMsg);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else {
            return null;
        }
    } catch (error) {
        console.error('Post error:', error);
        throw error;
    }
}

export async function putData(url, data, includeCsrf = true) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json',
    };
    if (includeCsrf && token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    try {
        const response = await fetch(url, {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify(data)
        });

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            window.location.href = '/';
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            let errorMsg = `HTTP error status: ${response.status}`;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                try {
                    const errorJson = await response.json();
                    errorMsg = errorJson.error || errorMsg;
                } catch (e) {
                    console.error('Could not parse JSON error response', e);
                    const errorText = await response.text();
                    errorMsg = errorText || errorMsg;
                }
            } else {
                const errorText = await response.text();
                errorMsg = errorText || errorMsg;
            }
            throw new Error(errorMsg);
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else {
            return null;
        }
    } catch (error) {
        console.error('Put error:', error);
        throw error;
    }
}

export async function deleteData(url) {
    const token = getCookie('XSRF-TOKEN');
    const headers = {};
    if (token) {
        headers['X-CSRF-TOKEN'] = token;
    }

    try {
        const response = await fetch(url, {
            method: 'DELETE',
            headers: headers
        });

        if (response.status === 401) {
            console.log('401 Unauthorized - redirecting to login');
            window.location.href = '/';
            throw new Error('Unauthorized - redirecting to login');
        } else if (response.status === 403) {
            let errorMsg = 'Forbidden';
            try {
                const errorText = await response.text();
                errorMsg = errorText || 'Access forbidden';
            } catch (e) {
                console.error('Could not read 403 error message', e);
            }
            // Create a new page for 403 error
            const errorPage = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>403 Forbidden</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
                </head>
                <body>
                    <div class="container mt-5">
                        <div class="row justify-content-center">
                            <div class="col-md-6">
                                <div class="card">
                                    <div class="card-body text-center">
                                        <h1 class="text-danger">403 Forbidden</h1>
                                        <p class="card-text">${errorMsg}</p>
                                        <a href="/" class="btn btn-primary">Go to Login</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `;
            const errorBlob = new Blob([errorPage], { type: 'text/html' });
            const errorUrl = URL.createObjectURL(errorBlob);
            window.location.href = errorUrl;
            throw new Error('403 Forbidden - see error page');
        }

        if (!response.ok) {
            let errorMsg = `HTTP error status: ${response.status}`;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                try {
                    const errorJson = await response.json();
                    errorMsg = errorJson.error || errorMsg;
                } catch (e) {
                    console.error('Could not parse JSON error response', e);
                    const errorText = await response.text();
                    errorMsg = errorText || errorMsg;
                }
            } else {
                const errorText = await response.text();
                errorMsg = errorText || errorMsg;
            }
            throw new Error(errorMsg);
        }

        return response.ok;
    } catch (error) {
        console.error('Delete error:', error);
        throw error;
    }
}

export function showError(sectionId, message) {
    let errorDiv = document.querySelector(`#${sectionId}-section [data-test="form-error"]`);
    if (!errorDiv) {
        errorDiv = document.createElement('div');
        errorDiv.setAttribute('data-test', 'form-error');
        errorDiv.style.color = 'red';
        errorDiv.style.display = 'block';
        const section = document.getElementById(`${sectionId}-section`);
        if (section) {
            section.insertBefore(errorDiv, section.firstChild.nextSibling); // After h2
        }
    }
    errorDiv.textContent = message;
    // Scroll error into view for better visibility
    errorDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

export function clearError(sectionId) {
    const errorDiv = document.querySelector(`#${sectionId}-section [data-test="form-error"]`);
    if (errorDiv) {
        errorDiv.remove();
    }
}

export function showBulkSuccess(textareaId) {
    let successDiv = document.querySelector(`#${textareaId} + [data-test="bulk-import-success"]`);
    if (!successDiv) {
        successDiv = document.createElement('div');
        successDiv.setAttribute('data-test', 'bulk-import-success');
        successDiv.style.color = 'green';
        successDiv.textContent = 'Bulk import successful.';
        successDiv.style.display = 'block';
    }
    const textarea = document.getElementById(textareaId);
    if (textarea) {
        textarea.insertAdjacentElement('afterend', successDiv);
    }
    setTimeout(() => {
        if (successDiv) successDiv.remove();
    }, 3000);
}

export function formatDate(dateString) {
    if (!dateString) return '';

    // The dateString from the backend is 'YYYY-MM-DD'.
    // `new Date('YYYY-MM-DD')` creates a date at midnight UTC.
    // In timezones behind UTC, this can result in the previous day.
    // To fix this, we parse the string and create the date in the local timezone.
    const parts = dateString.split('T')[0].split('-');
    const year = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed in JS
    const day = parseInt(parts[2], 10);
    const date = new Date(year, month, day);

    const displayMonth = String(date.getMonth() + 1).padStart(2, '0');
    const displayDay = String(date.getDate()).padStart(2, '0');
    const displayYear = date.getFullYear();
    return `${displayMonth}/${displayDay}/${displayYear}`;
}

export function shouldResetForSection(sectionId) {
    return sectionId !== 'books' && sectionId !== 'authors' || window.isLibrarian;
}

/**
 * Hash a password using SHA-256 before sending to server
 * This prevents plaintext password transmission and eliminates BCrypt's 72-byte limit
 * @param {string} password - The plaintext password
 * @returns {Promise<string>} - The SHA-256 hex hash of the password
 */
export async function hashPassword(password) {
    if (!password) {
        return password; // Return empty/null as-is
    }
    const encoder = new TextEncoder();
    const data = encoder.encode(password);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    return hashHex;
}

/**
 * Formats a LOC call number for display on a book spine label.
 *
 * Each component of the call number is placed on its own line because
 * library book spine labels have limited horizontal space. Breaking up
 * the call number makes it easier to read vertically on the spine.
 *
 * Examples:
 *   Input:  "BX 4705.M124 A77 2005"
 *   Output: "BX<br>4705<br>.M124<br>A77<br>2005"
 *
 *   Input:  "PN1009.5.C45 O27 1998"
 *   Output: "PN<br>1009.5<br>.C45<br>O27<br>1998"
 *
 * The parsing rules are:
 * 1. Split by spaces to get major components
 * 2. For the FIRST component only, if it starts with letters followed by digits
 *    (e.g., "PN1009"), split into the letter prefix and the rest (e.g., "PN", "1009")
 * 3. For components containing a period followed by a LETTER (e.g., "4705.M124"),
 *    split at that period keeping the period with the second part (e.g., "4705", ".M124")
 * 4. Periods followed by digits are NOT split (e.g., "1009.5" stays together)
 *
 * @param {string} locNumber - The LOC call number to format
 * @returns {string} The formatted call number with HTML <br> tags between components
 */
export function formatLocForSpine(locNumber) {
    if (!locNumber || locNumber.trim() === '') {
        return '';
    }

    const parts = [];
    const spaceParts = locNumber.trim().split(/\s+/);

    for (let componentIndex = 0; componentIndex < spaceParts.length; componentIndex++) {
        const part = spaceParts[componentIndex];

        // Only for the FIRST component: check if it starts with letters followed by digits
        // This handles cases where class letters and numbers are not separated by space
        if (componentIndex === 0 && part.length > 1 && /^[a-zA-Z]/.test(part.charAt(0))) {
            let firstDigitIndex = -1;
            for (let i = 0; i < part.length; i++) {
                if (/\d/.test(part.charAt(i))) {
                    firstDigitIndex = i;
                    break;
                }
            }

            // If we found digits after letters, split there
            if (firstDigitIndex > 0) {
                const letterPart = part.substring(0, firstDigitIndex);
                const restPart = part.substring(firstDigitIndex);

                // Add the letter part
                parts.push(letterPart);

                // Now process the rest for periods followed by letters
                splitAtLetterPeriods(restPart, parts);
                continue;
            }
        }

        // Process for periods followed by letters
        splitAtLetterPeriods(part, parts);
    }

    return parts.join('<br>');
}

/**
 * Splits a string at periods that are followed by letters, adding results to parts array.
 * Periods followed by digits are NOT split points (e.g., "1009.5" stays together).
 */
function splitAtLetterPeriods(part, parts) {
    if (!part || part.length === 0) {
        return;
    }

    // Find all period positions where the period is followed by a letter
    let startIndex = 0;
    for (let i = 0; i < part.length; i++) {
        if (part.charAt(i) === '.' && i + 1 < part.length && /[a-zA-Z]/.test(part.charAt(i + 1))) {
            // Found a period followed by a letter - split here
            if (i > startIndex) {
                parts.push(part.substring(startIndex, i));
            }
            startIndex = i; // Start next part from the period
        }
    }

    // Add the remaining part
    if (startIndex < part.length) {
        parts.push(part.substring(startIndex));
    }
}

// Expose formatLocForSpine globally for non-module scripts
window.formatLocForSpine = formatLocForSpine;

/**
 * Shows a spinner on a button and disables it during async operations.
 * Stores the original button text and disabled state for later restoration.
 * @param {HTMLButtonElement|string} button - The button element or button ID
 * @param {string} spinnerText - Optional text to show next to spinner (e.g., "Loading...")
 */
export function showButtonSpinner(button, spinnerText = null) {
    const btn = typeof button === 'string' ? document.getElementById(button) : button;
    if (!btn) return;

    // Store original state if not already stored
    if (!btn.dataset.originalText) {
        btn.dataset.originalText = btn.textContent || btn.innerHTML;
    }
    if (!btn.dataset.originalDisabled) {
        btn.dataset.originalDisabled = btn.disabled ? 'true' : 'false';
    }

    // Create spinner element
    const spinner = document.createElement('span');
    spinner.className = 'spinner-border spinner-border-sm me-1';
    spinner.setAttribute('role', 'status');
    spinner.setAttribute('aria-hidden', 'true');

    // Clear button content and add spinner
    btn.innerHTML = '';
    btn.appendChild(spinner);
    if (spinnerText) {
        btn.appendChild(document.createTextNode(spinnerText));
    }

    // Disable the button
    btn.disabled = true;
}

/**
 * Hides the spinner on a button and restores it to its original state.
 * @param {HTMLButtonElement|string} button - The button element or button ID
 * @param {string} text - Optional text to set (if not provided, uses stored original text)
 */
export function hideButtonSpinner(button, text = null) {
    const btn = typeof button === 'string' ? document.getElementById(button) : button;
    if (!btn) return;

    // Restore original text
    const originalText = text || btn.dataset.originalText || '';
    btn.innerHTML = originalText;

    // Restore original disabled state
    const wasDisabled = btn.dataset.originalDisabled === 'true';
    btn.disabled = wasDisabled;

    // Clean up stored data
    delete btn.dataset.originalText;
    delete btn.dataset.originalDisabled;
}
