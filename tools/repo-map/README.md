# Repository Mapping Tools

This directory contains tools for analyzing and mapping the repository structure, dependencies, and architecture.

## Generated Files

- **`REPO_MAP.md`** - Human-friendly repository overview with detailed module analysis
- **`repo-map.json`** - Machine-readable repository index with structured data
- **`repository-summary.json`** - High-level summary with quality metrics
- **`dependency-analysis.json`** - Detailed dependency analysis with cycles and suggestions
- **`architecture_graphs.md`** - Various Mermaid graphs for visualization

## Tools

### `run.py` - Main Repository Mapper
Scans the entire repository and generates comprehensive documentation.

```bash
python3 tools/repo-map/run.py
```

**Features:**
- Detects modules based on build files (Gradle, Maven, etc.)
- Extracts public APIs from source code
- Analyzes dependencies (internal and external)
- Finds complexity hotspots
- Generates Mermaid dependency graphs
- Creates both human and machine-readable outputs

### `dependency_analyzer.py` - Dependency Analysis
Provides detailed dependency analysis with architectural insights.

```bash
python3 tools/repo-map/dependency_analyzer.py
```

**Features:**
- Calculates architectural layers using topological sort
- Detects dependency cycles
- Computes fan-in/fan-out metrics
- Finds critical dependency paths
- Generates improvement suggestions

### `generate_graphs.py` - Graph Generator
Creates various types of visualization graphs.

```bash
python3 tools/repo-map/generate_graphs.py
```

**Generated Graphs:**
- Module dependency graph with styling
- Layered architecture visualization
- Technology stack groupings
- API surface relationships

### `summary.py` - Summary Generator
Creates a comprehensive summary with quality metrics.

```bash
python3 tools/repo-map/summary.py
```

**Output:**
- Project overview with key statistics
- Module breakdown and analysis
- Architecture patterns and technologies
- Quality metrics and recommendations

### `tests/test_schema.py` - Validation Tests
Validates the generated outputs against expected schemas.

```bash
python3 tools/repo-map/tests/test_schema.py
```

**Tests:**
- JSON schema validation
- Markdown consistency checks
- File reference validation

## Usage Workflow

1. **Generate Base Analysis:**
   ```bash
   python3 tools/repo-map/run.py
   ```

2. **Run Additional Analysis:**
   ```bash
   python3 tools/repo-map/dependency_analyzer.py
   python3 tools/repo-map/generate_graphs.py
   python3 tools/repo-map/summary.py
   ```

3. **Validate Results:**
   ```bash
   python3 tools/repo-map/tests/test_schema.py
   ```

## Output Files

All generated files are placed in the repository root:

- `REPO_MAP.md` - Main documentation
- `repo-map.json` - Structured data
- `repository-summary.json` - Summary metrics
- `dependency-analysis.json` - Dependency insights
- `architecture_graphs.md` - Visualization graphs
- `*.mmd` - Individual Mermaid graph files

## Customization

The tools are designed to be language-agnostic and can be extended to support additional:

- Build systems (Maven, npm, Cargo, etc.)
- Programming languages
- Project structures
- Analysis metrics

## Requirements

- Python 3.6+
- Standard library only (no external dependencies)
- Read access to repository files