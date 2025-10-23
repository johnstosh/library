from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")

    # Wait for the welcome header to be visible
    welcome_header = page.locator('[data-test="welcome-header"]')
    expect(welcome_header).to_be_visible(timeout=120000)

    # Show the login form
    page.wait_for_function("typeof showLoginForm === 'function'")
    page.evaluate("showLoginForm()")

    # Wait for the login form to be visible
    login_form = page.locator('[data-test="login-form"]')
    expect(login_form).to_be_visible(timeout=120000)

    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("password")
    page.get_by_test_id("login-submit").click()

    # Go to authors page
    page.get_by_test_id("menu-authors").click()

    # Verify bulk import for authors is gone
    expect(page.get_by_test_id("bulk-import-authors-btn")).not_to_be_visible()

    # Go to books page
    page.get_by_test_id("menu-books").click()

    # Verify bulk import for books is gone
    expect(page.get_by_test_id("bulk-import-books-btn")).not_to_be_visible()

    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)