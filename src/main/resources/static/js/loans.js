async function loadLoans() {
    try {
        const loans = await fetchData('/api/loans');
        const list = document.getElementById('loan-list-body');
        list.innerHTML = '';

        // Fetch all books and users once for efficiency
        const books = await fetchData('/api/books');
        const bookMap = new Map(books.map(book => [book.id, book.title]));
        const users = await fetchData('/api/users');
        const userMap = new Map(users.map(user => [user.id, user.username]));

        for (const loan of loans) {
            const row = document.createElement('tr');
            row.setAttribute('data-test', 'loan-item');
            row.setAttribute('data-entity-id', loan.id);

            const loanCell = document.createElement('td');
            loanCell.setAttribute('data-test', 'loan-details');
            const bookTitle = bookMap.get(loan.bookId) || 'Unknown Book';
            const userName = userMap.get(loan.userId) || 'Unknown User';
            loanCell.textContent = `${bookTitle} loaned to ${userName} on ${formatDate(loan.loanDate)}`;
            if (loan.returnDate) {
                loanCell.textContent += ` (returned on ${formatDate(loan.returnDate)})`;
            }
            row.appendChild(loanCell);

            const dueDateCell = document.createElement('td');
            dueDateCell.setAttribute('data-test', 'loan-due-date');
            dueDateCell.textContent = formatDate(loan.dueDate);
            row.appendChild(dueDateCell);

            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-test', 'loan-actions');

            if (!loan.returnDate) {
                const returnButton = document.createElement('button');
                returnButton.setAttribute('data-test', 'return-book-btn');
                returnButton.setAttribute('data-loan-id', loan.id);
                returnButton.textContent = 'Return';
                returnButton.className = 'return-btn';
                returnButton.onclick = () => returnBook(loan.id);
                actionsCell.appendChild(returnButton);
            }

            const viewBtn = document.createElement('button');
            viewBtn.setAttribute('data-test', 'view-loan-btn');
            viewBtn.textContent = '🔍';
            viewBtn.title = 'View details';
            viewBtn.onclick = () => viewLoan(loan.id);
            actionsCell.appendChild(viewBtn);

            const editBtn = document.createElement('button');
            editBtn.setAttribute('data-test', 'edit-loan-btn');
            editBtn.textContent = '✏️';
            editBtn.title = 'Edit';
            editBtn.onclick = () => editLoan(loan.id);
            actionsCell.appendChild(editBtn);

            const delBtn = document.createElement('button');
            delBtn.setAttribute('data-test', 'delete-loan-btn');
            delBtn.textContent = '🗑️';
            delBtn.title = 'Delete';
            delBtn.onclick = () => deleteLoan(loan.id);
            actionsCell.appendChild(delBtn);
            row.appendChild(actionsCell);

            list.appendChild(row);
        }
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to load loans: ' + error.message);
    }
}

async function checkoutBook() {
    const bookId = document.getElementById('loan-book').value;
    const userId = document.getElementById('loan-user').value;
    const loanDate = document.getElementById('loan-date').value;
    const returnDate = document.getElementById('return-date').value;
    if (!bookId || !userId) {
        showError('loans', 'Book and user are required.');
        return;
    }
    try {
        await postData('/api/loans/checkout', { bookId, userId, loanDate: loanDate || null, returnDate: returnDate || null });
        document.getElementById('loan-book').selectedIndex = 0;
        document.getElementById('loan-user').selectedIndex = 0;
        document.getElementById('loan-date').value = '';
        document.getElementById('return-date').value = '';
        await loadLoans();
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to checkout book: ' + error.message);
    }
}

async function editLoan(id) {
    const data = await fetchData(`/api/loans/${id}`);
    document.getElementById('loan-book').value = data.bookId || '';
    document.getElementById('loan-user').value = data.userId || '';
    document.getElementById('loan-date').value = data.loanDate || '';
    document.getElementById('return-date').value = data.returnDate || '';
    const btn = document.getElementById('checkout-btn');
    btn.textContent = 'Update Loan';
    btn.onclick = () => updateLoan(id);
}

async function updateLoan(id) {
    const bookId = document.getElementById('loan-book').value;
    const userId = document.getElementById('loan-user').value;
    const loanDate = document.getElementById('loan-date').value;
    const returnDate = document.getElementById('return-date').value;
    if (!bookId || !userId) {
        showError('loans', 'Book and user are required.');
        return;
    }
    try {
        await putData(`/api/loans/${id}`, { bookId, userId, loanDate: loanDate || null, returnDate: returnDate || null });
        document.getElementById('loan-book').selectedIndex = 0;
        document.getElementById('loan-user').selectedIndex = 0;
        document.getElementById('loan-date').value = '';
        document.getElementById('return-date').value = '';
        await loadLoans();
        const btn = document.getElementById('checkout-btn');
        btn.textContent = 'Checkout Book';
        btn.onclick = checkoutBook;
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to update loan: ' + error.message);
    }
}

async function deleteLoan(id) {
    if (!confirm('Are you sure you want to delete this loan?')) return;
    try {
        await deleteData(`/api/loans/${id}`);
        await loadLoans();
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to delete loan: ' + error.message);
    }
}

async function viewLoan(id) {
    const loan = await fetchData(`/api/loans/${id}`);
    const returnStatus = loan.returnDate ? `Returned on ${formatDate(loan.returnDate)}` : 'Not returned';
    alert(`Loan Details:\nID: ${loan.id}\nBook: ${loan.bookTitle}\nUser: ${loan.userName}\nLoan Date: ${formatDate(loan.loanDate)}\nDue Date: ${formatDate(loan.dueDate)}\n${returnStatus}`);
}

async function returnBook(loanId) {
    try {
        await putData(`/api/loans/return/${loanId}`);
        await loadLoans();
        clearError('loans');
    } catch (error) {
        showError('loans', 'Failed to return book: ' + error.message);
    }
}

async function populateLoanDropdowns() {
    try {
        const books = await fetchData('/api/books');
        const bookSelect = document.getElementById('loan-book');
        bookSelect.innerHTML = '';
        books.forEach(book => {
            const option = document.createElement('option');
            option.value = book.id;
            option.textContent = book.title;
            bookSelect.appendChild(option);
        });

        const users = await fetchData('/api/users');
        const userSelect = document.getElementById('loan-user');
        userSelect.innerHTML = '';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.username;
            userSelect.appendChild(option);
        });
    } catch (error) {
        showError('loans', 'Failed to populate dropdowns: ' + error.message);
    }
}

populateLoanDropdowns();
