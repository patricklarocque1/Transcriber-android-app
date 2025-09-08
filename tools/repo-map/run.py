#!/usr/bin/env python3
"""
Local repository mapper (no network). Scans files, parses common manifests,
emits REPO_MAP.md and repo-map.json.
"""
import os
import re
import json
import pathlib
import xml.etree.ElementTree as ET
from typing import Dict, List, Set, Optional, Any, Tuple
from dataclasses import dataclass, asdict
from collections import defaultdict


@dataclass
class ModuleInfo:
    name: str
    path: str
    language: str
    kind: str  # library|service|cli|package
    public_api: Dict[str, List[str]]
    deps_internal: List[str]
    deps_external: List[str]
    entrypoints: List[str]
    tests: Dict[str, Any]
    schemas: List[str]
    notes: str


class RepoMapper:
    def __init__(self, root_path: str):
        self.root = pathlib.Path(root_path).resolve()
        self.modules: List[ModuleInfo] = []
        self.file_counts = defaultdict(int)
        self.hotspots = []
        
    def scan_repository(self) -> Dict[str, Any]:
        """Main scanning entry point"""
        print(f"Scanning repository at {self.root}")
        
        # Detect modules and packages
        self._detect_modules()
        
        # Analyze dependencies and APIs
        self._analyze_dependencies()
        
        # Find hotspots
        self._find_hotspots()
        
        # Generate dependency graph
        graph_mermaid = self._generate_mermaid_graph()
        cycles = self._detect_cycles()
        
        return {
            "modules": [asdict(m) for m in self.modules],
            "graphs": {
                "modules_mermaid": graph_mermaid,
                "cycles": cycles
            },
            "hotspots": self.hotspots
        }
    
    def _detect_modules(self):
        """Detect modules based on build files and structure"""
        
        # Android modules (Gradle)
        gradle_files = list(self.root.glob("**/build.gradle*"))
        for gradle_file in gradle_files:
            if gradle_file.name in ["build.gradle", "build.gradle.kts"]:
                self._analyze_gradle_module(gradle_file)
        
        # Check for root-level configuration
        settings_gradle = self.root / "settings.gradle.kts"
        if settings_gradle.exists():
            self._analyze_gradle_settings(settings_gradle)
    
    def _analyze_gradle_module(self, gradle_file: pathlib.Path):
        """Analyze a Gradle module"""
        module_path = gradle_file.parent
        relative_path = module_path.relative_to(self.root)
        
        # Skip root build file if it's minimal
        if relative_path == pathlib.Path("."):
            content = gradle_file.read_text()
            if len(content.strip()) < 100:  # Minimal root build
                return
        
        content = gradle_file.read_text()
        
        # Determine module type
        kind = "library"
        if "com.android.application" in content:
            kind = "service"  # Android app
        elif "com.android.library" in content:
            kind = "library"
        
        # Extract namespace/package
        namespace_match = re.search(r'namespace\s*=\s*"([^"]+)"', content)
        app_id_match = re.search(r'applicationId\s*=\s*"([^"]+)"', content)
        
        name = str(relative_path) if relative_path != pathlib.Path(".") else "root"
        if namespace_match:
            name = namespace_match.group(1).split('.')[-1]
        elif app_id_match:
            name = app_id_match.group(1).split('.')[-1]
        
        # Find source files
        src_dir = module_path / "src" / "main"
        test_dir = module_path / "src" / "test"
        
        # Analyze Kotlin/Java files
        kt_files = list(src_dir.glob("**/*.kt")) if src_dir.exists() else []
        java_files = list(src_dir.glob("**/*.java")) if src_dir.exists() else []
        cpp_files = list(src_dir.glob("**/*.cpp")) if src_dir.exists() else []
        
        language = "kotlin"
        if java_files and not kt_files:
            language = "java"
        elif cpp_files:
            language = "kotlin+cpp"
        
        # Extract public API
        public_api = self._extract_kotlin_api(kt_files + java_files)
        
        # Find external dependencies
        deps_external = self._extract_gradle_deps(content)
        
        # Find entrypoints
        entrypoints = []
        if any("MainActivity" in f.name for f in kt_files):
            entrypoints.append("MainActivity")
        if any("Application" in f.name for f in kt_files):
            entrypoints.append("Application")
        
        # Test info
        test_files = list(test_dir.glob("**/*.kt")) if test_dir.exists() else []
        test_info = {
            "count": len(test_files),
            "paths": [str(f.relative_to(self.root)) for f in test_files[:5]]  # Limit to first 5
        }
        
        # Generate notes
        notes = self._generate_module_notes(module_path, content, kt_files, java_files)
        
        module = ModuleInfo(
            name=name,
            path=str(relative_path),
            language=language,
            kind=kind,
            public_api=public_api,
            deps_internal=[],  # Will be filled in _analyze_dependencies
            deps_external=deps_external,
            entrypoints=entrypoints,
            tests=test_info,
            schemas=[],
            notes=notes
        )
        
        self.modules.append(module)
    
    def _analyze_gradle_settings(self, settings_file: pathlib.Path):
        """Analyze Gradle settings for workspace structure"""
        content = settings_file.read_text()
        
        # Extract included modules
        include_matches = re.findall(r'include\s*\(\s*"([^"]+)"', content)
        
        # Add root project info if not already present
        root_name_match = re.search(r'rootProject\.name\s*=\s*"([^"]+)"', content)
        if root_name_match and not any(m.path == "." for m in self.modules):
            root_module = ModuleInfo(
                name=root_name_match.group(1),
                path=".",
                language="gradle",
                kind="package",
                public_api={"modules": include_matches},
                deps_internal=[],
                deps_external=[],
                entrypoints=["gradlew"],
                tests={"count": 0, "paths": []},
                schemas=[],
                notes=f"Gradle multi-module project with {len(include_matches)} modules"
            )
            self.modules.insert(0, root_module)
    
    def _extract_kotlin_api(self, source_files: List[pathlib.Path]) -> Dict[str, List[str]]:
        """Extract public API from Kotlin/Java files"""
        api = {"classes": [], "functions": [], "types": [], "endpoints": []}
        
        for file in source_files[:10]:  # Limit to avoid performance issues
            try:
                content = file.read_text()
                
                # Extract classes/interfaces
                class_matches = re.findall(r'(?:public\s+)?(?:class|interface|object|enum class)\s+(\w+)', content)
                api["classes"].extend(class_matches)
                
                # Extract public functions
                fun_matches = re.findall(r'(?:public\s+)?fun\s+(\w+)', content)
                api["functions"].extend(fun_matches)
                
                # Extract data classes and type aliases
                data_matches = re.findall(r'data\s+class\s+(\w+)', content)
                type_matches = re.findall(r'typealias\s+(\w+)', content)
                api["types"].extend(data_matches + type_matches)
                
            except Exception as e:
                print(f"Warning: Could not parse {file}: {e}")
        
        # Remove duplicates and limit size
        for key in api:
            api[key] = list(set(api[key]))[:10]  # Limit to 10 items per category
        
        return api
    
    def _extract_gradle_deps(self, content: str) -> List[str]:
        """Extract external dependencies from Gradle file"""
        deps = []
        
        # Find implementation/api dependencies
        dep_matches = re.findall(r'(?:implementation|api|testImplementation)\s*\(\s*(?:libs\.)?([^)]+)\)', content)
        
        for match in dep_matches:
            # Clean up the dependency string
            dep = match.strip('"\'')
            if not dep.startswith(':') and '.' in dep:  # External dependency
                # Extract package name and version if possible
                if '@' in dep:
                    deps.append(dep)
                else:
                    deps.append(dep)
        
        return list(set(deps))[:15]  # Limit and dedupe
    
    def _generate_module_notes(self, module_path: pathlib.Path, gradle_content: str, 
                             kt_files: List[pathlib.Path], java_files: List[pathlib.Path]) -> str:
        """Generate concise notes about the module"""
        notes = []
        
        # Determine primary purpose
        if "com.android.application" in gradle_content:
            if "wear" in str(module_path).lower():
                notes.append("Wear OS application module")
            else:
                notes.append("Android application module")
        elif "com.android.library" in gradle_content:
            notes.append("Android library module")
        
        # Check for key components
        file_names = [f.name for f in kt_files + java_files]
        
        if any("Service" in name for name in file_names):
            notes.append("contains background services")
        if any("Provider" in name for name in file_names):
            notes.append("implements provider pattern")
        if any("Database" in name or "Dao" in name for name in file_names):
            notes.append("includes database layer")
        if "compose" in gradle_content.lower():
            notes.append("uses Jetpack Compose UI")
        if any("whisper" in name.lower() for name in file_names):
            notes.append("integrates Whisper ASR")
        
        # Check for native code
        cpp_dir = module_path / "src" / "main" / "cpp"
        if cpp_dir.exists():
            notes.append("includes native C++ code")
        
        return "; ".join(notes) if notes else "Standard module"
    
    def _analyze_dependencies(self):
        """Analyze internal dependencies between modules"""
        module_by_path = {m.path: m for m in self.modules}
        
        for module in self.modules:
            # For Android modules, check imports in source files
            module_path = self.root / module.path
            src_files = list(module_path.glob("src/**/*.kt"))
            
            for src_file in src_files[:20]:  # Limit for performance
                try:
                    content = src_file.read_text()
                    # Find imports that reference other modules
                    import_matches = re.findall(r'import\s+com\.example\.wristlingo\.(\w+)', content)
                    
                    for imp in import_matches:
                        # Map import to module
                        for other_module in self.modules:
                            if other_module != module and imp in other_module.name:
                                if other_module.name not in module.deps_internal:
                                    module.deps_internal.append(other_module.name)
                                    
                except Exception:
                    continue
    
    def _find_hotspots(self):
        """Find complexity and risk hotspots"""
        for module in self.modules:
            module_path = self.root / module.path
            
            # Find large files
            for src_file in module_path.glob("**/*.kt"):
                try:
                    content = src_file.read_text()
                    loc = len(content.splitlines())
                    
                    if loc > 200:  # Large file threshold
                        self.hotspots.append({
                            "path": str(src_file.relative_to(self.root)),
                            "loc": loc,
                            "reasons": ["large file"]
                        })
                        
                except Exception:
                    continue
        
        # Sort by LOC descending and limit
        self.hotspots.sort(key=lambda x: x["loc"], reverse=True)
        self.hotspots = self.hotspots[:10]
    
    def _generate_mermaid_graph(self) -> str:
        """Generate Mermaid dependency graph"""
        lines = ["graph TD"]
        
        # Add nodes
        for module in self.modules:
            safe_name = re.sub(r'[^a-zA-Z0-9]', '_', module.name)
            lines.append(f"  {safe_name}[\"{module.name}\"]")
        
        # Add edges
        for module in self.modules:
            safe_name = re.sub(r'[^a-zA-Z0-9]', '_', module.name)
            for dep in module.deps_internal:
                safe_dep = re.sub(r'[^a-zA-Z0-9]', '_', dep)
                lines.append(f"  {safe_name} --> {safe_dep}")
        
        return "\n".join(lines)
    
    def _detect_cycles(self) -> List[List[str]]:
        """Detect dependency cycles"""
        # Simple cycle detection using DFS
        graph = defaultdict(list)
        for module in self.modules:
            for dep in module.deps_internal:
                graph[module.name].append(dep)
        
        visited = set()
        rec_stack = set()
        cycles = []
        
        def dfs(node, path):
            if node in rec_stack:
                # Found cycle
                cycle_start = path.index(node)
                cycle = path[cycle_start:] + [node]
                cycles.append(cycle)
                return
            
            if node in visited:
                return
            
            visited.add(node)
            rec_stack.add(node)
            
            for neighbor in graph.get(node, []):
                dfs(neighbor, path + [node])
            
            rec_stack.remove(node)
        
        for module in self.modules:
            if module.name not in visited:
                dfs(module.name, [])
        
        return cycles[:5]  # Limit to first 5 cycles


def main():
    root = pathlib.Path(".").resolve()
    mapper = RepoMapper(str(root))
    
    # Scan repository
    repo_data = mapper.scan_repository()
    
    # Write JSON output
    json_file = root / "repo-map.json"
    with open(json_file, 'w') as f:
        json.dump(repo_data, f, indent=2)
    
    print(f"Generated {json_file}")
    
    # Generate Markdown report
    md_file = root / "REPO_MAP.md"
    generate_markdown_report(repo_data, md_file)
    
    print(f"Generated {md_file}")
    print(f"Found {len(repo_data['modules'])} modules")


def generate_markdown_report(repo_data: Dict[str, Any], output_file: pathlib.Path):
    """Generate the REPO_MAP.md file"""
    
    modules = repo_data["modules"]
    graphs = repo_data["graphs"]
    hotspots = repo_data["hotspots"]
    
    content = []
    
    # Header
    content.append("# Repository Map: WristLingo (Android + Wear OS)")
    content.append("")
    content.append("**WristLingo** is an offline-first Android + Wear OS translation app that captures speech on the watch, processes it on the phone using on-device ASR (Whisper) and translation (ML Kit), and streams live captions back to the watch.")
    content.append("")
    
    # Repository Structure
    content.append("## Repository Structure")
    content.append("")
    content.append("```")
    content.append("Transcriber-android-app/")
    content.append("├── app/                    # Main Android phone application")
    content.append("│   ├── src/main/")
    content.append("│   │   ├── java/com/example/wristlingo/")
    content.append("│   │   │   ├── providers/  # ASR & Translation providers")
    content.append("│   │   │   ├── data/       # Database & data management")
    content.append("│   │   │   ├── wear/       # Wear OS communication")
    content.append("│   │   │   └── ...         # Core app components")
    content.append("│   │   └── cpp/            # Native Whisper JNI integration")
    content.append("│   └── build.gradle.kts    # App module build config")
    content.append("├── wear/                   # Wear OS companion application")
    content.append("│   └── src/main/java/com/example/wristlingo/wear/")
    content.append("├── docs/                   # Architecture & design documentation")
    content.append("├── codex/                  # AI assistant prompts & guidance")
    content.append("├── gradle/                 # Gradle version catalog & wrapper")
    content.append("└── build.gradle.kts        # Root project configuration")
    content.append("```")
    content.append("")
    
    # Modules Overview
    content.append("## Modules Overview")
    content.append("")
    
    for module in modules:
        content.append(f"### {module['name']}")
        content.append(f"**Path:** `{module['path']}`  ")
        content.append(f"**Language:** {module['language']}  ")
        content.append(f"**Type:** {module['kind']}  ")
        content.append("")
        
        content.append(f"**Purpose:** {module['notes']}")
        content.append("")
        
        if module['public_api'].get('classes'):
            content.append(f"**Key Classes:** {', '.join(module['public_api']['classes'][:5])}")
        if module['public_api'].get('functions'):
            content.append(f"**Key Functions:** {', '.join(module['public_api']['functions'][:5])}")
        if module['public_api'].get('modules'):
            content.append(f"**Submodules:** {', '.join(module['public_api']['modules'])}")
        if module['entrypoints']:
            content.append(f"**Entrypoints:** {', '.join(module['entrypoints'])}")
        
        if module['deps_external']:
            content.append(f"**External Dependencies:** {', '.join(module['deps_external'][:3])}")
        if module['deps_internal']:
            content.append(f"**Internal Dependencies:** {', '.join(module['deps_internal'])}")
        
        if module['tests']['count'] > 0:
            content.append(f"**Tests:** {module['tests']['count']} test files")
        
        content.append("")
    
    # Dependency Graph
    content.append("## Module Dependencies")
    content.append("")
    content.append("```mermaid")
    content.append(graphs["modules_mermaid"])
    content.append("```")
    content.append("")
    
    if graphs["cycles"]:
        content.append("**⚠️ Dependency Cycles Detected:**")
        for cycle in graphs["cycles"]:
            content.append(f"- {' → '.join(cycle)}")
        content.append("")
    
    # Build & Runtime
    content.append("## Build & Runtime Requirements")
    content.append("")
    content.append("**Build Requirements:**")
    content.append("- Android SDK (API 35)")
    content.append("- Build-Tools 35.0.0")
    content.append("- JDK 17+ (auto-provisioned via Foojay resolver)")
    content.append("- NDK (for Whisper C++ integration)")
    content.append("")
    content.append("**Runtime Requirements:**")
    content.append("- Phone: Android 8.0+ (API 26+)")
    content.append("- Watch: Wear OS 4+")
    content.append("")
    content.append("**Build Commands:**")
    content.append("```bash")
    content.append("# Debug builds (all flavors)")
    content.append("./gradlew :app:assembleDebug :wear:assembleDebug")
    content.append("")
    content.append("# Release builds")
    content.append("./gradlew :app:assembleRelease :wear:assembleRelease")
    content.append("")
    content.append("# Run tests")
    content.append("./gradlew :app:testOfflineDebug")
    content.append("```")
    content.append("")
    
    # Product Flavors
    content.append("## Product Flavors")
    content.append("")
    content.append("The app supports three build flavors for different usage scenarios:")
    content.append("")
    content.append("- **`offline`** - Whisper + ML Kit only (fully offline)")
    content.append("- **`hybrid`** - System SpeechRecognizer/Whisper + Cloud Translation (toggle)")
    content.append("- **`cloudstt`** - Enables Cloud STT v2 option (explicit opt-in)")
    content.append("")
    
    # Architecture Highlights
    content.append("## Architecture Highlights")
    content.append("")
    content.append("**Data Flow:**")
    content.append("1. Watch captures audio via microphone")
    content.append("2. Small PCM frames (200-300ms) sent to phone via Data Layer")
    content.append("3. Phone processes audio with ASR (Whisper/System)")
    content.append("4. Text translated using ML Kit (on-device)")
    content.append("5. Captions streamed back to watch in real-time")
    content.append("6. Sessions stored in Room database for review")
    content.append("")
    content.append("**Key Patterns:**")
    content.append("- **Provider Pattern:** Pluggable ASR and Translation providers")
    content.append("- **Repository Pattern:** Data access abstraction with Room")
    content.append("- **Foreground Service:** Background processing with proper notifications")
    content.append("- **Data Layer Messaging:** Watch-phone communication via MessageClient")
    content.append("")
    
    # Hotspots
    if hotspots:
        content.append("## Complexity Hotspots")
        content.append("")
        content.append("Files requiring attention due to size or complexity:")
        content.append("")
        for hotspot in hotspots:
            content.append(f"- **{hotspot['path']}** ({hotspot['loc']} LOC) - {', '.join(hotspot['reasons'])}")
        content.append("")
    
    # Security & Privacy
    content.append("## Security & Privacy")
    content.append("")
    content.append("- **On-device by default:** ASR and translation happen locally")
    content.append("- **Explicit cloud opt-in:** Cloud features disabled by default")
    content.append("- **PII redaction:** Optional redaction of sensitive information")
    content.append("- **Proper permissions:** Microphone, foreground service, wake lock")
    content.append("- **Secure storage:** Room database with optional encryption")
    content.append("")
    
    # Development Workflow
    content.append("## Development Workflow")
    content.append("")
    content.append("**Local Development:**")
    content.append("1. Clone repository and ensure Android SDK is configured")
    content.append("2. Create `local.properties` with SDK path")
    content.append("3. Optionally create `keystore.properties` for release signing")
    content.append("4. Run `./gradlew :app:assembleOfflineDebug :wear:assembleOfflineDebug`")
    content.append("")
    content.append("**Testing:**")
    content.append("- Unit tests: `./gradlew :app:testOfflineDebug`")
    content.append("- Integration tests available for providers and data layer")
    content.append("- Manual testing via debug builds on paired devices")
    content.append("")
    content.append("**CI/CD:**")
    content.append("- GitHub Actions workflow builds all flavors on push/PR")
    content.append("- Artifacts uploaded for manual testing")
    content.append("- Optional signed releases on tags with proper secrets")
    content.append("")
    
    # Extension Points
    content.append("## Extension Points")
    content.append("")
    content.append("**Adding New Providers:**")
    content.append("1. Implement `AsrProvider` or `TranslationProvider` interface")
    content.append("2. Add provider to DI configuration")
    content.append("3. Update settings UI for selection")
    content.append("")
    content.append("**Adding New Features:**")
    content.append("- Export formats: Extend `Export.kt` with new serializers")
    content.append("- Audio processing: Add VAD or noise reduction in `audio/`")
    content.append("- UI components: Leverage Compose architecture")
    content.append("- Data Layer: Add new message types in `WearBridge.kt`")
    content.append("")
    
    # Write to file
    with open(output_file, 'w') as f:
        f.write('\n'.join(content))


if __name__ == "__main__":
    main()