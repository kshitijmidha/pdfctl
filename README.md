# pdfctl

A command-line toolkit for PDF manipulation — merge, split, delete, rotate, and extract text. Built with Java and Apache PDFBox.

## Capabilities

* `info` — show page count, version, encryption, and standard metadata
* `merge` — combine 2+ PDFs in order
* `split` — explode into `page-001.pdf` or extract selected pages
* `delete` — remove selected pages
* `rotate` — rotate pages by 90, 180, or 270
* `extract-text` — extract UTF-8 text to stdout or file

## Installation

Requires Java 21.

**From source (Windows):**
```bat
.\gradlew installDist
build\install\pdfctl\bin\pdfctl.bat --help
```

**From source (Unix):**
```sh
./gradlew installDist
build/install/pdfctl/bin/pdfctl --help
```

**Distribution archives:**
```sh
./gradlew distZip   # build/distributions/pdfctl.zip
./gradlew distTar   # build/distributions/pdfctl.tar
unzip build/distributions/pdfctl.zip && ./pdfctl/bin/pdfctl --help
```

`build/libs/pdfctl.jar` is a thin JAR and is **not** intended to run via `java -jar`. Use the distribution (`installDist`/`distZip`) which bundles `pdfbox`, `fontbox`, and `picocli`.

## Usage

```sh
pdfctl info document.pdf
pdfctl info document.pdf --json
pdfctl merge a.pdf b.pdf -o merged.pdf
pdfctl split document.pdf -o pages/
pdfctl split document.pdf --pages 2,4-6 -o selected.pdf
pdfctl delete document.pdf --pages 2,4 -o cleaned.pdf
pdfctl rotate document.pdf --angle 90 -o rotated.pdf
pdfctl extract-text document.pdf -o text.txt
```

Pages are 1-indexed: `1`, `1,3,5`, `1,3-5`, `5-7,10`, `10-` (through end). `--password` for encrypted PDFs, `--force` to overwrite.

## Exit codes

| Code | Meaning |
|------|---------|
| 0 | success |
| 1 | usage / validation (bad args, bad page range) |
| 2 | I/O / output conflict (missing file, exists without `--force`) |
| 3 | corrupt PDF / unexpected processing failure |
| 4 | encrypted PDF / password failure |

## Preservation / Limitations

Preserved across `merge`/`split`/`delete`:
page content, page dimensions, rotation, standard metadata (Title/Author/Subject/Keywords/Creator/Producer), annotations/links (per-page).

Lost (importPage-based):
bookmarks/outlines, named destinations, AcroForms, embedded files.

Other:
* `merge` uses metadata from the first input.
* Cross-page `GoTo` links may dangle if target not retained.
* Large multi-file merges hold the destination in memory.

## Architecture

```
CLI (picocli)
  ↓
UseCase (validation, temp file, atomic move)
  ↓
PdfBoxService (interface)
  ↓
PDFBox (Loader, PDDocument, importPage, PDFTextStripper)
```

`AppFactory` is the explicit composition root — the only place that wires `PdfBoxServiceImpl` into `UseCase`s and `CLI` commands.

## Development

```sh
./gradlew test          # 165 tests, generated fixtures (no committed PDFs)
./gradlew clean build   # jar + distributions
```

Tests generate PDFs with PDFBox (`TestFixtures`) rather than checking in binaries.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
