# comic-reading-companion
Android companion app for creating and tracking comic reading orders

## Development Roadmap

Comic Reading Companion is intended to be a publisher-agnostic Android app for
building, following, and tracking comic-book reading orders.

### Core Data Model

- [x] Publisher database model
- [x] Universe / continuity model
- [x] Series model
- [x] Issue model
- [x] Issue types
  - Regular issues
  - Annuals
  - Specials
  - One-shots
  - Giant-size issues
  - Previews
- [x] External issue ID model
- [x] Reading-list model
- [x] Reading-list item model
- [x] Reading-list section model
- [x] Reading-progress model
- [x] Global issue read status across reading lists
- [x] Add database uniqueness constraints where appropriate
- [x] Add proper Room migrations as the schema evolves
- [ ] Add support for relationships between reading orders / events
- [x] Add collection / library concepts

### Reading Lists

- [x] Display available reading lists on the home screen
- [x] Open a reading-list detail screen
- [x] Display issues in reading-order sequence
- [x] Support large reading lists with lazy scrolling
- [x] Show reading-list description
- [x] Show each issue's reading-order position
- [x] Support required / optional items in the data model
- [x] Support notes on reading-list items in the data model
- [x] Display required / optional status in the UI
- [x] Display reading-list item notes in the UI
- [x] Reading-list sections / story arcs
- [x] Collapsible reading-list sections
- [x] Create custom reading lists
- [x] Edit reading-list title and description
- [x] Add issues to a reading list
- [x] Remove issues from a reading list
- [x] Reorder reading-list items
- [x] Move issues between reading-list sections
- [x] Reorder reading-list sections and move issues across section boundaries
- [x] Delete reading lists
- [x] Duplicate / copy a reading list
- [x] Search within a reading list
- [x] Filter reading lists by read status, series, or required status

### Reading Progress

- [x] Track unread issues
- [x] Track reading issues in the database
- [x] Track completed / read issues
- [x] Persist reading progress between app launches
- [x] Toggle issues between read and unread
- [x] Calculate completed issue count
- [x] Calculate completion percentage
- [x] Display a reading-list progress bar
- [x] Automatically update progress through Room Flow
- [x] Jump to the first unread issue
- [x] Expose the "Reading" status in the UI
- [x] "Continue Reading" action
- [x] Automatically open / scroll to the current reading position
- [x] Select multiple issues and mark as read
- [x] Select multiple issues and mark as unread
- [x] Mark everything before a selected issue as read
- [x] Reset reading-list progress
- [x] Mark entire reading-list as read
- [x] Display date started
- [x] Display date completed
- [ ] User notes on reading progress
- [x] Reading-history screen

### Issue and Series Browsing

- [x] Issue detail screen
- [x] Series detail screen
- [x] Display issue cover artwork
- [x] Display issue title
- [x] Display publication / cover date
- [x] Display issue description
- [x] Display issue type
- [x] Display universe / continuity
- [x] Browse issues by series
- [x] Browse series by publisher
- [x] Search issues and series
- [x] Filter by publisher
- [x] Filter by universe / continuity
- [x] Filter by publication year
- [x] Show an issue's appearances across multiple reading lists

### Reading-List Import System

- [x] Import reading lists from JSON assets
- [x] Parse publishers from imported data
- [x] Parse universes from imported data
- [x] Parse series and issue metadata
- [x] Reuse existing database records during import
- [x] Update changed series metadata
- [x] Update changed issue metadata
- [x] Update reading-list positions and notes
- [x] Remove stale reading-list entries when JSON changes
- [x] Preserve issue reading progress when reading-list JSON changes
- [x] Make imports idempotent
- [x] Import all bundled reading-list files automatically
- [x] Add importer transactions
- [x] Add stronger JSON validation and useful error reporting
- [x] Detect duplicate reading-list positions
- [x] Detect duplicate issues within a reading list
- [x] Validate missing / invalid series metadata
- [x] Support optional reading-list sections in JSON
- [x] Support per-item universe / continuity for multiverse reading lists
- [x] Support external IDs in reading-list JSON
- [x] Support importing user-provided reading-list files
- [x] Export reading lists to JSON

### Included Reading Orders

- [x] Spider-Man Volume 2 Era — Earth-616
  - September 1998 through September 2003
  - 213 issues
- [x] Ultimate Marvel (2000–2015) — Earth-1610
  - Complete original Ultimate Universe
  - 127 story arcs / sections
  - 714 issues
- [ ] Additional Spider-Man eras
- [ ] Larger Spider-Man master chronology
- [ ] Additional Marvel characters / events
- [ ] DC reading orders
- [ ] Image / independent publisher reading orders

### External Comic-Service Integration

- [x] External ID database architecture
- [x] Store Comic Vine identifiers for issues
- [x] Development-time Comic Vine metadata / cover enrichment
- [ ] Store Marvel Unlimited identifiers for issues
- [ ] "Read in Marvel Unlimited" action
- [ ] Deep-link to an issue in Marvel Unlimited where possible
- [ ] Other external comic-service links
- [ ] Automatically sync reading status with external services if an official,
  supported integration becomes available
- [x] Securely store the user-provided Comic Vine API key
    using Android Keystore-backed encryption
- [x] Exclude Comic Vine API-key storage from app backups
- [x] Validate user-provided Comic Vine API keys against the live API
- [x] Search external series catalogs using the configured API key
- [x] Display paginated series-search results
- [x] Rank series-search candidates by title, year, publisher, and expected issue count
- [x] Provide optional matching criteria in a collapsible search section
- [x] Fetch detailed metadata for a selected external series
- [x] Retrieve the complete issue list across paginated API responses
- [x] Display a series review screen with metadata and retrieved issues
- [x] Navigate from search results to series review
- [x] Test review-screen rendering and result selection
- [x] Infer common issue types from Comic Vine issue labels during import

> Marvel Unlimited does not currently expose a documented public API for
> third-party reading-history synchronization. The app should not depend on
> scraping or private Marvel APIs.

### Pull List and Release Planning

**Milestone 1 — Release-Data Foundation**

- [x] Evaluate Metron as the primary release-data provider
- [x] Define a provider interface so additional release sources can be added later
- [x] Securely store user-provided release-provider credentials using Android Keystore-backed encryption
- [x] Exclude release-provider credentials from app backups
- [x] Define local data models for scheduled releases, including store-release date, FOC date, source identifiers, and last-verified timestamp
- [x] Cache release data locally and support incremental refreshes
- [x] Match upcoming releases to existing catalog records using external identifiers first
- [x] Support upcoming releases that do not yet have a local Comic Vine issue record
- [ ] Handle ambiguous matches, changed dates, delayed releases, and canceled releases
- [x] Preserve source attribution and comply with provider data licenses and terms

**Milestone 2 — Personal Pull List and Upcoming Releases**

- [x] Create a dedicated Pull List screen
- [x] Add and remove ongoing series from the personal pull list
- [ ] Support explicitly tracking individual issues, one-shots, and special releases
- [ ] Display upcoming releases grouped by scheduled store-release date
- [ ] Display scheduled FOC deadlines separately from store-release dates
- [ ] Show issue details, publisher, scheduled date, and price when available
- [ ] Highlight upcoming releases associated with the personal pull list
- [ ] Show release-data freshness and the source of scheduled dates

**Milestone 3 — Store-Pickup Workflow**

- [ ] Add a "Since Your Last Pickup" view
- [ ] Record a pickup checkpoint to identify newly due releases
- [ ] Track pickup status: To Collect, Picked Up, Not Available, and Skipped
- [ ] Carry outstanding and unavailable releases forward to subsequent trips
- [ ] Preserve pickup status when release dates change
- [ ] Mark matched issues as owned when picked up
- [ ] Keep release scheduling, pickup status, reading progress, and collection ownership distinct
- [ ] Test pickup checkpoints, outstanding items, and ownership updates

**Milestone 4 — Release-Data Coverage and Corrections**

- [ ] Support importing distributor release data from CSV or Excel files
- [ ] Allow manual creation and correction of release records
- [ ] Allow review and resolution of conflicting release dates
- [ ] Support supplemental release providers without coupling the UI to a specific service
- [ ] Test release imports, matching, refreshes, date changes, and duplicate detection

### Comic Print Visual Design System

- [x] Replace starter Material palette with the Comic Print color system
- [x] Use app-controlled colors instead of dynamic system colors by default
- [x] Establish comic-inspired display typography and readable body typography
- [x] Define reusable spacing, border, shadow, and corner-shape design tokens
- [x] Add reusable comic-style section headers and banners
- [x] Add reusable comic-style action panels and cards
- [x] Add reusable progress indicators that fit the Comic Print style
- [x] Add subtle halftone / ink-texture decoration without reducing readability
- [x] Add original app-owned placeholder / decorative artwork
- [x] Maintain accessible contrast and touch targets beneath stylized visuals
- [x] Create a coordinated dark-theme treatment
- [x] Apply the design system consistently across all screens

### App Navigation and Screen Structure

- [x] Add persistent top-level navigation for Home, Library, Browse, and Collection
- [x] Add a dedicated Library screen
- [x] Add a dedicated Collection screen
- [x] Add a Settings screen
- [x] Open Settings from the Home header gear icon
- [x] Hide top-level navigation on detail / editing screens
- [x] Preserve normal back navigation from detail screens
- [x] Move reading-list management actions to the Library where appropriate
- [x] Move reading-list import from Home to the Library as a secondary action
- [x] Navigate from Settings to series search and back

### Home Screen

- [x] Reading-list cards
- [x] Navigate from a card to its reading list
- [x] Show progress on each reading-list card
- [x] Show issue count on each card
- [x] Show current / next issue on each card
- [x] Sort reading lists
- [x] Search reading lists
- [x] Track recently opened reading lists for Continue Reading prioritization
- [x] Dedicated Continue Reading section
- [x] Replace the current Home layout with the Bright & Playful Comic Print design
- [x] Add comic-style Home header with Settings gear
- [x] Add prominent Browse Comics action panel
- [x] Add prominent Create Reading List action panel
- [x] Add Continue Reading hero card
- [x] Show Recently Read issues as a three-card row
- [x] Remove the full reading-list library from Home once Library is available
- [x] Remove search / sorting controls from Home once Library is available
- [x] Reading-list cover / hero artwork
- [x] Add original placeholder artwork when reading-list artwork is unavailable

### Library Screen

- [x] Display all reading lists
- [x] Search reading lists
- [x] Sort reading lists
- [x] Open a reading list
- [x] Create a reading list
- [x] Import a reading list as a secondary management action
- [ ] Add library-focused filtering as needed
- [x] Use Comic Print reading-list cards and progress treatment
- [x] Support reading-list cover / hero artwork when available

### Reading-List Detail UI

- [x] Top app bar
- [x] Back navigation
- [x] Description
- [x] Progress summary
- [x] Progress indicator
- [x] Scrollable issue list
- [x] Read / unread checkboxes
- [x] Preserve reading-list ordering
- [x] Jump to current / first unread
- [x] Better issue-row visual design
- [x] Issue cover thumbnails
- [x] Required / optional indicators
- [x] Story / arc information
- [x] Publication date
- [ ] Expandable notes
- [x] Tap issue to open issue details
- [x] Persistent reading-progress header
- [x] Scroll-to-top action
- [x] Jump to a specific issue / reading-order number
- [x] Apply Comic Print visual treatment to headers, progress, sections, and rows
- [x] Search issues within a reading list
- [x] Filter issues by reading status, list status, and series
- [x] Bulk-select issues and mark them read / unread
- [x] Collapse and expand reading-list sections
- [x] Comic Print menus, confirmation dialogs, forms, and section picker

### Browse and Detail Screen Visual Refresh

- [x] Apply Comic Print styling to Browse
- [x] Apply Comic Print styling to Publisher detail
- [x] Apply Comic Print styling to Series detail
- [x] Apply Comic Print styling to Issue detail
- [x] Apply Comic Print styling to create / edit forms
- [x] Apply Comic Print styling to dialogs, menus, and empty states
- [x] Keep data-dense screens visually quieter than Home for readability

### Publisher and Continuity Support

- [x] Publisher-independent database design
- [x] Universe / continuity stored separately from publisher
- [x] Marvel Earth-616 support
- [x] Marvel Earth-1610 support
- [x] Multi-universe reading lists
- [ ] DC universe / continuity support
- [ ] Image and independent publisher support
- [ ] Publisher-specific metadata adapters where needed

### Data Management

- [x] User data export
- [x] User data import / restore
- [x] Reading-progress backup
- [x] Reading-list backup
- [ ] Database integrity checks
- [ ] Safe upgrade / migration path between app versions
- [ ] Optional cloud backup / synchronization

### Quality and Reliability

- [x] DAO tests
- [x] Repository tests
- [x] JSON parser tests
- [x] Reading-list importer tests
- [x] Progress-tracking tests
- [x] ViewModel tests
- [x] Compose UI tests
- [x] Add accessible semantic roles to interactive controls with UI regression tests
- [x] App navigation integration tests
- [x] Test database migrations
- [x] Handle malformed reading-list files gracefully
- [x] Handle importer failures without partially updating a list
- [x] Improve error reporting / logging
- [x] Add UI regression coverage for top-level Home / Browse / Library navigation
- [x] Add UI coverage for the redesigned Home and Library screens
- [x] Add UI regression coverage for Collection navigation and owned-issue screens
- [x] Add tests for secure Comic Vine API-key storage
- [x] Add UI regression coverage for Settings navigation and API-key management
- [x] Test Comic Vine API-key validation, including invalid keys and network failures
- [x] Test external series search UI, pagination, and navigation
- [x] Test candidate ranking and optional series matching criteria

### Longer-Term Ideas

- [ ] Reading-order relationships between events and sub-events
- [ ] Character-based reading orders
- [ ] Event-based reading orders
- [x] Personal comic collection tracking
- [x] Owned / unowned issue status
- [ ] Wanted / missing issue status
- [ ] Reading recommendations
- [ ] Reading statistics
- [ ] Completed reading-list archive
- [ ] Shareable reading lists
- [ ] Import community-created reading orders
