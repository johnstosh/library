from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")
    page.wait_for_load_state("domcontentloaded")

    # Login
    page.wait_for_selector("[data-test='menu-login']", state='visible')
    page.click("[data-test='menu-login']")
    page.wait_for_selector("[data-test='login-form']")
    page.fill("[data-test='login-username']", "librarian")
    page.fill("[data-test='login-password']", "password")
    page.click("[data-test='login-submit']")
    page.wait_for_selector("[data-test='main-content']", state='visible')

    # Navigate to authors section
    page.click("[data-test='menu-authors']")
    page.wait_for_selector("#authors-section")

    # Take screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)