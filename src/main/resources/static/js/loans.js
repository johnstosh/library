// (c) Copyright 2025 by Muczynski

async function loadLoansSection() {
    console.log('[Loans] Loading loans section - calling loadLoans() and populateLoanDropdowns()');
    await loadLoans();
    await populateLoanDropdowns();
    console.log('[Loans] Loans section fully loaded');
}

async function loadLoans() {
    console.log('[Loans] loadLoans() called');
    try {
        const showAll = document.getElementById('show-returned-loans').checked;
        const loans = await fetchData(`/api/loans?showAll=${showAll}`);
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

            const titleCell = document.createElement('td');
            titleCell.setAttribute('data-test', 'loan-book-title');
            titleCell.textContent = bookMap.get(loan.bookId) || 'Unknown Book';
            row.appendChild(titleCell);

            const userCell = document.createElement('td');
            userCell.setAttribute('data-test', 'loan-user');
            userCell.textContent = userMap.get(loan.userId) || 'Unknown';
            row.appendChild(userCell);

            const loanDateCell = document.createElement('td');
            loanDateCell.setAttribute('data-test', 'loan-date');
            loanDateCell.textContent = formatDate(loan.loanDate);
            row.appendChild(loanDateCell);

            const dueDateCell = document.createElement('td');
            dueDateCell.setAttribute('data-test', 'loan-due-date');
            dueDateCell.textContent = formatDate(loan.dueDate);
            row.appendChild(dueDateCell);

            const returnDateCell = document.createElement('td');
            returnDateCell.setAttribute('data-test', 'loan-return-date');
            returnDateCell.textContent = loan.returnDate ? formatDate(loan.returnDate) : 'Not returned';
            row.appendChild(returnDateCell);

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
    const dueDate = document.getElementById('due-date').value;
    const returnDate = document.getElementById('return-date').value;
    if (!bookId || !userId) {
        showError('loans', 'Book and name are required.');
        return;
    }
    try {
        await postData('/api/loans/checkout', { bookId, userId, loanDate: loanDate || null, dueDate: dueDate || null, returnDate: returnDate || null });
        document.getElementById('loan-book').selectedIndex = 0;
        document.getElementById('loan-user').selectedIndex = 0;
        document.getElementById('loan-date').value = '';
        document.getElementById('due-date').value = '';
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
    document.getElementById('due-date').value = data.dueDate || '';
    document.getElementById('return-date').value = data.returnDate || '';
    const btn = document.getElementById('checkout-btn');
    btn.textContent = 'Update Loan';
    btn.onclick = () => updateLoan(id);
}

async function updateLoan(id) {
    const bookId = document.getElementById('loan-book').value;
    const userId = document.getElementById('loan-user').value;
    const loanDate = document.getElementById('loan-date').value;
    const dueDate = document.getElementById('due-date').value;
    const returnDate = document.getElementById('return-date').value;
    if (!bookId || !userId) {
        showError('loans', 'Book and name are required.');
        return;
    }
    try {
        await putData(`/api/loans/${id}`, { bookId, userId, loanDate: loanDate || null, dueDate: dueDate || null, returnDate: returnDate || null });
        document.getElementById('loan-book').selectedIndex = 0;
        document.getElementById('loan-user').selectedIndex = 0;
        document.getElementById('loan-date').value = '';
        document.getElementById('due-date').value = '';
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
    alert(`Loan Details:\nID: ${loan.id}\nBook: ${loan.bookTitle}\nName: ${loan.userName}\nLoan Date: ${formatDate(loan.loanDate)}\nDue Date: ${formatDate(loan.dueDate)}\n${returnStatus}`);
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
    console.log('[Loans] populateLoanDropdowns() called');
    try {
        const books = await fetchData('/api/books');
        console.log(`[Loans] Fetched ${books.length} books`);
        const bookSelect = document.getElementById('loan-book');
        if (!bookSelect) {
            console.error('[Loans] loan-book select not found in DOM!');
            return;
        }
        bookSelect.innerHTML = '';
        books.forEach(book => {
            const option = document.createElement('option');
            option.value = book.id;
            option.textContent = book.title;
            bookSelect.appendChild(option);
        });
        console.log(`[Loans] Populated loan-book dropdown with ${books.length} options`);

        const users = await fetchData('/api/users');
        console.log(`[Loans] Fetched ${users.length} users`);
        const userSelect = document.getElementById('loan-user');
        if (!userSelect) {
            console.error('[Loans] loan-user select not found in DOM!');
            return;
        }
        userSelect.innerHTML = '';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.username;
            userSelect.appendChild(option);
        });
        console.log(`[Loans] Populated loan-user dropdown with ${users.length} options`);

        const loanDateInput = document.getElementById('loan-date');
        const dueDateInput = document.getElementById('due-date');
        if (!loanDateInput || !dueDateInput) {
            console.error('[Loans] Date inputs not found in DOM!');
            return;
        }

        const today = new Date();
        loanDateInput.value = today.toISOString().split('T')[0];

        const twoWeeksFromNow = new Date(today);
        twoWeeksFromNow.setDate(today.getDate() + 14);
        dueDateInput.value = twoWeeksFromNow.toISOString().split('T')[0];

        console.log(`[Loans] Set loan-date to: ${loanDateInput.value}, due-date to: ${dueDateInput.value}`);
        console.log('[Loans] populateLoanDropdowns() completed successfully');
    } catch (error) {
        console.error('[Loans] Error in populateLoanDropdowns():', error);
        showError('loans', 'Failed to populate dropdowns: ' + error.message);
    }
}
