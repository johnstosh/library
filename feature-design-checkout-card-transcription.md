# Checkout Card Transcription Feature

## Overview

The Checkout Card Transcription feature uses Grok AI (xAI) vision capabilities to automatically extract information from photos of library checkout cards. This enables quick digitization of physical checkout records into the library management system.

## Architecture

### Backend Components

**CheckoutCardTranscriptionService**
- Service class that handles photo-to-text transcription using Grok AI
- Uses the existing `AskGrok` service for AI integration
- Sends a specialized prompt to Grok-4 (vision model) for checkout card analysis
- Parses JSON response from Grok into structured DTO
- Extracts JSON from response text (handles cases where Grok includes extra explanation)

**CheckoutCardTranscriptionDto**
- DTO for transcription results
- Fields:
  - `title` - Book title
  - `author` - Author name
  - `callNumber` - Library of Congress call number
  - `lastDate` - Date of last checkout
  - `lastIssuedTo` - Name of patron who last borrowed the book
  - `lastDue` - Due date of last checkout
- Service parses snake_case JSON from Grok and returns camelCase to frontend

**LoanController Endpoint**
- `POST /api/loans/transcribe-checkout-card`
- Accepts multipart file upload (photo parameter)
- Security: Requires authenticated user (both USER and LIBRARIAN can access)
- Returns `CheckoutCardTranscriptionDto` with extracted data
- Validates file is not empty and is an image type

### Frontend Components

**CheckoutCardTranscriptionModal**
- React modal component for photo upload
- Supports two modes:
  - File upload (from disk)
  - Camera capture (on mobile devices using HTML5 capture attribute)
- Shows image preview before transcription
- Auto-navigates to checkout form with pre-filled filters on successful transcription
- Error handling for failed transcription
- Uses TanStack Query mutation for API integration

**LoansPage Integration**
- Two new buttons: "Checkout by Photo" and "Checkout by Camera"
- "Checkout by Photo" - Opens modal with file picker
- "Checkout by Camera" - Opens modal with camera capture (on supported devices)
- Positioned next to existing "Checkout Book" button

**API Integration**
- `useTranscribeCheckoutCard()` hook in `frontend/src/api/loans.ts`
- Uses `api.postFormData()` helper for multipart file uploads
- Returns `CheckoutCardTranscriptionDto` on success

## Grok AI Prompt

The service sends a detailed prompt to Grok that:
1. Describes the expected structure of a checkout card image
2. Instructs Grok to extract specific fields
3. Requests JSON output format
4. Handles variations (handwritten text, tilted images, blank fields)
5. Uses "N/A" for missing or blank fields

The prompt is designed specifically for the St. Martin de Porres library card format, but can work with similar card designs.

## Typical Workflow

1. User clicks "Checkout by Photo" or "Checkout by Camera" on Loans page
2. Modal opens with appropriate file input configuration
3. User selects/captures photo of checkout card
4. Image preview is displayed
5. User clicks "Transcribe" button
6. Photo is uploaded to backend API
7. Backend calls Grok AI vision model (grok-4-fast) with specialized prompt
8. Grok analyzes photo and returns JSON with extracted data
9. Backend parses JSON (snake_case) and returns structured DTO (camelCase)
10. Modal automatically closes and navigates to checkout form with pre-filled data:
    - Title filter populated from transcribed title
    - Author filter populated from transcribed author
    - Call Number filter populated from transcribed call number
    - Borrower filter populated from "Issued To" name
    - Checkout Date populated from last checkout date (normalized to MM-DD-YYYY)
    - Due Date populated from last due date (normalized to MM-DD-YYYY)
    - **Important:** When dates are provided from transcription, the form preserves them
      and does NOT auto-recalculate the due date from checkout date
11. User selects the matching book and borrower, then completes checkout

## Date Format Handling

The frontend normalizes dates from Grok to the form's expected MM-DD-YYYY format:

**Input formats supported:**
- `M-D` (e.g., "1-7") - Month-Day, year assumed to be current year
- `M-D-YY` (e.g., "1-7-26") - Month-Day-Year with 2-digit year (20XX assumed)
- `M-D-YYYY` (e.g., "1-7-2026") - Full format

**Behavior:**
- If transcription provides both checkoutDate and dueDate, both are preserved as-is
- The form's auto-calculation of dueDate (checkout + 14 days) is skipped when
  transcription provides a dueDate
- If the user later changes the checkout date manually, auto-calculation resumes

## Security & Authentication

- Requires authenticated user (either USER or LIBRARIAN authority)
- Uses existing xAI API key from user settings
- Each user must have their own xAI API key configured in User Settings
- Photo data is not stored permanently (only processed during request)

## Error Handling

**Backend:**
- Empty file validation
- Image file type validation
- Grok API errors (API key missing, network issues, etc.)
- JSON parsing errors (if Grok returns unexpected format)
- LibraryException thrown for all errors with descriptive messages

**Frontend:**
- Empty file validation
- Non-image file validation
- Network error handling via TanStack Query
- User-friendly error messages displayed in modal

## Testing

**Unit Tests:**
- `CheckoutCardTranscriptionServiceTest` - Tests service logic, JSON parsing, error handling

**Integration Tests:**
- `LoanControllerTest` - Tests API endpoint with various scenarios:
  - Successful transcription
  - Unauthorized access
  - Empty file
  - Non-image file

## Dependencies

- **Existing services:** AskGrok (for xAI API integration)
- **AI Model:** grok-4-fast (vision-capable model from xAI)
- **User requirement:** xAI API key must be configured in user settings

## Photo Storage

Starting with this feature, checkout card photos are stored in the database and associated with the loan record.

### How It Works

1. When user transcribes a photo, the photo is stored in sessionStorage (base64 encoded)
2. When the loan is created, the frontend calls `/api/loans/checkout-with-photo` with the photo
3. Backend saves the photo to the photos table with a foreign key to the loan
4. Photo SHA-256 checksum is computed for frontend caching
5. Loan DTO includes `photoId` and `photoChecksum` fields
6. Loan view page displays the photo using the ThrottledThumbnail component
7. Frontend uses checksum-based caching for efficient photo loading

### Database Schema

The `Photo` entity now includes:
- `loan_id` (FK to Loan) - nullable, for checkout card photos

### API Endpoints

**POST /api/loans/checkout-with-photo**
- Multipart form data with:
  - `bookId` (required)
  - `userId` (required)
  - `loanDate` (optional, ISO date string)
  - `dueDate` (optional, ISO date string)
  - `photo` (optional, image file)
- Returns `LoanDto` with `photoId` and `photoChecksum` if photo was saved

### Frontend Caching

The frontend uses the `ThrottledThumbnail` component with checksum-based caching:
- Photos are cached by their SHA-256 checksum
- Cache persists across page navigation
- Reduces network requests when viewing the same photo multiple times

## Future Enhancements

Potential improvements:
- Batch transcription (multiple cards at once)
- Manual correction UI for transcription errors
- Training/calibration mode to improve accuracy
- Support for different card formats
- OCR confidence scores in response
