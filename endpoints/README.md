# API Endpoints Documentation

This folder contains the complete API endpoint documentation for the Library Management System, organized by subsystem and feature.

## Documentation Index

### General
- **[endpoints-guide.md](endpoints-guide.md)** - Authentication patterns, annotation rules, troubleshooting, and best practices

### Core Features
- **[endpoints-books.md](endpoints-books.md)** - Book caching endpoints and AI-assisted LOC call number suggestions
- **[endpoints-authors.md](endpoints-authors.md)** - Author filtering and statistics endpoints
- **[endpoints-libraries.md](endpoints-libraries.md)** - Library statistics and management
- **[endpoints-search.md](endpoints-search.md)** - Global search across books and authors

### Photo Management
- **[endpoints-photo-management.md](endpoints-photo-management.md)** - Photo CRUD operations for books and authors (upload, rotate, reorder, delete)
- **[endpoints-photo-export.md](endpoints-photo-export.md)** - Google Photos sync and backup
- **[endpoints-books-from-feed.md](endpoints-books-from-feed.md)** - Import books from Google Photos feed with AI processing

### Data Management
- **[endpoints-import-export.md](endpoints-import-export.md)** - JSON database export/import for backup and migration

### User Management
- **[endpoints-users.md](endpoints-users.md)** - User account creation, updates, and deletion (librarian functions)
- **[endpoints-user-settings.md](endpoints-user-settings.md)** - Current user profile and settings management
- **[endpoints-loans.md](endpoints-loans.md)** - Book checkout, return, and loan tracking

### Library Cards & Documents
- **[endpoints-library-cards.md](endpoints-library-cards.md)** - Library card applications and PDF card generation
- **[endpoints-labels.md](endpoints-labels.md)** - Book spine label PDF generation

## Quick Reference

### Authentication Levels
- **Public** - No authentication required
- **Authenticated** - Any logged-in user (`isAuthenticated()`)
- **Librarian** - Requires LIBRARIAN authority (`hasAuthority('LIBRARIAN')`)

### Common Patterns
All endpoints follow REST conventions:
- `GET` - Retrieve data
- `POST` - Create new resources
- `PUT` - Update existing resources
- `DELETE` - Remove resources

See [endpoints-guide.md](endpoints-guide.md) for detailed authentication and annotation guidelines.
