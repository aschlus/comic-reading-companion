Find the Comic Vine volume ID

If you already know the Comic Vine volume ID, skip this step.

Otherwise, create a small temporary manifest, for example:

tools/comicvine/catalog_configs/my_new_series_search.json

{
"series": [
{
"title": "My New Series",
"volume": 1,
"startYear": 2004,
"expectedIssueCount": 12
}
]
}

Then from the project root run:

python ./tools/comicvine/search_unmapped_series.py --manifest tools/comicvine/catalog_configs/my_new_series_search.json

That searches Comic Vine and prints the best candidate volumes with IDs, years, issue counts, and publishers. It also writes a detailed candidate report into tools/comicvine/output.

Your Comic Vine API key can either be in the COMIC_VINE_API_KEY environment variable or in:

tools/comicvine/.comicvine_api_key

That file, along with the cache and output directories, is gitignored.

Create the actual catalog config

Once you know the correct Comic Vine volume ID, create a permanent config such as:

tools/comicvine/catalog_configs/my_new_series.json

For a normal Earth-616 Marvel series, the basic shape is:

{
"publisher": "Marvel Comics",
"universes": [
{
"name": "Marvel Universe",
"designation": "Earth-616",
"description": null
}
],
"series": [
{
"title": "My New Series",
"volume": 1,
"startYear": 2004,
"endYear": 2005,
"comicVineVolumeId": 12345,
"defaultUniverseDesignation": "Earth-616",
"defaultIssueType": "REGULAR"
}
]
}

That is the same structure used by your existing single-series configs such as slingers.json.

The valid default issue types supported by the builder are:

REGULAR
ANNUAL
SPECIAL
ONE_SHOT
GIANT_SIZE
PREVIEW

The builder also supports per-issue overrides and more complicated issueMappings when Comic Vine’s structure doesn’t map cleanly to your local series.

Cache all issues for that Comic Vine volume

Run:

python ./tools/comicvine/cache_mapped_volume_issues.py --config tools/comicvine/catalog_configs/my_new_series.json

This reads the configured Comic Vine volume ID, downloads every issue for that volume, and stores it as:

tools/comicvine/cache/issues/volume_12345.json

If the cache already exists, it reports a cache hit rather than downloading it again.

A useful thing to remember: if you ever deliberately need fresh Comic Vine data for a volume, delete that specific cached volume_<id>.json file first and rerun the cache command.

Build a preview catalog

Run:

python ./tools/comicvine/build_catalog.py `
  --config tools/comicvine/catalog_configs/my_new_series.json `
--output tools/comicvine/output/my_new_series_catalog.json

Or as one line:

python ./tools/comicvine/build_catalog.py --config tools/comicvine/catalog_configs/my_new_series.json --output tools/comicvine/output/my_new_series_catalog.json

The builder converts the cached Comic Vine data into the app’s catalog format. It brings over things such as:

issue number
issue title
publication month
cover image URL
Comic Vine external ID
Comic Vine detail URL
configured universe
configured issue type

It also validates duplicate issue numbers, duplicate external IDs, valid issue types, and valid universe designations before writing the output.

Inspect the preview before touching the real catalog

Open:

tools/comicvine/output/my_new_series_catalog.json

I would specifically check:

series title
volume number
start/end year
number of issues
first few issue numbers
last few issue numbers
annuals/specials that may have been included unexpectedly
publication dates
cover URLs
universe designation
issue types

This is the point where you catch Comic Vine oddities before merging anything into the app.

For a straightforward series, the preview should contain one series and however many issues Comic Vine reports.

Promote it into the correct app catalog

For example, if the series belongs in your existing Spider-Man catalog:

python ./tools/comicvine/promote_catalog_preview.py `
  --base app/src/main/assets/catalogs/spider_man_volume_2_catalog.json `
--preview tools/comicvine/output/my_new_series_catalog.json

One-line version:

python ./tools/comicvine/promote_catalog_preview.py --base app/src/main/assets/catalogs/spider_man_volume_2_catalog.json --preview tools/comicvine/output/my_new_series_catalog.json

With no --output, the script updates the base catalog itself.

It merges by Comic Vine series ID where available. If that Comic Vine volume is already present, it replaces that series entry; if it is new, it appends it. It also merges universe definitions and refuses conflicting definitions.

Your current app catalog assets are:

app/src/main/assets/catalogs/spider_man_volume_2_catalog.json
app/src/main/assets/catalogs/ultimate_marvel_2000_2015_catalog.json

So choose the one the series logically belongs to.

Review the Git diff

Before running the app, check the diff for the catalog JSON.

You should normally see:

one new series block added, or
one existing series block updated

You should not see unrelated series disappearing or massive unexpected rewrites.

The config file itself should also be committed. The cached Comic Vine response and generated preview should not be, because cache/ and output/ are intentionally ignored.

Run the catalog-related tests

At minimum I’d run:

ComicCatalogAssetParserTest
ComicCatalogImporterTest

The parser tests verify the catalog asset format, and the importer tests verify that catalog data can actually be imported into the database. Those are existing test suites in the repo.

For a larger catalog change, I’d then run the full instrumentation suite as you’ve been doing.

Launch the app

You do not have to manually call an importer.

On app startup, ComicReadingCompanionApplication enumerates every catalog asset under the catalog assets directory, parses each one, and imports each catalog into the database. After that it imports the reading-list assets.

So once the JSON is in:

app/src/main/assets/catalogs/

the app will pick it up automatically.

One compact version you can keep beside your terminal is:

1. Find Comic Vine volume ID
2. Create tools/comicvine/catalog_configs/<series>.json
3. py tools/comicvine/cache_mapped_volume_issues.py --config <config>
4. py tools/comicvine/build_catalog.py --config <config> --output tools/comicvine/output/<series>_catalog.json
5. Inspect preview
6. py tools/comicvine/promote_catalog_preview.py --base <app catalog> --preview <preview>
7. Review git diff
8. Run ComicCatalogAssetParserTest + ComicCatalogImporterTest
9. Run app / full instrumentation tests
10. Commit config + updated app catalog

For most normal series, that’s the whole pipeline. The only time it gets more involved is when Comic Vine splits one logical local series across multiple volumes, numbers issues strangely, or mixes annuals/specials into a volume; that’s when issueMappings or issueOverrides come into play.