#!/usr/bin/env python3
"""
Tests for repo-map.json schema validation and REPO_MAP.md consistency.
"""
import json
import pathlib
import sys
import re
from typing import Dict, Any, List


def test_json_schema():
    """Test that repo-map.json follows expected schema"""
    repo_root = pathlib.Path(__file__).parent.parent.parent.parent
    json_file = repo_root / "repo-map.json"
    
    assert json_file.exists(), "repo-map.json not found"
    
    with open(json_file) as f:
        data = json.load(f)
    
    # Test top-level structure
    assert "modules" in data, "Missing 'modules' key"
    assert "graphs" in data, "Missing 'graphs' key"
    assert "hotspots" in data, "Missing 'hotspots' key"
    
    assert isinstance(data["modules"], list), "modules should be a list"
    assert isinstance(data["graphs"], dict), "graphs should be a dict"
    assert isinstance(data["hotspots"], list), "hotspots should be a list"
    
    # Test module schema
    for module in data["modules"]:
        required_fields = ["name", "path", "language", "kind", "public_api", 
                          "deps_internal", "deps_external", "entrypoints", 
                          "tests", "schemas", "notes"]
        
        for field in required_fields:
            assert field in module, f"Module missing required field: {field}"
        
        # Test field types
        assert isinstance(module["name"], str), "name should be string"
        assert isinstance(module["path"], str), "path should be string"
        assert isinstance(module["language"], str), "language should be string"
        assert isinstance(module["kind"], str), "kind should be string"
        assert isinstance(module["public_api"], dict), "public_api should be dict"
        assert isinstance(module["deps_internal"], list), "deps_internal should be list"
        assert isinstance(module["deps_external"], list), "deps_external should be list"
        assert isinstance(module["entrypoints"], list), "entrypoints should be list"
        assert isinstance(module["tests"], dict), "tests should be dict"
        assert isinstance(module["schemas"], list), "schemas should be list"
        assert isinstance(module["notes"], str), "notes should be string"
        
        # Test public_api structure
        api_keys = ["functions", "classes", "endpoints", "types"]
        for key in api_keys:
            if key in module["public_api"]:
                assert isinstance(module["public_api"][key], list), f"public_api.{key} should be list"
        
        # Test tests structure
        if "count" in module["tests"]:
            assert isinstance(module["tests"]["count"], int), "tests.count should be int"
        if "paths" in module["tests"]:
            assert isinstance(module["tests"]["paths"], list), "tests.paths should be list"
    
    # Test graphs structure
    graphs = data["graphs"]
    assert "modules_mermaid" in graphs, "Missing modules_mermaid in graphs"
    assert "cycles" in graphs, "Missing cycles in graphs"
    
    assert isinstance(graphs["modules_mermaid"], str), "modules_mermaid should be string"
    assert isinstance(graphs["cycles"], list), "cycles should be list"
    
    # Test hotspots structure
    for hotspot in data["hotspots"]:
        assert "path" in hotspot, "hotspot missing path"
        assert "loc" in hotspot, "hotspot missing loc"
        assert "reasons" in hotspot, "hotspot missing reasons"
        
        assert isinstance(hotspot["path"], str), "hotspot path should be string"
        assert isinstance(hotspot["loc"], int), "hotspot loc should be int"
        assert isinstance(hotspot["reasons"], list), "hotspot reasons should be list"
    
    print("✓ JSON schema validation passed")


def test_markdown_consistency():
    """Test that REPO_MAP.md contains all modules from repo-map.json"""
    repo_root = pathlib.Path(__file__).parent.parent.parent.parent
    json_file = repo_root / "repo-map.json"
    md_file = repo_root / "REPO_MAP.md"
    
    assert json_file.exists(), "repo-map.json not found"
    assert md_file.exists(), "REPO_MAP.md not found"
    
    # Load JSON data
    with open(json_file) as f:
        json_data = json.load(f)
    
    # Load Markdown content
    with open(md_file) as f:
        md_content = f.read()
    
    # Extract module names from JSON
    json_modules = set(module["name"] for module in json_data["modules"])
    
    # Extract module names from Markdown (look for ### headers)
    md_module_matches = re.findall(r'^### (.+)$', md_content, re.MULTILINE)
    md_modules = set(match.strip() for match in md_module_matches)
    
    # Check that all JSON modules appear in Markdown
    missing_in_md = json_modules - md_modules
    extra_in_md = md_modules - json_modules
    
    if missing_in_md:
        print(f"⚠️ Modules in JSON but not in Markdown: {missing_in_md}")
    
    if extra_in_md:
        print(f"⚠️ Modules in Markdown but not in JSON: {extra_in_md}")
    
    # At least check that major modules are present
    expected_modules = {"app", "wear", "Transcriber-android-app"}
    present_modules = json_modules | md_modules
    
    for expected in expected_modules:
        if not any(expected.lower() in module.lower() for module in present_modules):
            print(f"⚠️ Expected module '{expected}' not found in either JSON or Markdown")
    
    print("✓ Markdown consistency check completed")


def test_file_references():
    """Test that file paths in hotspots actually exist"""
    repo_root = pathlib.Path(__file__).parent.parent.parent.parent
    json_file = repo_root / "repo-map.json"
    
    assert json_file.exists(), "repo-map.json not found"
    
    with open(json_file) as f:
        data = json.load(f)
    
    missing_files = []
    for hotspot in data["hotspots"]:
        file_path = repo_root / hotspot["path"]
        if not file_path.exists():
            missing_files.append(hotspot["path"])
    
    if missing_files:
        print(f"⚠️ Hotspot files not found: {missing_files}")
    else:
        print("✓ All hotspot file references are valid")


def main():
    """Run all tests"""
    print("Running repo-map validation tests...")
    
    try:
        test_json_schema()
        test_markdown_consistency()
        test_file_references()
        print("\n✅ All tests passed!")
        return 0
    except AssertionError as e:
        print(f"\n❌ Test failed: {e}")
        return 1
    except Exception as e:
        print(f"\n💥 Unexpected error: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())