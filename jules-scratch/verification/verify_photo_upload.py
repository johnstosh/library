import re
from playwright.sync_api import Page, expect

def test_add_photo_to_book(page: Page):
    # 1. Arrange: Go to the application and log in.
    page.goto("http://localhost:8080")
    page.get_by_test_id("menu-login").click()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("password")
    page.get_by_test_id("login-submit").click()

    # 2. Act: Navigate to the books section and edit a book.
    page.get_by_test_id("menu-books").click()
    page.get_by_test_id("book-item").first.get_by_test_id("edit-book-btn").click()

    # 3. Assert: Check that the "Add Photo" button is visible.
    expect(page.get_by_test_id("add-photo-btn")).to_be_visible()

    # 4. Act: Upload a photo.
    with page.expect_file_chooser() as fc_info:
        page.get_by_test_id("add-photo-btn").click()
    file_chooser = fc_info.value
    file_chooser.set_files("src/test/resources/test-image.png")

    # 5. Assert: Check that the photo is displayed.
    expect(page.get_by_test_id("book-photo")).to_be_visible()

    # 6. Screenshot: Capture the final result for visual verification.
    page.screenshot(path="verification.png")