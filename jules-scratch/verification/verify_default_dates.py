from playwright.sync_api import sync_playwright, expect
import datetime

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    try:
        print("Navigating to the page...")
        page.goto("http://localhost:8080")
        page.wait_for_load_state("networkidle")
        print("Page loaded.")

        print("Clicking login menu button...")
        page.click("[data-test='menu-login']")
        print("Login menu button clicked.")

        print("Filling in login form...")
        page.fill("[data-test='login-username']", "librarian")
        page.fill("[data-test='login-password']", "password")
        print("Login form filled.")

        print("Clicking login submit button...")
        page.click("[data-test='login-submit']")
        print("Login submit button clicked.")

        print("Waiting for URL to change...")
        page.wait_for_url("http://localhost:8080/")
        print("URL changed.")


        print("Waiting for main content to be visible...")
        page.wait_for_selector("[data-test='main-content']", state='visible')
        print("Main content is visible.")

        print("Clicking loans menu button...")
        page.click("[data-test='menu-loans']")
        print("Loans menu button clicked.")

        print("Waiting for loans section to be visible...")
        page.wait_for_selector("[data-test='loans-section']", state='visible')
        print("Loans section is visible.")

        loan_date_input = page.locator("[data-test='loans-form'] [data-test='loan-date']")
        due_date_input = page.locator("[data-test='loans-form'] [data-test='due-date']")

        today = datetime.date.today()
        two_weeks = today + datetime.timedelta(days=14)

        expect(loan_date_input).to_have_value(today.strftime('%Y-%m-%d'))
        expect(due_date_input).to_have_value(two_weeks.strftime('%Y-%m-%d'))

        page.screenshot(path="jules-scratch/verification/verification.png")
        print("Screenshot taken.")

    finally:
        browser.close()

with sync_playwright() as playwright:
    run(playwright)