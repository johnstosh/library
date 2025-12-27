# Library Management System

A full-stack library management system with book cataloging, patron tracking, library card generation, and Google Photos integration.

## Technology Stack

### Backend
- **Java 17** with Spring Boot 3.5+
- **PostgreSQL** (production) / **H2** (development/testing)
- **Spring Security** with OAuth2 (Google SSO)
- **Spring Data JPA** with Hibernate ORM
- **MapStruct** for DTO mapping
- **iText 8** for PDF generation
- **Google Photos API** for photo storage
- **Marc4J** for Library of Congress lookup

### Frontend
- **React 18.3** with TypeScript 5.3
- **Tailwind CSS v4** for styling
- **TanStack Query v5** for server state
- **React Router v6** for routing
- **Zustand** for client state
- **Vite 7.3** for builds

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+ and npm
- Docker (for deployment)
- Google Cloud SDK (for deployment)

### Development Mode

**Option 1: Run both servers together (Recommended)**
```bash
./dev.sh
```
This starts:
- React dev server at http://localhost:5173 (with hot reload)
- Spring Boot API at http://localhost:8080

**Option 2: Run servers separately**

Terminal 1 - Backend:
```bash
./gradlew bootRun
```

Terminal 2 - Frontend:
```bash
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173 in your browser.

### Production Build

Build the entire application (frontend + backend):
```bash
./build-frontend.sh  # Build React and copy to static resources
./gradlew build      # Build Spring Boot JAR with embedded frontend
```

Run the production JAR:
```bash
java -jar build/libs/library-*.jar
```

Then open http://localhost:8080 in your browser.

## Development Workflow

### Frontend Development

The frontend lives in `frontend/` and uses Vite for fast development:

```bash
cd frontend

# Install dependencies
npm install

# Start dev server with hot reload (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Lint and format
npm run lint
```

**Development Server Features:**
- Hot Module Replacement (HMR) for instant updates
- API proxy to Spring Boot (requests to /api/* → http://localhost:8080)
- TypeScript type checking
- Fast rebuild times (~50ms)

**Frontend Project Structure:**
```
frontend/src/
├── api/          # API clients with TanStack Query hooks
├── pages/        # Page components (routes)
├── components/   # Reusable components
├── stores/       # Zustand state stores
├── utils/        # Utility functions
├── types/        # TypeScript types
└── config/       # Configuration
```

### Backend Development

The backend is a Spring Boot application:

```bash
# Run with H2 in-memory database (default)
./gradlew bootRun

# Run with PostgreSQL (requires configuration)
./gradlew bootRun --args='--spring.profiles.active=prod'

# Run tests (excluding UI tests)
./fast-tests.sh

# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "com.muczynski.library.controller.BookControllerTest"

# Clean build
./gradlew clean build
```

**Backend Project Structure:**
```
src/main/java/com/muczynski/library/
├── controller/   # REST controllers (@RestController)
├── service/      # Business logic (@Service)
├── repository/   # Data access (@Repository)
├── entity/       # JPA entities
├── dto/          # Data Transfer Objects
├── mapper/       # MapStruct mappers
├── config/       # Spring configuration
└── security/     # Security configuration
```

### Making Changes

**Adding a Backend API Endpoint:**
1. Create/update entity in `entity/`
2. Create repository in `repository/`
3. Create DTO in `dto/`
4. Create MapStruct mapper in `mapper/`
5. Create service in `service/`
6. Create controller in `controller/`
7. Add tests in `src/test/java/`

**Adding a Frontend Feature:**
1. Create API functions in `frontend/src/api/feature.ts`
2. Add React Query hooks (useFeatures, useCreateFeature, etc.)
3. Create page component in `frontend/src/pages/feature/`
4. Add child components (filters, table, form, modals)
5. Add route in `frontend/src/App.tsx` (lazy-loaded)
6. Add navigation link in `frontend/src/components/layout/Navigation.tsx`
7. Add TypeScript interfaces in `frontend/src/types/dtos.ts`

## Testing

### Unit & Integration Tests (Backend)
```bash
# Fast tests (excludes UI tests)
./fast-tests.sh

# All tests
./gradlew test

# Single test class
./gradlew test --tests "com.muczynski.library.controller.BookControllerTest"
```

### UI Tests (Playwright)
```bash
# Run UI tests
./gradlew test --tests "com.muczynski.library.ui.*"
```

### Frontend Tests (Future)
```bash
cd frontend
npm run test  # React Testing Library (planned)
```

## Building & Deployment

### Building for Production

**Step 1: Build Frontend**
```bash
./build-frontend.sh
```
This:
- Runs `npm run build` in frontend/
- Copies the production build to `src/main/resources/static/`
- Clears old static files
- Creates optimized bundles with code splitting

**Step 2: Build Backend**
```bash
./gradlew clean build
```
This:
- Compiles Java code
- Runs all tests
- Packages frontend static files into the JAR
- Creates `build/libs/library-*.jar`

**All-in-One Build:**
```bash
./build-frontend.sh && ./gradlew clean build
```

### Deployment to Google Cloud Run

**Prerequisites:**
- Google Cloud account
- Project configured
- Cloud SQL instance

**Environment Variables Required:**
```bash
export GCP_PROJECT_ID="your-project-id"
export DB_PASSWORD="your-db-password"
export GCP_REGION="us-east1"
export BINARY_REPO_NAME="scrabble-game"  # Optional, defaults to this
export CLOUD_RUN_SERVICE_ACCOUNT="service-account-name"  # Optional
```

**Deploy:**
```bash
./deploy.sh
```

This script:
1. Builds the React frontend
2. Creates/updates Cloud SQL database
3. Builds Spring Boot JAR
4. Creates Docker image
5. Pushes to Artifact Registry
6. Deploys to Cloud Run
7. Configures environment variables
8. Connects to Cloud SQL

### Manual Docker Build

```bash
# Build frontend first
./build-frontend.sh

# Build Docker image
docker build -t library-app .

# Run locally
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_PASSWORD=yourpassword \
  library-app
```

## Configuration

### Backend Configuration

**Development (H2 Database):**
`src/main/resources/application.properties`

**Production (PostgreSQL):**
`src/main/resources/application-prod.properties`

**Key Settings:**
- `server.port=8080` - API port
- `spring.jpa.hibernate.ddl-auto=update` - Auto-update schema
- `app.show-test-data-page=true` - Show test data menu (dev only)

### Frontend Configuration

**Vite Configuration:**
`frontend/vite.config.ts`
- Path aliases (@/)
- Dev server port (5173)
- API proxy to Spring Boot

**API Base URL:**
`frontend/src/api/client.ts`
- Development: Proxied through Vite
- Production: Same origin (served by Spring Boot)

## Application Features

### Core Features
- **Books Management**: CRUD operations, filters, status tracking
- **Authors Management**: Biographical info, book counts
- **Library Management**: Multiple library branches
- **Loans**: Checkout, return, overdue tracking
- **Users**: Patron and staff accounts with roles
- **Search**: Public book/author search (no auth required)

### Advanced Features
- **Photo Management**: Upload, crop, rotate, reorder
- **Google Photos Integration**: Import books from photo feed
- **Library of Congress Lookup**: Automatic call number retrieval
- **Library Cards**: Generate wallet-sized PDF cards
- **Book Labels**: PDF spine labels with LOC numbers
- **Data Management**: JSON import/export, photo backup
- **Google OAuth SSO**: Login with Google account

### Access Control
- **PUBLIC**: Search functionality (no login)
- **USER**: Books, Authors, Loans, My Card, Settings
- **LIBRARIAN**: Full access including Users, Libraries, Data Management

## Architecture

### Request Flow (Development)

```
Browser → http://localhost:5173
         ↓
    Vite Dev Server (React)
         ↓
    /api/* requests proxied to →
         ↓
    http://localhost:8080
         ↓
    Spring Boot (API)
         ↓
    H2 Database
```

### Request Flow (Production)

```
Browser → http://localhost:8080
         ↓
    Spring Boot
    ├─ /api/* → REST Controllers
    └─ /* → index.html (React SPA)
         ↓
    PostgreSQL / H2
```

### Frontend Architecture
- **Routing**: React Router with protected routes
- **State**: TanStack Query (server) + Zustand (client) + useState (local)
- **Styling**: Tailwind CSS utility classes
- **API**: TanStack Query hooks with automatic caching
- **Code Splitting**: Lazy-loaded page components (49% bundle reduction)

### Backend Architecture
- **Controller Layer**: REST endpoints with DTOs
- **Service Layer**: Business logic, transactions
- **Repository Layer**: Spring Data JPA
- **Security**: Spring Security with authorities (not roles)
- **DTOs**: MapStruct for entity-DTO conversion

## Performance

### Frontend Optimization
- **Code Splitting**: 34 separate chunks, 275 KB initial bundle
- **Query Caching**: 5-minute stale time, 30-minute garbage collection
- **Lazy Loading**: All page components loaded on demand
- **Error Boundaries**: Graceful error handling

### Backend Optimization
- **Database**: Connection pooling, JPA lazy loading
- **Caching**: Browser-based photo caching
- **Queries**: LEFT JOIN FETCH for performance

## Documentation

Detailed documentation in root directory:

- `feature-design-frontend.md` - Frontend architecture (React)
- `feature-design-security.md` - Authentication & authorization
- `feature-design-photos.md` - Photo storage & Google Photos
- `feature-design-loc.md` - Library of Congress integration
- `feature-design-import-export.md` - Data import/export
- `feature-design-libraries.md` - Library card design
- `backend-requirements.md` - Backend development guidelines
- `endpoints.md` - API endpoint documentation
- `FRONTEND_PROGRESS.md` - Migration status and features
- `CLAUDE.md` - AI assistant context

## Troubleshooting

### Frontend Issues

**Build Errors:**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

**Dev Server Won't Start:**
- Check port 5173 is available: `lsof -i :5173`
- Check Node.js version: `node --version` (need 18+)

**API Requests Failing:**
- Verify backend is running on port 8080
- Check Vite proxy config in `vite.config.ts`
- Check browser Network tab for errors

### Backend Issues

**Database Connection:**
- Development: Uses H2 in-memory (no config needed)
- Production: Check `application-prod.properties` settings

**Port Already in Use:**
```bash
# Kill process on port 8080
lsof -i :8080
kill -9 <PID>
```

**Tests Failing:**
```bash
# Run fast tests only (excludes UI tests)
./fast-tests.sh

# Clean and rebuild
./gradlew clean build
```

### Production Build Issues

**Static Files Not Found:**
```bash
# Rebuild frontend and copy to static/
./build-frontend.sh

# Verify files copied
ls -la src/main/resources/static/

# Rebuild JAR
./gradlew clean build
```

**React Router 404s:**
- Check `SpaController.java` is mapping all routes
- Verify `index.html` is in static/ directory

## Git Workflow

This project uses direct commits to the `dev` branch:

```bash
# Before starting work
git checkout dev
git pull

# After completing work
git add .
git commit -m "Description of changes"
git push origin dev
```

## Environment Variables

### Development
No environment variables required (uses H2 in-memory database).

### Production Deployment
Required:
- `GCP_PROJECT_ID` - Google Cloud project ID
- `DB_PASSWORD` - PostgreSQL password
- `GCP_REGION` - Google Cloud region (default: us-east1)

Optional:
- `BINARY_REPO_NAME` - Artifact Registry repo (default: scrabble-game)
- `CLOUD_RUN_SERVICE_ACCOUNT` - Service account name

## Scripts Reference

### Development Scripts
- `./dev.sh` - Run both frontend and backend dev servers
- `./fast-tests.sh` - Run tests excluding UI tests

### Build Scripts
- `./build-frontend.sh` - Build React and copy to static/
- `./gradlew bootRun` - Run Spring Boot
- `./gradlew build` - Build production JAR

### Deployment Scripts
- `./deploy.sh` - Deploy to Google Cloud Run
- `./docker-test.sh` - Run tests in Docker
- `./docker-stop-testing.sh` - Stop Docker test containers

## Support

For issues or questions:
1. Check documentation in `.md` files
2. Check troubleshooting section above
3. Review error logs in console
4. Check browser DevTools Network tab

## License

(c) Copyright 2025 by Muczynski
