#!/usr/bin/env python3
"""
Repository summary generator.
Creates a comprehensive summary of the repository analysis.
"""
import json
import pathlib
from datetime import datetime


def generate_summary():
    """Generate comprehensive repository summary"""
    
    # Load data
    repo_map_file = pathlib.Path("repo-map.json")
    dependency_file = pathlib.Path("dependency-analysis.json")
    
    if not repo_map_file.exists():
        print("❌ repo-map.json not found. Run tools/repo-map/run.py first.")
        return
    
    with open(repo_map_file) as f:
        repo_data = json.load(f)
    
    dependency_data = {}
    if dependency_file.exists():
        with open(dependency_file) as f:
            dependency_data = json.load(f)
    
    # Generate summary
    summary = {
        "generated_at": datetime.now().isoformat(),
        "repository": {
            "name": "WristLingo (Android + Wear OS)",
            "description": "Offline-first Android + Wear OS translation app",
            "type": "Android Multi-Module Project",
            "languages": list(set(m["language"] for m in repo_data["modules"])),
            "total_modules": len(repo_data["modules"])
        },
        "modules": {
            module["name"]: {
                "type": module["kind"],
                "language": module["language"],
                "path": module["path"],
                "key_components": len(module["public_api"].get("classes", [])),
                "external_deps": len(module["deps_external"]),
                "test_coverage": module["tests"]["count"] > 0
            }
            for module in repo_data["modules"]
        },
        "architecture": {
            "patterns": [
                "Provider Pattern (ASR & Translation)",
                "Repository Pattern (Data Access)",
                "Foreground Service (Background Processing)",
                "Data Layer Messaging (Watch-Phone Communication)"
            ],
            "key_technologies": [
                "Kotlin",
                "Jetpack Compose",
                "Room Database",
                "ML Kit",
                "Whisper.cpp (JNI)",
                "Android Data Layer"
            ],
            "deployment_flavors": ["offline", "hybrid", "cloudstt"]
        },
        "quality_metrics": {
            "dependency_cycles": len(repo_data["graphs"]["cycles"]),
            "complexity_hotspots": len(repo_data["hotspots"]),
            "test_modules": sum(1 for m in repo_data["modules"] if m["tests"]["count"] > 0),
            "documentation_files": ["README.md", "README.wristlingo.md", "docs/", "codex/"]
        },
        "build_system": {
            "type": "Gradle (Kotlin DSL)",
            "java_version": "17+",
            "android_sdk": "35",
            "min_sdk_phone": "26",
            "min_sdk_wear": "30",
            "ci_cd": "GitHub Actions"
        }
    }
    
    # Add dependency analysis if available
    if dependency_data:
        summary["dependency_analysis"] = {
            "architectural_layers": len(dependency_data.get("layers", {})),
            "critical_paths": len(dependency_data.get("critical_path", [])),
            "improvement_suggestions": len(dependency_data.get("suggestions", []))
        }
    
    # Save summary
    summary_file = pathlib.Path("repository-summary.json")
    with open(summary_file, 'w') as f:
        json.dump(summary, f, indent=2)
    
    # Print human-readable summary
    print("# Repository Analysis Summary")
    print(f"Generated: {summary['generated_at']}")
    print()
    
    repo = summary["repository"]
    print(f"**Project:** {repo['name']}")
    print(f"**Type:** {repo['type']}")
    print(f"**Languages:** {', '.join(repo['languages'])}")
    print(f"**Modules:** {repo['total_modules']}")
    print()
    
    print("## Module Breakdown")
    for name, info in summary["modules"].items():
        print(f"- **{name}** ({info['type']}) - {info['language']} - {info['key_components']} components")
    print()
    
    print("## Architecture")
    for pattern in summary["architecture"]["patterns"]:
        print(f"- {pattern}")
    print()
    
    print("## Key Technologies")
    for tech in summary["architecture"]["key_technologies"]:
        print(f"- {tech}")
    print()
    
    quality = summary["quality_metrics"]
    print("## Quality Metrics")
    print(f"- Dependency cycles: {quality['dependency_cycles']}")
    print(f"- Complexity hotspots: {quality['complexity_hotspots']}")
    print(f"- Modules with tests: {quality['test_modules']}/{repo['total_modules']}")
    print()
    
    build = summary["build_system"]
    print("## Build System")
    print(f"- Build tool: {build['type']}")
    print(f"- Java version: {build['java_version']}")
    print(f"- Target Android SDK: {build['android_sdk']}")
    print(f"- Min SDK (phone/wear): {build['min_sdk_phone']}/{build['min_sdk_wear']}")
    print(f"- CI/CD: {build['ci_cd']}")
    print()
    
    print(f"✅ Detailed summary saved to {summary_file}")
    
    # Generate recommendations
    print("\n## Recommendations")
    
    if quality['dependency_cycles'] > 0:
        print(f"⚠️ Address {quality['dependency_cycles']} dependency cycle(s)")
    
    if quality['test_modules'] < repo['total_modules']:
        untested = repo['total_modules'] - quality['test_modules']
        print(f"📝 Add tests to {untested} module(s) without test coverage")
    
    if quality['complexity_hotspots'] > 0:
        print(f"🔧 Refactor {quality['complexity_hotspots']} complexity hotspot(s)")
    
    print("✅ Repository analysis complete!")


if __name__ == "__main__":
    generate_summary()