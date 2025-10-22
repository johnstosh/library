from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Go to the login page
    page.goto("http://localhost:8080")

    # Go to the login page
    page.goto("http://localhost:8080")

    # Go to the login page
    page.goto("http://localhost:8080")

    # Go to the login page
    page.goto("http://localhost:8080")

    # Give the page some time to load
    page.wait_for_timeout(5000)

    # Show the login form
    page.evaluate("showLoginForm()")

    # Log in as librarian
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("password")
    page.get_by_test_id("login-submit").click()

    # Wait for the main content to load
    expect(page.get_by_test_id("main-content")).to_be_visible()

    # Navigate to the "Applications" section
    page.get_by_test_id("menu-applied").click()

    # Wait for the applications table to be visible
    expect(page.get_by_test_id("applied-table")).to_be_visible()

    # Take a screenshot
    page.screenshot(path="jules-scratch/verification/applications_section.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)