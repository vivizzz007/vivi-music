#!/usr/bin/env python3
"""
Batch download Material Symbols icons from Google Fonts.
"""

import argparse
import gzip
import logging
import os
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed


def download_icon(icon_name, output_dir, overwrite=False):
    """Download a single Material Symbol icon."""
    output_path = os.path.join(output_dir, f"{icon_name}.svg")

    if not overwrite and os.path.exists(output_path):
        logging.info("Skipping %s (already exists)", icon_name)
        return icon_name, True, "cached"

    url = f"https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/{icon_name}/default/24px.svg"

    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0",
        "Accept": "*/*",
        "Accept-Language": "en-US,en;q=0.9",
        "Accept-Encoding": "gzip, deflate, br, zstd",
        "Referer": "https://fonts.google.com/",
        "Origin": "https://fonts.google.com",
        "Sec-GPC": "1",
        "Connection": "keep-alive",
        "Sec-Fetch-Dest": "empty",
        "Sec-Fetch-Mode": "cors",
        "Sec-Fetch-Site": "cross-site",
        "Priority": "u=4",
        "TE": "trailers",
    }

    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=10) as response:
            if response.status != 200:
                return icon_name, False, f"HTTP {response.status}"

            svg_data = response.read()

            # Decompress if gzip compressed
            if svg_data[:2] == b"\x1f\x8b":  # gzip magic number
                svg_data = gzip.decompress(svg_data)

            # Ensure output directory exists
            os.makedirs(output_dir, exist_ok=True)

            with open(output_path, "wb") as f:
                f.write(svg_data)

            logging.info("Downloaded %s successfully", icon_name)
            return icon_name, True, "downloaded"
    except Exception as exc:
        logging.error("Failed to download %s: %s", icon_name, exc)
        return icon_name, False, str(exc)


def main():
    parser = argparse.ArgumentParser(
        description="Batch download Material Symbols icons from Google Fonts",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Download single icon
  %(prog)s palette -o ./icons

  # Download multiple icons
  %(prog)s palette star_shine music_note -o ./icons

  # Download from file list
  %(prog)s -f icon_list.txt -o ./icons

  # Parallel downloads
  %(prog)s palette star music piano -o ./icons -j 4
        """,
    )

    parser.add_argument(
        "icons",
        nargs="*",
        help="Icon names to download (e.g., palette, star_shine)",
    )
    parser.add_argument(
        "-f",
        "--file",
        type=str,
        help="Read icon names from file (one per line)",
    )
    parser.add_argument(
        "-o",
        "--output-dir",
        type=str,
        default=".",
        help="Output directory for downloaded SVGs (default: current directory)",
    )
    parser.add_argument(
        "-j",
        "--jobs",
        type=int,
        default=1,
        help="Number of parallel downloads (default: 1)",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Overwrite existing files",
    )
    parser.add_argument(
        "-v",
        "--verbose",
        action="store_true",
        help="Enable verbose logging",
    )

    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="[%(levelname)s] %(message)s",
    )

    # Collect icon names
    icon_names = list(args.icons) if args.icons else []

    if args.file:
        try:
            with open(args.file, "r") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#"):
                        icon_names.append(line)
        except Exception as exc:
            print(f"Error reading file {args.file}: {exc}", file=sys.stderr)
            return 1

    if not icon_names:
        parser.error(
            "No icons specified. Provide icon names as arguments or via --file"
        )

    # Remove duplicates while preserving order
    seen = set()
    unique_icons = []
    for icon in icon_names:
        if icon not in seen:
            seen.add(icon)
            unique_icons.append(icon)

    logging.info("Downloading %d icon(s) to %s", len(unique_icons), args.output_dir)

    # Download icons
    success_count = 0
    fail_count = 0
    cached_count = 0

    if args.jobs > 1:
        # Parallel downloads
        with ThreadPoolExecutor(max_workers=args.jobs) as executor:
            futures = {
                executor.submit(
                    download_icon, icon, args.output_dir, args.overwrite
                ): icon
                for icon in unique_icons
            }

            for future in as_completed(futures):
                icon_name, success, status = future.result()
                if success:
                    if status == "cached":
                        cached_count += 1
                    else:
                        success_count += 1
                else:
                    fail_count += 1
    else:
        # Sequential downloads
        for icon in unique_icons:
            icon_name, success, status = download_icon(
                icon, args.output_dir, args.overwrite
            )
            if success:
                if status == "cached":
                    cached_count += 1
                else:
                    success_count += 1
            else:
                fail_count += 1

    # Summary
    print()
    print(f"=== Download Summary ===")
    print(f"Total icons: {len(unique_icons)}")
    print(f"Downloaded: {success_count}")
    print(f"Cached: {cached_count}")
    print(f"Failed: {fail_count}")
    print(f"Output directory: {os.path.abspath(args.output_dir)}")

    return 0 if fail_count == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
