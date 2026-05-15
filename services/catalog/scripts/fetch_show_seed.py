"""
Fetch episode data for a TV show from Wikipedia and emit a Flyway seed migration.

Usage:
    uv run --project services/catalog/scripts fetch_show_seed.py \
        "Stranger Things" \
        --episode-list-page "List of Stranger Things episodes" \
        --slug stranger_things \
        --output services/catalog/src/main/resources/db/migration/V2__seed_stranger_things.sql

Notes:
- Deterministic UUIDs (uuid5) keep re-runs byte-identical when the data hasn't
  changed, so `git diff` is meaningful.
- Episode list pages on Wikipedia transclude per-season articles using
  {{:Show season N}} — this script fetches each season article directly and
  extracts {{Episode list/sublist}} blocks which contain ShortSummary inline.
- Content is licensed CC BY-SA 3.0; attribution is emitted as a trailing
  comment block on the SQL file.
"""

from __future__ import annotations

import argparse
import datetime as dt
import re
import sys
import uuid
from dataclasses import dataclass

import requests
from bs4 import BeautifulSoup

WIKI_API = "https://en.wikipedia.org/w/api.php"
NAMESPACE = uuid.NAMESPACE_DNS
USER_AGENT = "team-special-ops-catalog-seed/0.1 (educational project)"


@dataclass
class EpisodeRow:
    season: int
    episode_number: int
    episode_index: int
    title: str
    summary: str
    article_title: str


def fetch_wikitext(page_title: str) -> str | None:
    """Return the wikitext for a Wikipedia page, or None if it doesn't exist."""
    resp = requests.get(
        WIKI_API,
        params={
            "action": "parse",
            "page": page_title,
            "prop": "wikitext",
            "format": "json",
            "formatversion": 2,
        },
        headers={"User-Agent": USER_AGENT},
        timeout=30,
    )
    resp.raise_for_status()
    data = resp.json()
    if "error" in data or "parse" not in data:
        return None
    return data["parse"]["wikitext"]


def discover_seasons(episode_list_wikitext: str, show_title: str) -> list[int]:
    """
    Discover season numbers from the episode list page.

    The episode list page transcludes season articles using {{:Show season N}}
    syntax. We scan for those transclusions and return the season numbers.
    """
    # Match patterns like {{:Stranger Things season 1}} or {{:Stranger Things season 5}}
    pattern = re.compile(
        r"\{\{:" + re.escape(show_title) + r"\s+season\s+(\d+)",
        re.IGNORECASE,
    )
    seasons = sorted(set(int(m.group(1)) for m in pattern.finditer(episode_list_wikitext)))
    return seasons


def extract_field(block: str, field_name: str) -> str:
    """
    Extract a named field value from an Episode list/sublist block.

    Handles both inline (| Field = value) and multi-line values, stopping
    at the next pipe-prefixed field or end of block.
    """
    pattern = re.compile(
        r"\|\s*" + re.escape(field_name) + r"\s*=\s*(.*?)(?=\n\s*\||\}\}$)",
        re.DOTALL | re.IGNORECASE,
    )
    m = pattern.search(block)
    if not m:
        return ""
    return m.group(1).strip()


def split_episode_blocks(wikitext: str) -> list[str]:
    """
    Split wikitext into individual Episode list/sublist template blocks.

    Each block starts at '{{Episode list/sublist' and ends just before the
    next such block or at end-of-string.
    """
    marker = "{{Episode list/sublist"
    parts = wikitext.split(marker)
    # parts[0] is text before first block; parts[1:] are each block (without the marker)
    return [marker + p for p in parts[1:]]


def clean_wiki_markup(raw: str) -> str:
    """Strip common wiki markup from a text field."""
    s = raw.strip()
    # Remove [[target|display]] -> display, or [[target]] -> target
    s = re.sub(r"\[\[(?:[^\]|]+\|)?([^\]]+)\]\]", r"\1", s)
    # Remove remaining {{...}} templates
    s = re.sub(r"\{\{[^}]*\}\}", "", s)
    # Remove <ref>...</ref> and <ref ... />
    s = re.sub(r"<ref[^>]*/?>.*?</ref>", "", s, flags=re.DOTALL)
    s = re.sub(r"<ref[^>]*/>", "", s)
    # Remove HTML tags
    s = re.sub(r"<[^>]+>", "", s)
    # Remove citation markers like [1]
    s = re.sub(r"\[\d+\]", "", s)
    # Remove leading/trailing quotes added by some templates
    s = re.sub(r'^"|"$', "", s)
    # Normalise whitespace
    s = re.sub(r"\s+", " ", s)
    return s.strip()


def parse_season_episodes(
    season_wikitext: str,
    season_number: int,
    global_offset: int,
) -> list[tuple[int, int, int, str, str]]:
    """
    Parse all episodes from a season article's wikitext.

    Returns a list of (season, in_season_ep, global_ep, title, short_summary)
    tuples in airing order.
    """
    blocks = split_episode_blocks(season_wikitext)
    results = []
    for block in blocks:
        ep_global_raw = extract_field(block, "EpisodeNumber")
        ep_in_season_raw = extract_field(block, "EpisodeNumber2")
        title_raw = extract_field(block, "Title")
        summary_raw = extract_field(block, "ShortSummary")

        if not ep_in_season_raw:
            continue  # skip malformed blocks

        try:
            in_season = int(ep_in_season_raw.strip())
        except ValueError:
            continue

        global_ep = global_offset + in_season
        title = clean_wiki_markup(title_raw) if title_raw else f"Season {season_number}, Episode {in_season}"
        summary = clean_wiki_markup(summary_raw) if summary_raw else ""

        results.append((season_number, in_season, global_ep, title, summary))

    return results


def emit_sql(show_title: str, slug: str, episodes: list[EpisodeRow], output_path: str) -> None:
    show_id = uuid.uuid5(NAMESPACE, f"show:{slug}")
    seasons_count = max(e.season for e in episodes)
    episodes_count = len(episodes)
    now = dt.datetime.now(dt.UTC).isoformat()

    lines: list[str] = []
    lines.append(f"-- Seed migration: {show_title}")
    lines.append("-- Auto-generated by services/catalog/scripts/fetch_show_seed.py")
    lines.append(f"-- Generated at: {now}")
    lines.append("-- Edit by re-running the script, not by hand.")
    lines.append("")
    lines.append(
        f"INSERT INTO show (id, title, seasons_count, episodes_count) VALUES "
        f"('{show_id}', $${show_title}$$, {seasons_count}, {episodes_count});"
    )
    lines.append("")
    for e in episodes:
        ep_id = uuid.uuid5(NAMESPACE, f"episode:{slug}:s{e.season}e{e.episode_number}")
        lines.append(
            "INSERT INTO episode "
            "(id, show_id, season, episode_number, episode_index, title, summary) VALUES ("
            f"'{ep_id}', '{show_id}', {e.season}, {e.episode_number}, "
            f"{e.episode_index}, $${e.title}$$, $${e.summary}$$);"
        )
    lines.append("")
    lines.append("-- Source: English Wikipedia, articles on each episode and the")
    lines.append(f"-- 'List of {show_title} episodes' page.")
    lines.append("-- Content licensed under CC BY-SA 3.0.")
    lines.append("")

    with open(output_path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    print(f"Wrote {output_path} ({episodes_count} episodes, {seasons_count} seasons).")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("show_title", help='Display title, e.g. "Stranger Things"')
    p.add_argument(
        "--episode-list-page",
        required=True,
        help='Wikipedia page title for the list page, e.g. "List of Stranger Things episodes"',
    )
    p.add_argument("--slug", required=True, help="Filesystem-safe slug, e.g. stranger_things")
    p.add_argument("--output", required=True, help="Path to write the V2 SQL migration to")
    args = p.parse_args()

    # Step 1: Fetch the episode list page to discover which seasons exist
    print(f"Fetching episode list page: {args.episode_list_page}")
    list_wikitext = fetch_wikitext(args.episode_list_page)
    if not list_wikitext:
        print(f"ERROR: Could not fetch page '{args.episode_list_page}'", file=sys.stderr)
        return 1

    seasons = discover_seasons(list_wikitext, args.show_title)
    if not seasons:
        print(
            f"ERROR: Could not discover season transclusions from '{args.episode_list_page}'.\n"
            "Expected patterns like {{:Stranger Things season 1}} in the wikitext.",
            file=sys.stderr,
        )
        return 1
    print(f"Discovered seasons: {seasons}")

    # Step 2: Fetch each season article and parse episodes
    all_episodes: list[EpisodeRow] = []
    global_index = 0
    for season_num in seasons:
        season_page = f"{args.show_title} season {season_num}"
        print(f"Fetching season {season_num}: {season_page}")
        season_wikitext = fetch_wikitext(season_page)
        if not season_wikitext:
            print(f"  WARNING: Could not fetch '{season_page}', skipping.", file=sys.stderr)
            continue

        season_rows = parse_season_episodes(season_wikitext, season_num, global_index)
        if not season_rows:
            print(f"  WARNING: No episodes found in '{season_page}'.", file=sys.stderr)
            continue

        for season, in_season, global_ep, title, summary in season_rows:
            if not summary:
                print(f"  WARNING: empty summary for S{season}E{in_season} '{title}'", file=sys.stderr)
                summary = f"(No plot summary available for {title}.)"

            all_episodes.append(
                EpisodeRow(
                    season=season,
                    episode_number=in_season,
                    episode_index=global_ep,
                    title=title,
                    summary=summary,
                    article_title=title,
                )
            )
            print(f"  fetched S{season}E{in_season} '{title}' ({len(summary)} chars)")

        global_index += len(season_rows)

    if not all_episodes:
        print(
            "ERROR: Could not parse any episodes. "
            "The wikitext format may have changed; inspect the season pages manually.",
            file=sys.stderr,
        )
        return 1

    emit_sql(args.show_title, args.slug, all_episodes, args.output)
    return 0


if __name__ == "__main__":
    sys.exit(main())
