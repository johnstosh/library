from playwright.sync_api import sync_playwright, Page, expect
import pytest

def login(page: Page, port: int):
    page.goto(f"http://localhost:{port}", wait_until="domcontentloaded")
    page.wait_for_url(f"http://localhost:{port}/", timeout=60000)
    page.get_by_test_id("menu-login").click()
    page.wait_for_selector("[data-test='login-form']", timeout=10000)
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("password")
    page.get_by_test_id("login-submit").click()
    expect(page.locator("#main-content")).to_be_visible(timeout=10000)

def navigate_to_section(page: Page, section: str):
    page.get_by_test_id(f"menu-{section}").click()
    expect(page.locator(f"#{section}-section")).to_be_visible(timeout=10000)

def run_test():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Hardcoding the port for the verification script
        port = 8080

        login(page, port)
        navigate_to_section(page, "books")

        expect(page.locator("[data-test='book-item']")).to_have_count(5, timeout=20000)

        page.screenshot(path="jules-scratch/verification/verification.png")

        browser.close()

if __name__ == "__main__":
    run_test()