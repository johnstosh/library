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
    try {
        await postData('/api/public/register', { username, password }, false, false);
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
