#!/usr/bin/env python3
"""
Dependency analyzer for the repository.
Provides detailed dependency analysis and cycle detection.
"""
import json
import pathlib
from typing import Dict, List, Set, Tuple
from collections import defaultdict, deque


class DependencyAnalyzer:
    def __init__(self, repo_map_path: str):
        self.repo_map_path = pathlib.Path(repo_map_path)
        self.modules = {}
        self.load_repo_map()
    
    def load_repo_map(self):
        """Load repository map data"""
        with open(self.repo_map_path) as f:
            data = json.load(f)
        
        for module in data["modules"]:
            self.modules[module["name"]] = module
    
    def analyze_dependencies(self) -> Dict:
        """Perform comprehensive dependency analysis"""
        analysis = {
            "dependency_graph": self._build_dependency_graph(),
            "cycles": self._find_all_cycles(),
            "layers": self._analyze_layers(),
            "fanin_fanout": self._calculate_fanin_fanout(),
            "critical_path": self._find_critical_paths(),
            "suggestions": self._generate_suggestions()
        }
        return analysis
    
    def _build_dependency_graph(self) -> Dict[str, List[str]]:
        """Build directed dependency graph"""
        graph = defaultdict(list)
        
        for module_name, module in self.modules.items():
            for dep in module.get("deps_internal", []):
                graph[module_name].append(dep)
        
        return dict(graph)
    
    def _find_all_cycles(self) -> List[List[str]]:
        """Find all dependency cycles using DFS"""
        graph = self._build_dependency_graph()
        cycles = []
        visited = set()
        rec_stack = set()
        
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
            if module not in visited:
                dfs(module, [])
        
        return cycles
    
    def _analyze_layers(self) -> Dict[str, List[str]]:
        """Analyze architectural layers using topological sort"""
        graph = self._build_dependency_graph()
        in_degree = defaultdict(int)
        
        # Calculate in-degrees
        for node in self.modules:
            in_degree[node] = 0
        
        for node in graph:
            for neighbor in graph[node]:
                in_degree[neighbor] += 1
        
        # Topological sort to determine layers
        layers = {}
        queue = deque([node for node, degree in in_degree.items() if degree == 0])
        layer = 0
        
        while queue:
            current_layer = []
            for _ in range(len(queue)):
                node = queue.popleft()
                current_layer.append(node)
                
                for neighbor in graph.get(node, []):
                    in_degree[neighbor] -= 1
                    if in_degree[neighbor] == 0:
                        queue.append(neighbor)
            
            layers[f"Layer {layer}"] = current_layer
            layer += 1
        
        return layers
    
    def _calculate_fanin_fanout(self) -> Dict[str, Dict[str, int]]:
        """Calculate fan-in and fan-out for each module"""
        graph = self._build_dependency_graph()
        fanout = {module: len(graph.get(module, [])) for module in self.modules}
        fanin = defaultdict(int)
        
        for module in graph:
            for dep in graph[module]:
                fanin[dep] += 1
        
        return {
            module: {
                "fanin": fanin.get(module, 0),
                "fanout": fanout[module]
            }
            for module in self.modules
        }
    
    def _find_critical_paths(self) -> List[List[str]]:
        """Find critical dependency paths (longest paths)"""
        graph = self._build_dependency_graph()
        
        def longest_path_from(start):
            visited = set()
            
            def dfs(node):
                if node in visited:
                    return []
                
                visited.add(node)
                max_path = [node]
                
                for neighbor in graph.get(node, []):
                    path = dfs(neighbor)
                    if len(path) + 1 > len(max_path):
                        max_path = [node] + path
                
                visited.remove(node)
                return max_path
            
            return dfs(start)
        
        paths = []
        for module in self.modules:
            path = longest_path_from(module)
            if len(path) > 2:  # Only include paths with dependencies
                paths.append(path)
        
        # Sort by length and return top 5
        paths.sort(key=len, reverse=True)
        return paths[:5]
    
    def _generate_suggestions(self) -> List[str]:
        """Generate architectural improvement suggestions"""
        suggestions = []
        
        cycles = self._find_all_cycles()
        if cycles:
            suggestions.append(f"Break {len(cycles)} dependency cycle(s) by introducing interfaces or extracting common code")
        
        fanin_fanout = self._calculate_fanin_fanout()
        high_fanout = [m for m, metrics in fanin_fanout.items() if metrics["fanout"] > 3]
        if high_fanout:
            suggestions.append(f"Consider splitting high fan-out modules: {', '.join(high_fanout)}")
        
        high_fanin = [m for m, metrics in fanin_fanout.items() if metrics["fanin"] > 3]
        if high_fanin:
            suggestions.append(f"High fan-in modules may be doing too much: {', '.join(high_fanin)}")
        
        return suggestions


def main():
    analyzer = DependencyAnalyzer("repo-map.json")
    analysis = analyzer.analyze_dependencies()
    
    # Output detailed analysis
    print("## Dependency Analysis Report")
    print()
    
    print("### Architectural Layers")
    for layer, modules in analysis["layers"].items():
        print(f"**{layer}:** {', '.join(modules)}")
    print()
    
    print("### Fan-in/Fan-out Analysis")
    for module, metrics in analysis["fanin_fanout"].items():
        print(f"- **{module}**: Fan-in={metrics['fanin']}, Fan-out={metrics['fanout']}")
    print()
    
    if analysis["cycles"]:
        print("### Dependency Cycles")
        for i, cycle in enumerate(analysis["cycles"], 1):
            print(f"{i}. {' → '.join(cycle)}")
        print()
    
    if analysis["critical_path"]:
        print("### Critical Dependency Paths")
        for i, path in enumerate(analysis["critical_path"], 1):
            print(f"{i}. {' → '.join(path)} (length: {len(path)})")
        print()
    
    if analysis["suggestions"]:
        print("### Improvement Suggestions")
        for suggestion in analysis["suggestions"]:
            print(f"- {suggestion}")
    
    # Save detailed analysis
    output_file = pathlib.Path("dependency-analysis.json")
    with open(output_file, 'w') as f:
        json.dump(analysis, f, indent=2)
    
    print(f"\nDetailed analysis saved to {output_file}")


if __name__ == "__main__":
    main()