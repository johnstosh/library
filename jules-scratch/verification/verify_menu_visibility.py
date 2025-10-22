from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # --- Verification for Non-Librarian User ---
    page.goto("http://localhost:8080")
    page.click("[data-test='menu-login']")
    page.fill("[data-test='login-username']", "user")
    page.fill("[data-test='login-password']", "password")
    page.click("[data-test='login-submit']")
    page.pause()
    page.wait_for_selector("[data-test='search-section']", state='visible')

    # Check that librarian-only menu items are not visible
    expect(page.locator("[data-test='menu-libraries']")).not_to_be_visible()
    expect(page.locator("[data-test='menu-authors']")).not_to_be_visible()
    expect(page.locator("[data-test='menu-books']")).not_to_be_visible()
    expect(page.locator("[data-test='menu-users']")).not_to_be_visible()
    expect(page.locator("[data-test='menu-loans']")).not_to_be_visible()
    expect(page.locator("[data-test='menu-test-data']")).not_to_be_visible()

    page.screenshot(path="jules-scratch/verification/non_librarian_view.png")

    # --- Logout and Verification for Librarian User ---
    page.click("[data-test='menu-logout']")
    page.wait_for_selector("[data-test='menu-login']")

    page.click("[data-test='menu-login']")
    page.fill("[data-test='login-username']", "librarian")
    page.fill("[data-test='login-password']", "password")
    page.click("[data-test='login-submit']")
    page.wait_for_selector("[data-test='loans-section']", state='visible')

    # Check that librarian-only menu items are visible
    expect(page.locator("[data-test='menu-libraries']")).to_be_visible()
    expect(page.locator("[data-test='menu-authors']")).to_be_visible()
    expect(page.locator("[data-test='menu-books']")).to_be_visible()
    expect(page.locator("[data-test='menu-users']")).to_be_visible()
    expect(page.locator("[data-test='menu-loans']")).to_be_visible()
    expect(page.locator("[data-test='menu-test-data-item']")).to_be_visible()

    page.screenshot(path="jules-scratch/verification/librarian_view.png")

    context.close()
    browser.close()

with sync_playwright() as playwright:
    run(playwright)