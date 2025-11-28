// (c) Copyright 2025 by Muczynski


async function loadApplied() {
    if (!isLibrarian) return;
    try {
        const applications = await fetchData('/api/applied');
        const appliedListBody = document.getElementById('applied-list-body');
        if (!appliedListBody) return; // Guard against missing DOM element
        appliedListBody.innerHTML = '';
        if (!applications || !Array.isArray(applications)) {
            console.warn('No applications data or invalid format');
            return;
        }
        applications.forEach(app => {
            if (!app || typeof app !== 'object') return; // Skip invalid app objects
            if (!app.id) {
                console.warn('Skipping application without id:', app);
                return;
            }
            const status = app.status || 'PENDING'; // Default to PENDING if null/undefined
            const name = app.name || ''; // Avoid rendering "null"
            const id = app.id; // Use directly as number; assume valid from check above
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${name}</td>
                <td>
                    <select class="form-select" onchange="updateAppliedStatus(${id}, this.value)">
                        <option value="PENDING" ${status === 'PENDING' ? 'selected' : ''}>Pending</option>
                        <option value="APPROVED" ${status === 'APPROVED' ? 'selected' : ''}>Approved</option>
                        <option value="NOT_APPROVED" ${status === 'NOT_APPROVED' ? 'selected' : ''}>Not Approved</option>
                        <option value="QUESTION" ${status === 'QUESTION' ? 'selected' : ''}>Question</option>
                    </select>
                </td>
                <td>
                    <button class="btn btn-success btn-sm" onclick="approveApplication(${id})">✔️</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteApplied(${id})">🗑️</button>
                </td>
            `;
            appliedListBody.appendChild(row);
        });
    } catch (error) {
        console.error('Failed to load applications:', error);
        showError('library-card', 'Failed to load applications.');
    }
}

async function approveApplication(id) {
    if (!confirm('Are you sure you want to approve this application?')) {
        return;
    }
    try {
        await postData(`/api/applied/${id}/approve`, {});
        await loadApplied(); // Add await for consistency
    } catch (error) {
        console.error(`Failed to approve application ${id}:`, error);
        showError('library-card', `Failed to approve application ${id}.`);
    }
}

async function updateAppliedStatus(id, status) {
    if (!status) return; // Guard against empty status
    try {
        await putData(`/api/applied/${id}`, { status: status.trim() });
        await loadApplied(); // Add await for consistency
    } catch (error) {
        console.error(`Failed to update application ${id}:`, error);
        showError('library-card', `Failed to update application ${id}.`);
    }
}

async function deleteApplied(id) {
    if (!confirm('Are you sure you want to delete this application?')) {
        return;
    }
    try {
        await deleteData(`/api/applied/${id}`);
        await loadApplied(); // Add await for consistency
    } catch (error) {
        console.error(`Failed to delete application ${id}:`, error);
        showError('library-card', `Failed to delete application ${id}.`);
    }
}

async function applyForCard() {
    const nameElement = document.getElementById('new-applicant-name');
    const passwordElement = document.getElementById('new-applicant-password');
    if (!nameElement || !passwordElement) {
        showApplyError('Form elements not found. Please refresh the page.');
        return;
    }
    const username = nameElement.value.trim();
    const password = passwordElement.value;
    if (!username || !password) {
        showApplyError('Please fill in all fields.');
        return;
    }

    const btn = document.getElementById('apply-for-card-btn');
    showButtonSpinner(btn, 'Applying...');

    try {
        const hashedPassword = await hashPassword(password);
        await postData('/api/public/register', { username, password: hashedPassword }, false, false);
        nameElement.value = '';
        passwordElement.value = '';
        showApplySuccess('Library card application successful.');
        clearApplyError();
        // Safely check isLibrarian (fallback to false if undefined)
        if (typeof isLibrarian !== 'undefined' && isLibrarian) {
            await loadApplied();
        }
    } catch (error) {
        showApplyError(error.message || 'An unknown error occurred.');
    } finally {
        hideButtonSpinner(btn, 'Apply');
    }
}

function showApplyError(message) {
    const errorEl = document.getElementById('apply-error');
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }
}

function clearApplyError() {
    const errorEl = document.getElementById('apply-error');
    if (errorEl) {
        errorEl.style.display = 'none';
    }
}

function showApplySuccess(message) {
    const successEl = document.getElementById('apply-success');
    if (successEl) {
        successEl.textContent = message;
        successEl.style.display = 'block';
    }
}

/**
 * Load library card design preference and show print section if user is logged in
 *
 * IMPORTANT: This function MUST show the print-card-section for logged-in users.
 * The print section allows users to select a card design and print their library card.
 * It should be visible for ALL logged-in users (both SSO and traditional login).
 * The apply-for-card section at the bottom is always visible for anyone who wants to apply.
 */
async function loadLibraryCardSection() {
    const printSection = document.getElementById('print-card-section');

    try {
        // Check if user is logged in by fetching user settings
        // This works for both SSO users and traditional login users
        const userSettings = await fetchData('/api/user-settings', { suppress401Redirect: true });

        if (userSettings) {
            // User is logged in - MUST show the print section
            // This is the Library Card design selector that allows users to:
            // 1. Select their preferred card design
            // 2. Save their design preference
            // 3. Print their library card as PDF
            if (printSection) {
                printSection.style.display = 'block';
            }

            // Load and select the user's saved card design preference
            const currentDesign = userSettings.libraryCardDesign || 'CLASSICAL_DEVOTION';
            const radioButton = document.getElementById(`design-${currentDesign.toLowerCase().replace(/_/g, '-')}`);
            if (radioButton) {
                radioButton.checked = true;
            }
        } else {
            // No user settings returned - hide print section
            if (printSection) {
                printSection.style.display = 'none';
            }
        }
    } catch (error) {
        // Error fetching user settings - user is not logged in or there was an API error
        // Hide print section and only show apply section
        console.log('Could not load user settings:', error.message || error);
        if (printSection) {
            printSection.style.display = 'none';
        }
    }
}

/**
 * Save the selected library card design
 */
async function saveCardDesign() {
    const btn = document.getElementById('save-card-design-btn');

    try {
        clearCardDesignMessages();

        // Get selected design
        const selectedRadio = document.querySelector('input[name="card-design"]:checked');
        if (!selectedRadio) {
            showCardDesignError('Please select a card design first.');
            return;
        }

        const design = selectedRadio.value;

        showButtonSpinner(btn, 'Saving...');

        // Save to user settings
        await putData('/api/user-settings', { libraryCardDesign: design });

        showCardDesignSuccess('Card design saved successfully!');

        // Also update the settings dropdown if it exists
        const settingsDropdown = document.getElementById('settings-card-design');
        if (settingsDropdown) {
            settingsDropdown.value = design;
        }
    } catch (error) {
        showCardDesignError('Failed to save card design: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Save Design');
    }
}

/**
 * Print library card as PDF
 */
async function printLibraryCard() {
    try {
        clearCardDesignMessages();

        // Open PDF in new window
        window.open('/api/library-card/print', '_blank');

        showCardDesignSuccess('Opening your library card PDF...');
    } catch (error) {
        showCardDesignError('Failed to generate library card: ' + error.message);
    }
}

function showCardDesignError(message) {
    const errorEl = document.getElementById('card-design-error');
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }
}

function showCardDesignSuccess(message) {
    const successEl = document.getElementById('card-design-success');
    if (successEl) {
        successEl.textContent = message;
        successEl.style.display = 'block';
    }
}

function clearCardDesignMessages() {
    const errorEl = document.getElementById('card-design-error');
    const successEl = document.getElementById('card-design-success');
    if (errorEl) {
        errorEl.style.display = 'none';
    }
    if (successEl) {
        successEl.style.display = 'none';
    }
}

// Expose functions globally for HTML onclick handlers and sections.js
window.loadApplied = loadApplied;
window.applyForCard = applyForCard;
window.approveApplication = approveApplication;
window.updateAppliedStatus = updateAppliedStatus;
window.deleteApplied = deleteApplied;
window.loadLibraryCardSection = loadLibraryCardSection;
window.saveCardDesign = saveCardDesign;
window.printLibraryCard = printLibraryCard;
