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
- [ ] Add collection / library concepts

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
- [ ] Display date started
- [ ] Display date completed
- [ ] User notes on reading progress
- [ ] Reading-history screen

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
- [ ] Filter by publisher
- [ ] Filter by universe / continuity
- [ ] Filter by publication year
- [ ] Show an issue's appearances across multiple reading lists

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

> Marvel Unlimited does not currently expose a documented public API for
> third-party reading-history synchronization. The app should not depend on
> scraping or private Marvel APIs.

### Comic Print Visual Design System

- [ ] Replace starter Material palette with the Comic Print color system
- [ ] Use app-controlled colors instead of dynamic system colors by default
- [ ] Establish comic-inspired display typography and readable body typography
- [ ] Define reusable spacing, border, shadow, and corner-shape design tokens
- [ ] Add reusable comic-style section headers and banners
- [ ] Add reusable comic-style action panels and cards
- [ ] Add reusable progress indicators that fit the Comic Print style
- [ ] Add subtle halftone / ink-texture decoration without reducing readability
- [ ] Add original app-owned placeholder / decorative artwork
- [ ] Maintain accessible contrast and touch targets beneath stylized visuals
- [ ] Create a coordinated dark-theme treatment
- [ ] Apply the design system consistently across all screens

### App Navigation and Screen Structure

- [ ] Add persistent top-level navigation for Home, Browse, and Library
- [ ] Add a dedicated Library screen
- [ ] Add a Settings screen
- [ ] Open Settings from the Home header gear icon
- [ ] Hide top-level navigation on detail / editing screens
- [ ] Preserve normal back navigation from detail screens
- [ ] Move reading-list management actions to the Library where appropriate
- [ ] Move reading-list import from Home to the Library as a secondary action

### Home Screen

- [x] Reading-list cards
- [x] Navigate from a card to its reading list
- [x] Show progress on each reading-list card
- [x] Show issue count on each card
- [x] Show current / next issue on each card
- [x] Sort reading lists
- [x] Search reading lists
- [x] Recently opened reading lists
- [x] Dedicated Continue Reading section
- [ ] Replace the current Home layout with the Bright & Playful Comic Print design
- [ ] Add comic-style Home header with Settings gear
- [ ] Add prominent Browse Comics action panel
- [ ] Add prominent Create Reading List action panel
- [ ] Add Continue Reading hero card
- [ ] Show Recently Opened as a horizontal card row
- [ ] Remove the full reading-list library from Home once Library is available
- [ ] Remove search / sorting controls from Home once Library is available
- [ ] Reading-list cover / hero artwork
- [ ] Add original placeholder artwork when reading-list artwork is unavailable

### Library Screen

- [ ] Display all reading lists
- [ ] Search reading lists
- [ ] Sort reading lists
- [ ] Open a reading list
- [ ] Create a reading list
- [ ] Import a reading list as a secondary management action
- [ ] Add library-focused filtering as needed
- [ ] Use Comic Print reading-list cards and progress treatment
- [ ] Support reading-list cover / hero artwork when available

### Reading-List Detail UI

- [x] Top app bar
- [x] Back navigation
- [x] Description
- [x] Progress summary
- [x] Progress indicator
- [x] Scrollable issue list
- [x] Read / unread checkboxes
- [x] Reading-order numbers
- [x] Jump to first unread
- [ ] Better issue-row visual design
- [x] Issue cover thumbnails
- [x] Required / optional indicators
- [x] Story / arc information
- [x] Publication date
- [ ] Expandable notes
- [x] Tap issue to open issue details
- [x] Persistent reading-progress header
- [ ] Scroll-to-top action
- [ ] Jump to a specific issue / reading-order number
- [ ] Apply Comic Print visual treatment to headers, progress, sections, and rows

### Browse and Detail Screen Visual Refresh

- [ ] Apply Comic Print styling to Browse
- [ ] Apply Comic Print styling to Publisher detail
- [ ] Apply Comic Print styling to Series detail
- [ ] Apply Comic Print styling to Issue detail
- [ ] Apply Comic Print styling to create / edit forms
- [ ] Apply Comic Print styling to dialogs, menus, and empty states
- [ ] Keep data-dense screens visually quieter than Home for readability

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

- [ ] User data export
- [ ] User data import / restore
- [ ] Reading-progress backup
- [ ] Reading-list backup
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
- [x] App navigation integration tests
- [x] Test database migrations
- [x] Handle malformed reading-list files gracefully
- [x] Handle importer failures without partially updating a list
- [x] Improve error reporting / logging
- [ ] Add UI regression coverage for top-level Home / Browse / Library navigation
- [ ] Add UI coverage for the redesigned Home and Library screens

### Longer-Term Ideas

- [ ] Reading-order relationships between events and sub-events
- [ ] Character-based reading orders
- [ ] Event-based reading orders
- [ ] Personal comic collection tracking
- [ ] Owned / wanted / missing issue status
- [ ] Reading recommendations
- [ ] Reading statistics
- [ ] Completed reading-list archive
- [ ] Shareable reading lists
- [ ] Import community-created reading orders
