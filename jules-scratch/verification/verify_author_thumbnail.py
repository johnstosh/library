from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("http://localhost:8080")

    # Login
    page.wait_for_selector('[data-test="menu-login"]')
    page.locator('[data-test="menu-login"]').click()
    page.locator('[data-test="login-username"]').fill("librarian")
    page.locator('[data-test="login-password"]').fill("divinemercy")
    page.locator('[data-test="login-submit"]').click()

    # Create author
    page.wait_for_selector('[data-test="menu-authors"]')
    page.locator('[data-test="menu-authors"]').click()
    page.locator('[data-test="new-author-name"]').fill("Test Author")
    page.locator('[data-test="add-author-btn"]').click()

    # Go back to authors page to see the list
    page.locator('[data-test="menu-authors"]').click()
    page.wait_for_selector('[data-test="author-item"]')


    # Edit author and add photo
    page.locator('[data-test="edit-author-btn"]').first.click()
    page.wait_for_selector('[data-test="add-author-photo-btn"]')
    page.locator("#author-photo-upload").set_input_files('src/test/resources/test-images/test-image.jpg')
    page.wait_for_selector('[data-test="author-photo"]')

    # Rotate photo
    page.locator(".rotate-cw-btn").click()
    page.wait_for_timeout(1000)


    # Go back to authors page
    page.locator('[data-test="menu-authors"]').click()
    page.wait_for_selector('[data-test="author-photo-cell"]')

    page.screenshot(path="jules-scratch/verification/verification.png")
    browser.close()