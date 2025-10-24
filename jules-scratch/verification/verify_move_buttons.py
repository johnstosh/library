from playwright.sync_api import sync_playwright, expect
import time

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Log in as librarian
    page.goto("http://localhost:8080")
    page.locator('[data-test="menu-login"]').click()
    page.locator('[data-test="login-username"]').fill("librarian")
    page.locator('[data-test="login-password"]').fill("divinemercy")
    page.locator('[data-test="login-submit"]').click()
    expect(page.locator('[data-test="section-menu"]')).to_be_visible()

    # Go to test data page and create a book
    page.locator('[data-test="menu-test-data"]').click()
    page.locator('[data-test="num-books"]').fill("1")
    page.locator('[data-test="generate-test-data-btn"]').click()
    # It takes a bit for the book to be created.
    time.sleep(2)


    # Go to books page
    page.locator('[data-test="menu-books"]').click()
    expect(page.locator('[data-test="books-header"]')).to_be_visible()

    # Click on the first book to edit
    page.locator('[data-test="edit-book-btn"]').first.click()
    expect(page.locator('[data-test="books-form"]')).to_be_visible()

    # Add two photos
    page.locator('[data-test="add-photo-btn"]').click()
    page.locator('input[type="file"]').set_input_files('README.md')
    time.sleep(2)
    page.locator('[data-test="add-photo-btn"]').click()
    page.locator('input[type="file"]').set_input_files('README.md')


    # Wait for photos to load
    expect(page.locator('[data-test="book-photos-container"]')).to_be_visible()

    # Take a screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)