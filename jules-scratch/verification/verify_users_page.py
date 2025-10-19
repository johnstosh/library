from playwright.sync_api import sync_playwright, Page, expect
import time

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")
    page.click("[data-test='menu-login']")
    page.fill("[data-test='login-username']", "librarian")
    page.fill("[data-test='login-password']", "password")
    page.click("[data-test='login-submit']")
    time.sleep(2) # Give page time to react
    page.screenshot(path="jules-scratch/verification/after_login_attempt.png")
    page.wait_for_selector("[data-test='welcome-screen']", state='hidden')
    page.wait_for_selector("[data-test='main-content']")

    page.click("[data-test='menu-users']")
    page.wait_for_selector("[data-test='users-section']")

    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)