from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")
    page.wait_for_load_state("networkidle")

    # Screenshot of the page for an unauthenticated user
    page.wait_for_selector('[data-test="menu-library-card"]', timeout=5000)
    page.get_by_test_id("menu-library-card").click()
    page.screenshot(path="jules-scratch/verification/unauthenticated_library_card.png")

    # Log in as a librarian
    page.get_by_test_id("menu-login").click()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("divinemercy")
    page.get_by_test_id("login-submit").click()

    # Screenshot of the page for an authenticated user
    page.get_by_test_id("menu-library-card").click()
    page.wait_for_selector('[data-test="applied-table"]')
    page.screenshot(path="jules-scratch/verification/authenticated_library_card.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)