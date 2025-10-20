from playwright.sync_api import sync_playwright, Page, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")
    page.wait_for_load_state("domcontentloaded")
    page.click("[data-test='menu-login']")
    page.fill("[data-test='login-username']", "librarian")
    page.fill("[data-test='login-password']", "password")
    page.click("[data-test='login-submit']")
    page.wait_for_timeout(5000) # 5 second delay
    page.wait_for_selector("[data-test='main-content']", timeout=60000)

    page.click("[data-test='menu-loans']")
    page.wait_for_selector("[data-test='loans-section']")

    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)