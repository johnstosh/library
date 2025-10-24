import re
from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    try:
        page.goto("http://localhost:8080")

        # Click login button to reveal form
        page.click('button[data-test="menu-login"]')
        expect(page.locator('[data-test="login-form"]')).to_be_visible()

        # Login as librarian
        page.fill('input[name="username"]', 'librarian')
        page.fill('input[name="password"]', 'divinemercy')
        page.click('button[type="submit"]')
        expect(page.locator('button[data-test="menu-logout"]')).to_be_visible()

        # Navigate to books section
        page.click('button[data-test="menu-books"]')
        expect(page.locator('[data-test="book-table"]')).to_be_visible()

        # Add a library
        page.click('button[data-test="menu-libraries"]')
        page.fill('input[data-test="new-library-name"]', 'Test Library')
        page.fill('input[data-test="new-library-hostname"]', 'testhost')
        page.click('button[data-test="add-library-btn"]')
        expect(page.locator('[data-test="library-name"]')).to_contain_text('Test Library')

        # Add an author
        page.click('button[data-test="menu-authors"]')
        page.fill('input[data-test="new-author-name"]', 'Test Author')
        page.click('button[data-test="add-author-btn"]')
        page.wait_for_timeout(1000) # wait for UI to update
        expect(page.locator('[data-test="author-list-body"]')).to_contain_text('Test Author')

        # Navigate back to books section
        page.click('button[data-test="menu-books"]')

        # Add a book to ensure one exists
        page.fill('#new-book-title', 'Test Book')
        page.select_option('#book-author', label='Test Author')
        page.select_option('#book-library', label='Test Library')
        page.click('#add-book-btn')
        expect(page.locator('[data-test="book-item"]')).to_contain_text('Test Book')

        # Click on the first book's edit button
        page.locator('[data-test="edit-book-btn"]').first.click()
        expect(page.locator('[data-test="books-form"]')).to_be_visible()

        # Upload a photo
        page.set_input_files('input#photo-upload', 'jules-scratch/verification/test_image.jpg')

        # Check for wait cursor
        cursor_style = page.evaluate("document.body.style.cursor")
        print(f"Cursor style: {cursor_style}")
        # Not a reliable way to test this in playwright, but we can check if it changed from default

        # Wait for the photo to appear
        expect(page.locator('[data-test="book-photo"]')).to_be_visible()

        page.screenshot(path="jules-scratch/verification/verification.png")

    finally:
        context.close()
        browser.close()

with sync_playwright() as playwright:
    run(playwright)