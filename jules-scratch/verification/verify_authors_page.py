from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Navigate to the login page
    page.goto("http://localhost:8080")
    page.click("[data-test='menu-login']")
    page.wait_for_selector("[data-test='login-form']", state='visible')

    # Log in
    page.fill("[data-test='login-username']", "librarian")
    page.fill("[data-test='login-password']", "password")
    page.click("[data-test='login-submit']")
    try:
        page.wait_for_selector("[data-test='main-content']", state='visible', timeout=10000)
    except:
        page.screenshot(path="jules-scratch/verification/error.png")
        raise

    # Navigate to the authors page
    page.click("[data-test='menu-authors']")
    page.wait_for_selector("[data-test='authors-section']", state='visible')

    # Take a screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)