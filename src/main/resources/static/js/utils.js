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
    if (['authors', 'books', 'users'].includes(sectionId)) {
        window.scrollTo(0, 0);
    }
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
