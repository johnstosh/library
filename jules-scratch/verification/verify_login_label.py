import time
from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    time.sleep(10) # Wait for server to start

    page.goto("http://localhost:8080")

    # Wait for the page to load
    page.wait_for_load_state("networkidle")

    # Click the login button to show the form
    page.locator("[data-test='menu-login']").click()

    # Check that the label is "Name:"
    expect(page.locator("label[for='username']")).to_have_text("Name:")

    # Take a screenshot of the login form
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)