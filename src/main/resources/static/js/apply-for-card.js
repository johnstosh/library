document.addEventListener('DOMContentLoaded', function () {
    const applicationsTable = document.getElementById('applications-table');
    const librarianSection = document.getElementById('librarian-section');

    fetch('/api/users/me')
        .then(response => {
            if (!response.ok) {
                return Promise.reject('Not logged in');
            }
            return response.json();
        })
        .then(user => {
            if (user.roles.some(role => role.name === 'LIBRARIAN')) {
                librarianSection.style.display = 'block';
                fetch('/apply/api')
                    .then(response => response.json())
                    .then(data => {
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
                            applicationsTable.appendChild(row);
                        });
                    });
            }
        })
        .catch(error => {
            console.log('User is not a librarian or not logged in.');
        });

    applicationsTable.addEventListener('change', function (event) {
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

    applicationsTable.addEventListener('click', function (event) {
        if (event.target.classList.contains('delete-btn')) {
            const id = event.target.dataset.id;
            fetch(`/apply/api/${id}`, {
                method: 'DELETE'
            }).then(() => {
                event.target.closest('tr').remove();
            });
        }
    });
});