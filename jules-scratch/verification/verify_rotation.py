from playwright.sync_api import sync_playwright, expect
import os

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")
    page.wait_for_load_state("domcontentloaded")
    page.wait_for_selector("a[data-testid='menu-login']", timeout=60000)
    print(page.content())
    page.locator("a[data-testid='menu-login']").click()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("divinemercy")
    page.get_by_test_id("login-submit").click()

    page.get_by_test_id("menu-books").click()

    page.locator("[data-test='edit-book-btn']").first.click()

    # Create a dummy file to upload
    with open("rectangle.svg", "w") as f:
        f.write('<svg width="200" height="100" xmlns="http://www.w3.org/2000/svg"><rect width="200" height="100" style="fill:rgb(0,0,255);stroke-width:3;stroke:rgb(0,0,0)" /></svg>')

    page.locator("input#photo-upload").set_input_files("rectangle.svg")

    # Wait for the photo to appear
    photo_thumbnail = page.locator(".book-photo-thumbnail").first
    expect(photo_thumbnail).to_be_visible()

    # Rotate the image
    rotate_button = photo_thumbnail.locator(".rotate-cw-btn")
    rotate_button.click()

    # Wait for the rotation to be applied
    expect(page.locator(".book-photo-thumbnail img")).to_have_css("transform", "matrix(0, 1, -1, 0, 0, 0)")


    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()
    os.remove("rectangle.svg")

with sync_playwright() as playwright:
    run(playwright)