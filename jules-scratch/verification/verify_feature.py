from playwright.sync_api import sync_playwright, expect
import time

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    try:
        context = browser.new_context()
        page = context.new_page()

        # Give the server a moment to start up
        time.sleep(10)

        page.goto("http://localhost:8080")

        # Login
        page.click("[data-test='menu-login']")
        page.wait_for_selector("[data-test='login-form']", state='visible')
        page.fill("[data-test='login-username']", "librarian")
        page.fill("[data-test='login-password']", "password")
        page.click("[data-test='login-submit']")

        # Check for login error
        try:
            error_message = page.locator("[data-test='login-error']")
            expect(error_message).to_be_hidden()
        except Exception as e:
            print(f"Login failed with error: {e}")
            page.screenshot(path="jules-scratch/verification/login-error.png")
            raise

        page.wait_for_selector("[data-test='main-content']", state='visible')

        # Navigate to books section
        page.click("[data-test='menu-books']")
        page.wait_for_selector("#books-section", state='visible')

        # Click edit on the first book
        page.locator("[data-test='edit-book-btn']").first.click()

        # Wait for photos to be visible
        page.wait_for_selector("[data-test='book-photos-container']", state='visible')

        # Take a screenshot
        page.screenshot(path="jules-scratch/verification/verification.png")
    finally:
        browser.close()

with sync_playwright() as playwright:
    run(playwright)