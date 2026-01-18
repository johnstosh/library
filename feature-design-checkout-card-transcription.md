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
- Uses `@JsonProperty` annotations to map snake_case JSON (from Grok) to camelCase Java properties

**LoanController Endpoint**
- `POST /api/loans/transcribe-checkout-card`
- Accepts multipart file upload (photo parameter)
- Security: Requires authenticated user (both USER and LIBRARIAN can access)
- Returns `CheckoutCardTranscriptionDto` with extracted data
- Validates file is not empty and is an image type

### Frontend Components

**CheckoutCardTranscriptionModal**
- React modal component for photo upload and result display
- Supports two modes:
  - File upload (from disk)
  - Camera capture (on mobile devices using HTML5 capture attribute)
- Shows image preview before transcription
- Displays transcription results in structured format
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
7. Backend calls Grok AI vision model with specialized prompt
8. Grok analyzes photo and returns JSON with extracted data
9. Backend parses JSON and returns structured DTO
10. Frontend displays transcription results in modal
11. User can use this information to find the book and create a loan record

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
- **AI Model:** Grok-4 (vision-capable model from xAI)
- **User requirement:** xAI API key must be configured in user settings

## Future Enhancements

Potential improvements:
- Auto-populate checkout form with transcribed data
- Batch transcription (multiple cards at once)
- Manual correction UI for transcription errors
- Training/calibration mode to improve accuracy
- Support for different card formats
- OCR confidence scores in response
