# JSON types

Field lists for the response and request types named by the controller pages.
Enum constants are the Java names. Several filters also accept a lowercase key; the controller page states which.

## AclaLookupResultDto

- `bookId` (`Long`)
- `success` (`boolean`)
- `audioAvailable` (`Boolean`)
- `paperAvailable` (`Boolean`)
- `ebookAvailable` (`Boolean`)
- `matchedTitle` (`String`)
- `matches` (`List<CatalogMatchRef>`)
- `errorMessage` (`String`)

## AdditionalShippingUpdateDto

- `additionalShippingDollars` (`BigDecimal`)

## Album

- `title` (`String`)

## AlbumCreateRequest

- `album` (`Album`)

## AlbumPosition

- `position` (`String`)

## AlbumResponse

- `album` (`Album`)

## AlbumsListResponse

- `albums` (`List<Album>`)
- `nextPageToken` (`String`)

## AppliedDto

- `id` (`Long`)
- `name` (`String`)
- `email` (`String`)
- `phone` (`String`)
- `status` (`Applied.ApplicationStatus`)

## AuthorAvailabilityDto

- `authorId` (`Long`)
- `hasYdlBook` (`Boolean`)
- `hasYdlEbook` (`Boolean`)
- `hasYdlAudio` (`Boolean`)
- `hasEmuBook` (`Boolean`)
- `hasEmuEbook` (`Boolean`)
- `hasEmuAudio` (`Boolean`)
- `hasAclaBook` (`Boolean`)
- `hasAclaEbook` (`Boolean`)
- `hasAclaAudio` (`Boolean`)
- `hasWccBook` (`Boolean`)
- `hasWccEbook` (`Boolean`)
- `hasWccAudio` (`Boolean`)
- `hasMsuBook` (`Boolean`)
- `hasMsuEbook` (`Boolean`)
- `hasMsuAudio` (`Boolean`)

## AuthorDto

- `id` (`Long`)
- `name` (`String`)
- `alternateNames` (`List<String>`)
- `dateOfBirth` (`LocalDate`)
- `dateOfDeath` (`LocalDate`)
- `religiousAffiliation` (`String`)
- `birthCountry` (`String`)
- `nationality` (`String`)
- `briefBiography` (`String`)
- `grokipediaUrl` (`String`)
- `invalidGrokipediaUrls` (`List<RejectedUrl>`)
- `firstPhotoId` (`Long`)
- `firstPhotoChecksum` (`String`)
- `bookCount` (`Long`)
- `lastModified` (`LocalDateTime`)
- `books` (`List<BookDto>`)

## AuthorEnrichmentResultDto

- `authorId` (`Long`)
- `name` (`String`)
- `success` (`boolean`)
- `skipped` (`boolean`)
- `filledFields` (`List<String>`)
- `errorMessage` (`String`)
- `updatedAuthor` (`AuthorDto`)

## AuthorNameMatchDto

- `id` (`Long`)
- `name` (`String`)

## AuthorNameNormalizationResultDto

- `authorId` (`Long`)
- `name` (`String`)
- `before` (`String`)
- `after` (`String`)
- `changed` (`boolean`)
- `success` (`boolean`)
- `errorMessage` (`String`)
- `mergedIntoAuthorId` (`Long`)
- `updatedAuthor` (`AuthorDto`)

## AuthorSummaryDto

- `id` (`Long`)
- `lastModified` (`LocalDateTime`)
- `name` (`String`)

## BatchCreateRequest

- `albumId` (`String`)
- `newMediaItems` (`List<NewMediaItem>`)
- `albumPosition` (`AlbumPosition`)

## BatchCreateResponse

- `newMediaItemResults` (`List<NewMediaItemResult>`)

## BatchGetRequest

- `mediaItemIds` (`List<String>`)

## BatchGetResponse

- `mediaItemResults` (`List<MediaItemResult>`)

## BatchUpdateRequest

- `mediaItemUpdates` (`List<MediaItemUpdate>`)

## BatchUpdateResponse

- `mediaItemResults` (`List<MediaItemResult>`)

## BookAvailabilityStatsDto

- `electronicResource` (`long`)
- `hasCallNumber` (`long`)
- `hasFreeOnlineText` (`long`)
- `hasFreeOnlineAudio` (`long`)
- `withdrawn` (`long`)
- `requested` (`long`)
- `availableAtYdl` (`long`)
- `ydlPaper` (`long`)
- `ydlEbook` (`long`)
- `ydlAudio` (`long`)
- `availableAtEmu` (`long`)
- `emuPaper` (`long`)
- `emuEbook` (`long`)
- `emuAudio` (`long`)
- `availableAtAcla` (`long`)
- `aclaPaper` (`long`)
- `aclaEbook` (`long`)
- `aclaAudio` (`long`)
- `availableAtWcc` (`long`)
- `wccPaper` (`long`)
- `wccEbook` (`long`)
- `wccAudio` (`long`)
- `availableAtMsu` (`long`)
- `msuPaper` (`long`)
- `msuEbook` (`long`)
- `msuAudio` (`long`)

## BookDto

- `id` (`Long`)
- `title` (`String`)
- `alternateTitles` (`List<String>`)
- `publicationYear` (`Integer`)
- `publisher` (`String`)
- `plotSummary` (`String`)
- `relatedWorks` (`String`)
- `detailedDescription` (`String`)
- `grokipediaUrl` (`String`)
- `invalidGrokipediaUrls` (`List<RejectedUrl>`)
- `freeTextUrl` (`String`)
- `invalidFreeTextUrls` (`List<RejectedUrl>`)
- `invalidFreeAudioUrls` (`List<RejectedUrl>`)
- `invalidHoldings` (`List<RejectedHolding>`)
- `freeTextLastSearched` (`LocalDateTime`)
- `dateAddedToLibrary` (`LocalDateTime`)
- `lastModified` (`LocalDateTime`)
- `status` (`BookStatus`)
- `authorId` (`Long`)
- `author` (`String`)
- `authorGrokipediaUrl` (`String`)
- `libraryId` (`Long`)
- `library` (`String`)
- `firstPhotoId` (`Long`)
- `firstPhotoChecksum` (`String`)
- `loanCount` (`Long`)
- `locNumber` (`String`)
- `electronicResource` (`Boolean`)
- `statusReason` (`String`)
- `ydlAudioAvailable` (`Boolean`)
- `ydlPaperAvailable` (`Boolean`)
- `ydlEbookAvailable` (`Boolean`)
- `ydlLastChecked` (`LocalDateTime`)
- `ydlLookupError` (`String`)
- `emuAudioAvailable` (`Boolean`)
- `emuPaperAvailable` (`Boolean`)
- `emuEbookAvailable` (`Boolean`)
- `emuLastChecked` (`LocalDateTime`)
- `emuLookupError` (`String`)
- `aclaAudioAvailable` (`Boolean`)
- `aclaPaperAvailable` (`Boolean`)
- `aclaEbookAvailable` (`Boolean`)
- `aclaLastChecked` (`LocalDateTime`)
- `aclaLookupError` (`String`)
- `wccAudioAvailable` (`Boolean`)
- `wccPaperAvailable` (`Boolean`)
- `wccEbookAvailable` (`Boolean`)
- `wccLastChecked` (`LocalDateTime`)
- `wccLookupError` (`String`)
- `msuAudioAvailable` (`Boolean`)
- `msuPaperAvailable` (`Boolean`)
- `msuEbookAvailable` (`Boolean`)
- `msuLastChecked` (`LocalDateTime`)
- `msuLookupError` (`String`)
- `readingDifficulty` (`ReadingDifficulty`)
- `binding` (`BookCoverType`)
- `desireToPurchase` (`Integer`)
- `tagsList` (`List<String>`)

## BookLocStatusDto

- `id` (`Long`)
- `title` (`String`)
- `authorName` (`String`)
- `currentLocNumber` (`String`)
- `hasLocNumber` (`boolean`)
- `publicationYear` (`Integer`)
- `firstPhotoId` (`Long`)
- `firstPhotoChecksum` (`String`)
- `dateAdded` (`String`)

## BookPriceDto

- `id` (`Long`)
- `bookId` (`Long`)
- `bookTitle` (`String`)
- `author` (`String`)
- `sellerId` (`Long`)
- `sellerName` (`String`)
- `cover` (`BookCoverType`)
- `priceDollars` (`BigDecimal`)
- `shippingDollars` (`BigDecimal`)
- `totalDollars` (`BigDecimal`)
- `condition` (`String`)
- `lookedUpAt` (`LocalDateTime`)
- `detailsUrl` (`String`)
- `lookupError` (`String`)
- `lastModified` (`LocalDateTime`)

## BookPriceLookupResultDto

- `bookId` (`Long`)
- `bookTitle` (`String`)
- `success` (`boolean`)
- `rateLimited` (`boolean`)
- `cancelled` (`boolean`)
- `hardcover` (`BookPriceDto`)
- `softcover` (`BookPriceDto`)
- `libraryBinding` (`BookPriceDto`)
- `errorMessage` (`String`)

## BookSummaryDto

- `id` (`Long`)
- `lastModified` (`LocalDateTime`)
- `dateAddedToLibrary` (`LocalDateTime`)

## BookTags

- `bookId` (`Long`)
- `title` (`String`)
- `before` (`List<String>`)
- `after` (`List<String>`)

## BranchDto

- `id` (`Long`)
- `branchName` (`String`)
- `librarySystemName` (`String`)

## BranchStatisticsDto

- `branchId` (`Long`)
- `branchName` (`String`)
- `bookCount` (`Long`)
- `activeLoansCount` (`Long`)

## BulkDeleteFailureDto

- `id` (`Long`)
- `title` (`String`)
- `errorMessage` (`String`)

## BulkDeleteResultDto

- `deletedCount` (`int`)
- `failedCount` (`int`)
- `deletedIds` (`List<Long>`)
- `failures` (`List<BulkDeleteFailureDto>`)

## CatalogMatchRef

- `recordKey` (`String`)
- `url` (`String`)

## CatalogToolApplyRequestDto

- `changes` (`List<CatalogToolChangeDto>`)

## CatalogToolApplyResultDto

- `actionId` (`Long`)
- `actionKind` (`String`)
- `applied` (`int`)
- `skipped` (`int`)
- `message` (`String`)
- `error` (`String`)
- `changes` (`List<CatalogToolChangeDto>`)
- `skippedChanges` (`List<CatalogToolChangeDto>`)

## CatalogToolCandidatesDto

- `bookIds` (`List<Long>`)
- `authorIds` (`List<Long>`)

## CatalogToolChangeDto

- `bookId` (`Long`)
- `authorId` (`Long`)
- `title` (`String`)
- `before` (`String`)
- `after` (`String`)
- `added` (`String`)
- `reason` (`String`)

## CatalogToolPreviewDto

- `booksScanned` (`int`)
- `changeCount` (`int`)
- `message` (`String`)
- `error` (`String`)
- `changes` (`List<CatalogToolChangeDto>`)
- `skipped` (`List<CatalogToolChangeDto>`)

## CatalogToolSnapshotDto

- `changes` (`List<CatalogToolChangeDto>`)

## CatalogToolUndoRequestDto

- `actionId` (`Long`)

## CatalogToolUndoableDto

- `actionId` (`Long`)
- `changeCount` (`Integer`)
- `summary` (`String`)

## CheckoutCardTranscriptionDto

- `title` (`String`)
- `author` (`String`)
- `callNumber` (`String`)
- `lastDate` (`String`)
- `lastIssuedTo` (`String`)
- `lastDue` (`String`)

## CheckoutMatchDto

- `id` (`Long`)
- `title` (`String`)
- `author` (`String`)
- `locNumber` (`String`)
- `status` (`String`)

## ChunkUploadResultDto

- `uploadId` (`String`)
- `chunkIndex` (`int`)
- `processedPhotos` (`List<PhotoZipImportItemDto>`)
- `totalProcessedSoFar` (`int`)
- `totalSuccessSoFar` (`int`)
- `totalFailureSoFar` (`int`)
- `totalSkippedSoFar` (`int`)
- `complete` (`boolean`)
- `finalResult` (`PhotoZipImportResultDto`)
- `errorMessage` (`String`)

## ContentFilter

- `includedContentCategories` (`java.util.List<String>`)
- `excludedContentCategories` (`java.util.List<String>`)

## ContributorInfo

- `profilePictureBaseUrl` (`String`)
- `displayName` (`String`)

## CountDto

- `count` (`long`)

## CreateUserDto

- `username` (`String`)
- `password` (`String`)
- `authority` (`String`)
- `email` (`String`)
- `phone` (`String`)

## CurrentUserDto

- `id` (`Long`)
- `username` (`String`)
- `authority` (`String`)
- `ssoSubjectId` (`String`)
- `createdAt` (`LocalDateTime`)

## DatabaseStatsDto

- `branchCount` (`Long`)
- `bookCount` (`Long`)
- `authorCount` (`Long`)
- `userCount` (`Long`)
- `loanCount` (`Long`)
- `favoriteCount` (`Long`)
- `priceCount` (`Long`)

## Date

- `year` (`Integer`)
- `month` (`Integer`)
- `day` (`Integer`)

## DateFilter

- `ranges` (`java.util.List<DateRange>`)

## DateRange

- `startDate` (`Date`)
- `endDate` (`Date`)

## DuplicateTitleActionResultDto

- `actionId` (`Long`)
- `actionKind` (`String`)
- `summary` (`String`)

## DuplicateTitleDismissalRequestDto

- `titleA` (`String`)
- `authorA` (`String`)
- `titleB` (`String`)
- `authorB` (`String`)

## DuplicateTitlePairActionRequestDto

- `bookAId` (`Long`)
- `bookBId` (`Long`)

## DuplicateTitlePairDto

- `score` (`double`)
- `bookAId` (`Long`)
- `bookATitle` (`String`)
- `bookAAlternateTitles` (`List<String>`)
- `bookAAuthorName` (`String`)
- `bookAStatus` (`String`)
- `bookBId` (`Long`)
- `bookBTitle` (`String`)
- `bookBAlternateTitles` (`List<String>`)
- `bookBAuthorName` (`String`)
- `bookBStatus` (`String`)
- `matchedTitleA` (`String`)
- `matchedTitleB` (`String`)
- `matchedFieldA` (`String`)
- `matchedFieldB` (`String`)
- `bookAAuthorId` (`Long`)
- `bookBAuthorId` (`Long`)
- `bookAAuthorAlternateNames` (`List<String>`)
- `bookBAuthorAlternateNames` (`List<String>`)
- `bookALocNumber` (`String`)
- `bookBLocNumber` (`String`)
- `bookAElectronicResource` (`Boolean`)
- `bookBElectronicResource` (`Boolean`)
- `bookACatalogLabels` (`List<String>`)
- `bookBCatalogLabels` (`List<String>`)
- `titleDifference` (`String`)
- `authorDifference` (`String`)
- `titleAction` (`String`)
- `titleChangeNeeded` (`boolean`)
- `titlePlan` (`String`)
- `authorMergeAvailable` (`boolean`)
- `authorPlan` (`String`)

## DuplicateTitleUndoRequestDto

- `actionId` (`Long`)

## DuplicateTitlesResultDto

- `pairs` (`List<DuplicateTitlePairDto>`)
- `booksScanned` (`long`)
- `representativesCompared` (`long`)
- `message` (`String`)
- `error` (`String`)

## EmuLookupResultDto

- `bookId` (`Long`)
- `success` (`boolean`)
- `audioAvailable` (`Boolean`)
- `paperAvailable` (`Boolean`)
- `ebookAvailable` (`Boolean`)
- `matchedTitle` (`String`)
- `matches` (`List<CatalogMatchRef>`)
- `errorMessage` (`String`)

## ErrorResponse

- `error` (`String`)
- `message` (`String`)
- `timestamp` (`LocalDateTime`)
- `entityType` (`String`)
- `entityName` (`String`)
- `existingEntityId` (`Long`)

## ExportChunkDto

- `section` (`String`)
- `afterId` (`long`)
- `limit` (`int`)
- `nextAfterId` (`Long`)
- `total` (`long`)
- `count` (`int`)
- `hasMore` (`boolean`)
- `items` (`List<Object>`)

## ExportChunkPlanDto

- `targetChunks` (`int`)
- `pageSize` (`int`)
- `totalItems` (`long`)
- `sections` (`List<SectionPlan>`)

## FavoriteItemDto

- `itemType` (`FavoriteItemType`)
- `itemId` (`Long`)
- `selectedLists` (`List<String>`)
- `availableLists` (`List<String>`)

## FavoriteListCountDto

- `listName` (`String`)
- `bookCount` (`long`)
- `authorCount` (`long`)

## FavoriteListMembershipDto

- `listName` (`String`)
- `bookIds` (`List<Long>`)
- `authorIds` (`List<Long>`)

## FavoriteSummaryDto

- `favoriteBookIds` (`List<Long>`)
- `favoriteAuthorIds` (`List<Long>`)
- `lists` (`List<FavoriteListMembershipDto>`)
- `availableLists` (`List<String>`)

## FavoriteUpdateDto

- `itemType` (`FavoriteItemType`)
- `itemId` (`Long`)
- `listNames` (`List<String>`)

## FieldError

- `field` (`final String`)
- `message` (`final String`)

## Filters

- `dateFilter` (`DateFilter`)
- `contentFilter` (`ContentFilter`)
- `mediaTypeFilter` (`MediaTypeFilter`)

## FreeTextBulkLookupResultDto

- `bookId` (`Long`)
- `bookTitle` (`String`)
- `authorName` (`String`)
- `success` (`boolean`)
- `freeTextUrl` (`String`)
- `providerName` (`String`)
- `errorMessage` (`String`)
- `providersSearched` (`List<String>`)

## GenreChangeLineDto

- `kind` (`String`)
- `fromTag` (`String`)
- `toTag` (`String`)
- `titleCount` (`int`)
- `summary` (`String`)

## GenreCleanupSnapshotDto

- `books` (`List<BookTags>`)

## GenreLookupResultDto

- `bookId` (`Long`)
- `title` (`String`)
- `success` (`boolean`)
- `suggestedGenres` (`List<String>`)
- `errorMessage` (`String`)
- `updatedBook` (`BookDto`)

## GlobalSettingsDto

- `googleClientSecret` (`String`)
- `googleClientSecretPartial` (`String`)
- `googleClientSecretUpdatedAt` (`Instant`)
- `googleClientId` (`String`)
- `redirectUri` (`String`)
- `googleSsoClientId` (`String`)
- `googleSsoClientSecret` (`String`)
- `googleSsoClientSecretPartial` (`String`)
- `googleSsoCredentialsUpdatedAt` (`Instant`)
- `googleSsoClientSecretConfigured` (`boolean`)
- `googleSsoClientIdConfigured` (`boolean`)
- `lastUpdated` (`Instant`)
- `googleClientSecretConfigured` (`boolean`)
- `googleClientSecretValidation` (`String`)
- `googleSsoClientSecretValidation` (`String`)
- `emailMethod` (`EmailMethod`)
- `emailFromAddress` (`String`)
- `emailFromName` (`String`)
- `emailNotifyLibrariansOnPending` (`Boolean`)
- `emailNotifyApplicantOnPending` (`Boolean`)
- `emailLibrarianRecipients` (`String`)
- `emailIncludeLibrarianUserEmails` (`Boolean`)
- `emailNotifyLibrariansOnLoanChange` (`Boolean`)
- `emailNotifyBorrowerOnLoanChange` (`Boolean`)
- `smtpHost` (`String`)
- `smtpPort` (`Integer`)
- `smtpUsername` (`String`)
- `smtpUsernameSource` (`String`)
- `smtpPassword` (`String`)
- `smtpPasswordPartial` (`String`)
- `smtpPasswordConfigured` (`boolean`)
- `smtpPasswordSource` (`String`)
- `smtpStartTls` (`Boolean`)
- `smtpSsl` (`Boolean`)
- `sendGridApiKey` (`String`)
- `sendGridApiKeyPartial` (`String`)
- `sendGridApiKeyConfigured` (`boolean`)
- `webhookUrl` (`String`)
- `webhookBearerToken` (`String`)
- `webhookBearerTokenPartial` (`String`)
- `webhookBearerTokenConfigured` (`boolean`)
- `emailMethodConfigured` (`boolean`)
- `emailMethodStatus` (`String`)

## GrokJobDto

- `jobId` (`String`)
- `kind` (`String`)
- `status` (`String`)
- `result` (`Object`)
- `error` (`String`)
- `httpStatus` (`Integer`)

## GrokipediaLookupResultDto

- `bookId` (`Long`)
- `authorId` (`Long`)
- `name` (`String`)
- `success` (`boolean`)
- `grokipediaUrl` (`String`)
- `errorMessage` (`String`)

## IllegalGenresMaintenanceDto

- `booksAffected` (`long`)
- `booksScanned` (`long`)
- `booksUpdated` (`long`)
- `pluralCorrections` (`long`)
- `illegalRemoved` (`long`)
- `message` (`String`)
- `error` (`String`)
- `actionId` (`Long`)
- `changes` (`List<GenreChangeLineDto>`)

## ImportAuthorDto

- `name` (`String`)
- `alternateNames` (`List<String>`)
- `dateOfBirth` (`LocalDate`)
- `dateOfDeath` (`LocalDate`)
- `religiousAffiliation` (`String`)
- `birthCountry` (`String`)
- `nationality` (`String`)
- `briefBiography` (`String`)
- `grokipediaUrl` (`String`)
- `invalidGrokipediaUrls` (`List<RejectedUrl>`)

## ImportBookDto

- `title` (`String`)
- `alternateTitles` (`List<String>`)
- `alternateTitle` (`String`)
- `publicationYear` (`Integer`)
- `publisher` (`String`)
- `plotSummary` (`String`)
- `relatedWorks` (`String`)
- `detailedDescription` (`String`)
- `grokipediaUrl` (`String`)
- `invalidGrokipediaUrls` (`List<RejectedUrl>`)
- `freeTextUrl` (`String`)
- `invalidFreeTextUrls` (`List<RejectedUrl>`)
- `invalidFreeAudioUrls` (`List<RejectedUrl>`)
- `invalidHoldings` (`List<RejectedHolding>`)
- `freeTextLastSearched` (`LocalDateTime`)
- `dateAddedToLibrary` (`LocalDateTime`)
- `lastModified` (`LocalDateTime`)
- `status` (`BookStatus`)
- `locNumber` (`String`)
- `electronicResource` (`Boolean`)
- `statusReason` (`String`)
- `tagsList` (`List<String>`)
- `libraryName` (`String`)
- `authorName` (`String`)
- `author` (`ImportAuthorDto`)
- `ydlAudioAvailable` (`Boolean`)
- `ydlPaperAvailable` (`Boolean`)
- `ydlEbookAvailable` (`Boolean`)
- `ydlLastChecked` (`LocalDateTime`)
- `ydlLookupError` (`String`)
- `emuAudioAvailable` (`Boolean`)
- `emuPaperAvailable` (`Boolean`)
- `emuEbookAvailable` (`Boolean`)
- `emuLastChecked` (`LocalDateTime`)
- `emuLookupError` (`String`)
- `aclaAudioAvailable` (`Boolean`)
- `aclaPaperAvailable` (`Boolean`)
- `aclaEbookAvailable` (`Boolean`)
- `aclaLastChecked` (`LocalDateTime`)
- `aclaLookupError` (`String`)
- `wccAudioAvailable` (`Boolean`)
- `wccPaperAvailable` (`Boolean`)
- `wccEbookAvailable` (`Boolean`)
- `wccLastChecked` (`LocalDateTime`)
- `wccLookupError` (`String`)
- `msuAudioAvailable` (`Boolean`)
- `msuPaperAvailable` (`Boolean`)
- `msuEbookAvailable` (`Boolean`)
- `msuLastChecked` (`LocalDateTime`)
- `msuLookupError` (`String`)
- `readingDifficulty` (`ReadingDifficulty`)
- `binding` (`BookCoverType`)
- `desireToPurchase` (`Integer`)

## ImportCounts

- `branches` (`int`)
- `authors` (`int`)
- `users` (`int`)
- `books` (`int`)
- `loans` (`int`)
- `photos` (`int`)
- `favorites` (`int`)
- `prices` (`int`)
- `sellers` (`int`)
- `listings` (`int`)

## ImportErrorDto

- `entityType` (`String`)
- `entityName` (`String`)
- `errorMessage` (`String`)

## ImportFavoriteDto

- `username` (`String`)
- `listName` (`String`)
- `bookTitle` (`String`)
- `bookAuthorName` (`String`)
- `authorName` (`String`)

## ImportListingDto

- `bookTitle` (`String`)
- `bookAuthorName` (`String`)
- `vendorId` (`Long`)
- `abeListingId` (`Long`)
- `listingTitle` (`String`)
- `detailsUrl` (`String`)
- `priceDollars` (`BigDecimal`)
- `shippingDollars` (`BigDecimal`)
- `priorityShippingDollars` (`BigDecimal`)
- `condition` (`String`)
- `binding` (`BookCoverType`)
- `isbn13` (`String`)
- `quantity` (`Integer`)
- `lookedUpAt` (`LocalDateTime`)

## ImportLoanDto

- `loanDate` (`LocalDate`)
- `dueDate` (`LocalDate`)
- `returnDate` (`LocalDate`)
- `bookTitle` (`String`)
- `bookAuthorName` (`String`)
- `username` (`String`)
- `book` (`ImportBookDto`)
- `user` (`ImportUserDto`)

## ImportPhotoDto

- `imageChecksum` (`String`)
- `contentType` (`String`)
- `caption` (`String`)
- `bookTitle` (`String`)
- `bookAuthorName` (`String`)
- `authorName` (`String`)
- `photoOrder` (`Integer`)
- `permanentId` (`String`)
- `exportedAt` (`LocalDateTime`)
- `exportStatus` (`Photo.ExportStatus`)
- `exportErrorMessage` (`String`)

## ImportPriceDto

- `bookTitle` (`String`)
- `bookAuthorName` (`String`)
- `cover` (`BookCoverType`)
- `priceDollars` (`BigDecimal`)
- `shippingDollars` (`BigDecimal`)
- `condition` (`String`)
- `lookedUpAt` (`LocalDateTime`)
- `detailsUrl` (`String`)
- `lookupError` (`String`)

## ImportRequestDto

- `branches` (`List<BranchDto>`)
- `authors` (`List<ImportAuthorDto>`)
- `users` (`List<ImportUserDto>`)
- `books` (`List<ImportBookDto>`)
- `loans` (`List<ImportLoanDto>`)
- `photos` (`List<ImportPhotoDto>`)
- `favorites` (`List<ImportFavoriteDto>`)
- `prices` (`List<ImportPriceDto>`)
- `sellers` (`List<ImportSellerDto>`)
- `listings` (`List<ImportListingDto>`)

## ImportResponseDto

- `success` (`boolean`)
- `message` (`String`)
- `counts` (`ImportCounts`)
- `errors` (`List<ImportErrorDto>`)

## ImportResult

- `counts` (`ImportCounts`)
- `errors` (`List<ImportErrorDto>`)

## ImportSellerDto

- `vendorId` (`Long`)
- `name` (`String`)
- `city` (`String`)
- `region` (`String`)
- `country` (`String`)
- `starRating` (`Integer`)
- `storefrontUrl` (`String`)
- `sampleListingId` (`Long`)
- `sampleStandardShipping` (`BigDecimal`)
- `samplePriorityShipping` (`BigDecimal`)
- `shippingTerms` (`String`)
- `additionalShippingDollars` (`BigDecimal`)
- `additionalShippingSource` (`AdditionalShippingSource`)
- `lookedUpAt` (`LocalDateTime`)
- `ratesLookedUpAt` (`LocalDateTime`)

## ImportUserDto

- `username` (`String`)
- `password` (`String`)
- `xaiApiKey` (`String`)
- `googlePhotosApiKey` (`String`)
- `googlePhotosRefreshToken` (`String`)
- `googlePhotosTokenExpiry` (`String`)
- `googleClientSecret` (`String`)
- `googlePhotosAlbumId` (`String`)
- `lastPhotoTimestamp` (`String`)
- `ssoProvider` (`String`)
- `ssoSubjectId` (`String`)
- `email` (`String`)
- `phone` (`String`)
- `libraryCardDesign` (`LibraryCardDesign`)
- `authorities` (`List<String>`)
- `roles` (`List<String>`)
- `userIdentifier` (`String`)

## LabelCountDto

- `label` (`String`)
- `count` (`long`)

## LibraryCardDesignDto

- `name` (`String`)
- `displayName` (`String`)
- `description` (`String`)
- `imageUrl` (`String`)

## LoanDto

- `id` (`Long`)
- `bookId` (`Long`)
- `bookTitle` (`String`)
- `userId` (`Long`)
- `userName` (`String`)
- `loanDate` (`LocalDate`)
- `dueDate` (`LocalDate`)
- `returnDate` (`LocalDate`)
- `lastModified` (`LocalDateTime`)
- `photoId` (`Long`)
- `photoChecksum` (`String`)

## LocLookupResultDto

- `bookId` (`Long`)
- `success` (`boolean`)
- `locNumber` (`String`)
- `errorMessage` (`String`)
- `matchCount` (`int`)
- `aiSuggested` (`boolean`)

## LoginRequestDto

- `username` (`String`)
- `password` (`String`)

## MediaItem

- `description` (`String`)
- `filename` (`String`)

## MediaItemResponse

- `id` (`String`)
- `description` (`String`)
- `filename` (`String`)
- `mimeType` (`String`)
- `baseUrl` (`String`)
- `productUrl` (`String`)
- `mediaMetadata` (`MediaMetadata`)
- `contributorInfo` (`ContributorInfo`)

## MediaItemResult

- `status` (`Status`)
- `mediaItem` (`MediaItemResponse`)

## MediaItemUpdate

- `mediaItemId` (`String`)
- `updateMask` (`String`)
- `mediaItem` (`MediaItem`)

## MediaMetadata

- `creationTime` (`String`)
- `width` (`String`)
- `height` (`String`)
- `photo` (`Photo`)
- `video` (`Video`)

## MediaTypeFilter

- `mediaTypes` (`java.util.List<String>`)

## MsuLookupResultDto

- `bookId` (`Long`)
- `success` (`boolean`)
- `audioAvailable` (`Boolean`)
- `paperAvailable` (`Boolean`)
- `ebookAvailable` (`Boolean`)
- `matchedTitle` (`String`)
- `matches` (`List<CatalogMatchRef>`)
- `errorMessage` (`String`)

## NameNormalizationResultDto

- `bookId` (`Long`)
- `title` (`String`)
- `before` (`String`)
- `after` (`String`)
- `changed` (`boolean`)
- `success` (`boolean`)
- `errorMessage` (`String`)
- `updatedBook` (`BookDto`)

## NewMediaItem

- `description` (`String`)
- `simpleMediaItem` (`SimpleMediaItem`)

## NewMediaItemResult

- `uploadToken` (`String`)
- `status` (`Status`)
- `mediaItem` (`MediaItem`)

## PageInfoDto

- `totalPages` (`int`)
- `totalElements` (`long`)
- `currentPage` (`int`)
- `pageSize` (`int`)

## Photo

- `cameraMake` (`String`)
- `cameraModel` (`String`)
- `focalLength` (`Double`)
- `apertureFNumber` (`Double`)
- `isoEquivalent` (`Integer`)

## PhotoDto

- `id` (`Long`)
- `contentType` (`String`)
- `caption` (`String`)
- `bookId` (`Long`)
- `authorId` (`Long`)
- `loanId` (`Long`)
- `imageChecksum` (`String`)
- `dateTaken` (`LocalDateTime`)

## PhotoExportInfoDto

- `id` (`Long`)
- `lastModified` (`LocalDateTime`)
- `caption` (`String`)
- `exportStatus` (`String`)
- `exportedAt` (`LocalDateTime`)
- `permanentId` (`String`)
- `exportErrorMessage` (`String`)
- `contentType` (`String`)
- `hasImage` (`Boolean`)
- `checksum` (`String`)
- `bookTitle` (`String`)
- `bookId` (`Long`)
- `bookLocNumber` (`String`)
- `bookDateAdded` (`LocalDateTime`)
- `bookAuthorId` (`Long`)
- `bookAuthorName` (`String`)
- `authorName` (`String`)
- `authorId` (`Long`)

## PhotoExportResponseDto

- `message` (`String`)
- `photoId` (`Long`)
- `stats` (`PhotoExportStatsDto`)

## PhotoExportStatsDto

- `total` (`Long`)
- `exported` (`Long`)
- `imported` (`Long`)
- `pendingExport` (`Long`)
- `pendingImport` (`Long`)
- `failed` (`Long`)
- `inProgress` (`Long`)
- `completed` (`Long`)
- `pending` (`Long`)
- `albumName` (`String`)
- `albumId` (`String`)

## PhotoImportResultDto

- `message` (`String`)
- `imported` (`Integer`)
- `failed` (`Integer`)

## PhotoSummaryDto

- `id` (`Long`)
- `lastModified` (`LocalDateTime`)

## PhotoVerifyResultDto

- `photoId` (`Long`)
- `permanentId` (`String`)
- `valid` (`Boolean`)
- `message` (`String`)
- `filename` (`String`)
- `mimeType` (`String`)

## PhotoZipImportItemDto

- `filename` (`String`)
- `status` (`String`)
- `entityType` (`String`)
- `entityName` (`String`)
- `entityId` (`Long`)
- `photoId` (`Long`)
- `errorMessage` (`String`)

## PhotoZipImportResultDto

- `totalFiles` (`int`)
- `successCount` (`int`)
- `failureCount` (`int`)
- `skippedCount` (`int`)
- `items` (`List<PhotoZipImportItemDto>`)

## PhotoZipPartDto

- `partNumber` (`int`)
- `totalParts` (`int`)
- `rangeLabel` (`String`)
- `photoCount` (`int`)
- `estimatedMb` (`long`)
- `startKey` (`String`)
- `endKey` (`String`)

## ReadingDifficultyLookupResultDto

- `bookId` (`Long`)
- `title` (`String`)
- `success` (`boolean`)
- `suggestedDifficulty` (`ReadingDifficulty`)
- `errorMessage` (`String`)
- `updatedBook` (`BookDto`)

## ResumeInfoDto

- `uploadId` (`String`)
- `resumeFromChunkIndex` (`int`)
- `bytesToSkipInChunk` (`long`)
- `totalProcessed` (`int`)
- `successCount` (`int`)
- `failureCount` (`int`)
- `skippedCount` (`int`)

## SavedBookDto

- `id` (`Long`)
- `title` (`String`)
- `authorId` (`Long`)
- `author` (`String`)
- `library` (`String`)
- `photoCount` (`Long`)
- `needsProcessing` (`Boolean`)
- `locNumber` (`String`)
- `status` (`String`)
- `grokipediaUrl` (`String`)

## SearchRequest

- `albumId` (`String`)
- `pageSize` (`Integer`)
- `pageToken` (`String`)
- `filters` (`Filters`)

## SearchResponse

- `mediaItems` (`List<MediaItemResponse>`)
- `nextPageToken` (`String`)

## SearchResponseDto

- `books` (`List<BookDto>`)
- `authors` (`List<AuthorDto>`)
- `bookPage` (`PageInfoDto`)
- `authorPage` (`PageInfoDto`)

## SectionPlan

- `section` (`String`)
- `total` (`long`)

## SellerReparseResultDto

- `examined` (`int`)
- `updated` (`int`)
- `unchanged` (`int`)
- `keptLibrarian` (`int`)

## SimpleMediaItem

- `uploadToken` (`String`)

## Status

- `code` (`Integer`)
- `message` (`String`)

## TestDataResponseDto

- `success` (`boolean`)
- `message` (`String`)

## TestDataStatsDto

- `books` (`long`)
- `authors` (`long`)
- `loans` (`long`)
- `users` (`long`)

## TestEmailRequest

- `to` (`String`)

## TestEmailResultDto

- `sent` (`boolean`)
- `method` (`EmailMethod`)
- `message` (`String`)
- `recipients` (`List<String>`)

## TitleLoanedDto

- `bookId` (`Long`)
- `title` (`String`)
- `loaned` (`boolean`)

## UserDto

- `id` (`Long`)
- `username` (`String`)
- `authorities` (`Set<String>`)
- `xaiApiKey` (`String`)
- `googlePhotosApiKey` (`String`)
- `googlePhotosRefreshToken` (`String`)
- `googlePhotosTokenExpiry` (`String`)
- `googleClientSecret` (`String`)
- `googlePhotosAlbumId` (`String`)
- `lastPhotoTimestamp` (`String`)
- `ssoProvider` (`String`)
- `ssoSubjectId` (`String`)
- `email` (`String`)
- `phone` (`String`)
- `receiveEmails` (`boolean`)
- `libraryCardDesign` (`LibraryCardDesign`)
- `activeLoansCount` (`int`)
- `createdAt` (`LocalDateTime`)
- `lastModified` (`LocalDateTime`)

## UserSettingsDto

- `username` (`String`)
- `currentPassword` (`String`)
- `password` (`String`)
- `xaiApiKey` (`String`)
- `googlePhotosApiKey` (`String`)
- `googlePhotosRefreshToken` (`String`)
- `googlePhotosTokenExpiry` (`String`)
- `googleClientSecret` (`String`)
- `googlePhotosAlbumId` (`String`)
- `lastPhotoTimestamp` (`String`)
- `libraryCardDesign` (`LibraryCardDesign`)
- `email` (`String`)
- `phone` (`String`)
- `receiveEmails` (`Boolean`)

## ValidationErrorResponse

- `fieldErrors` (`List<FieldError>`)

## Video

- `cameraMake` (`String`)
- `cameraModel` (`String`)
- `fps` (`Double`)
- `status` (`String`)

## WccLookupResultDto

- `bookId` (`Long`)
- `success` (`boolean`)
- `audioAvailable` (`Boolean`)
- `paperAvailable` (`Boolean`)
- `ebookAvailable` (`Boolean`)
- `matchedTitle` (`String`)
- `matches` (`List<CatalogMatchRef>`)
- `errorMessage` (`String`)

## YdlLookupResultDto

- `bookId` (`Long`)
- `success` (`boolean`)
- `audioAvailable` (`Boolean`)
- `paperAvailable` (`Boolean`)
- `ebookAvailable` (`Boolean`)
- `matchedTitle` (`String`)
- `matches` (`List<CatalogMatchRef>`)
- `errorMessage` (`String`)

## Enums

- `AdditionalShippingSource`: `PARSED`, `LIBRARIAN`, `FREE`, `UNKNOWN`
- `ApplicationStatus`: `PENDING`, `APPROVED`, `NOT_APPROVED`, `QUESTION`
- `BookCoverType`: `HARDCOVER`, `SOFTCOVER`, `LIBRARY_BINDING`, `OTHER`, `UNKNOWN`
- `BookStatus`: `ACTIVE`, `LOST`, `WITHDRAWN`, `ON_ORDER`, `REQUESTED`
- `BookStatusFilter`: `IN_LIBRARY`, `ELECTRONIC_RESOURCE`, `WITHOUT_LOC`, `LOST`, `WITHDRAWN`, `ON_ORDER`, `REQUESTED`
- `EmailMethod`: `DISABLED`, `LOG`, `SMTP`, `SENDGRID`, `WEBHOOK`
- `ExportStatus`: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`
- `FavoriteItemType`: `BOOK`, `AUTHOR`
- `LibraryCardDesign`: `COUNTRYSIDE_YOUTH`, `SACRED_HEART_PORTRAIT`, `RADIANT_BLESSING`, `PATRON_OF_CREATURES`, `CLASSICAL_DEVOTION`, `CLARES_LIBRARY_CARD`, `GRANDFATHER_LIBRARY_CARD`, `GRANDFATHER`, `LITTLE_FLOWER_BANK`, `LITTLE_FLOWER`, `MOTHER_ANGELICA`, `BLUE_AND_GOLD`, `FLEUR_DE_LIS_COLORFUL`, `SACRED_HEART_FLAMES`, `SACRED_HEART_BLOOD_RED`, `SACRED_HEART_ORNATE`, `MAROON_ON_PARCHMENT`, `MAROON_AND_GOLD_RADIAL`, `SACRED_HEART_COLORFUL`, `SACRED_HEART_THORNS`, `ST_MARTIN_SERVUS_PAUPERUM`, `ST_MARTIN_BROOM`, `ST_MARTIN_BROOMS_CROSSES`, `ST_MARTIN_MAROON`, `VERY_COLORFUL_ORNATE`
- `PublicLibrary`: `YDL`, `EMU`, `ACLA`, `WCC`, `MSU`
- `ReadingDifficulty`: `CHILDREN`, `ACCESSIBLE`, `MODERATE`, `DEMANDING`, `ADVANCED`, `UNSET`
