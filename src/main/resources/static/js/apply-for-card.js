// (c) Copyright 2025 by Muczynski
function loadApplications() {
    const applicationsTableBody = document.getElementById('applied-list-body');
    if (!applicationsTableBody) {
        return;
    }
    fetch('/apply/api')
        .then(response => response.json())
        .then(data => {
            applicationsTableBody.innerHTML = ''; // Clear existing data
            data.forEach(application => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${application.id}</td>
                    <td>${application.name}</td>
                    <td>
                        <select data-id="${application.id}" class="status-select">
                            <option value="pending" ${application.status === 'pending' ? 'selected' : ''}>Pending</option>
                            <option value="approved" ${application.status === 'approved' ? 'selected' : ''}>Approved</option>
                            <option value="not-approved" ${application.status === 'not-approved' ? 'selected' : ''}>Not Approved</option>
                            <option value="question" ${application.status === 'question' ? 'selected' : ''}>Question</option>
                        </select>
                    </td>
                    <td>
                        <button data-id="${application.id}" class="btn btn-danger delete-btn">Delete</button>
                    </td>
                `;
                applicationsTableBody.appendChild(row);
            });
        });
}

document.addEventListener('DOMContentLoaded', function () {
    const appliedSection = document.getElementById('applied-section');

    fetch('/api/users/me')
        .then(response => {
            if (!response.ok) {
                return Promise.reject('Not logged in');
            }
            return response.json();
        })
        .then(user => {
            if (user.roles.some(role => role.name === 'LIBRARIAN')) {
                if (appliedSection) {
                    // The '.librarian-only' class in app.js handles visibility.
                    // This explicit style change is not needed and can cause race conditions.
                }
                loadApplications();
            }
        })
        .catch(error => {
            console.log('User is not a librarian or not logged in.');
        });

    if (appliedSection) {
        appliedSection.addEventListener('change', function (event) {
            if (event.target.classList.contains('status-select')) {
                const id = event.target.dataset.id;
                const status = event.target.value;
                fetch(`/apply/api/${id}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ status })
                });
            }
        });

        appliedSection.addEventListener('click', function (event) {
            if (event.target.classList.contains('delete-btn')) {
                const id = event.target.dataset.id;
                fetch(`/apply/api/${id}`, {
                    method: 'DELETE'
                }).then(() => {
                    event.target.closest('tr').remove();
                });
            }
        });
    }
});

async function applyForCard() {
    const username = document.getElementById('new-applicant-name').value.trim();
    const password = document.getElementById('new-applicant-password').value;
    if (!username || !password) {
        showApplyError('Please fill in all fields.');
        return;
    }
    try {
        await postData('/api/public/register', { username, password }, false, false);
        document.getElementById('new-applicant-name').value = '';
        document.getElementById('new-applicant-password').value = '';
        showApplySuccess('Library card application successful.');
        clearApplyError();
        const appliedSection = document.getElementById('applied-section');
        if (appliedSection && appliedSection.style.display === 'block') {
            loadApplications();
        }
    } catch (error) {
        showApplyError(error.message);
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