async function generateTestData() {
    const numBooks = document.getElementById('num-books').value;
    if (!numBooks || numBooks <= 0) {
        alert('Please enter a valid number of books to generate.');
        return;
    }

    try {
        const response = await fetch('/api/test-data/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify({ numBooks: numBooks })
        });

        if (response.ok) {
            alert('Test data generated successfully!');
        } else {
            const error = await response.text();
            alert(`Error generating test data: ${error}`);
        }
    } catch (error) {
        console.error('Error generating test data:', error);
        alert('An error occurred while generating test data.');
    }
}