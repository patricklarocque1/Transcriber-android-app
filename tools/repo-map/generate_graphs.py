#!/usr/bin/env python3
"""
Graph generator for repository visualization.
Creates various types of dependency and architecture graphs.
"""
import json
import pathlib
from typing import Dict, List, Set


class GraphGenerator:
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
    
    def generate_all_graphs(self) -> Dict[str, str]:
        """Generate all types of graphs"""
        return {
            "module_dependencies": self.generate_module_dependency_graph(),
            "layered_architecture": self.generate_layered_graph(),
            "technology_stack": self.generate_technology_graph(),
            "api_surface": self.generate_api_graph()
        }
    
    def generate_module_dependency_graph(self) -> str:
        """Generate Mermaid graph showing module dependencies"""
        lines = ["graph TD"]
        
        # Add nodes with styling based on module type
        for module_name, module in self.modules.items():
            safe_name = self._safe_name(module_name)
            kind = module.get("kind", "library")
            
            if kind == "service":
                lines.append(f"  {safe_name}[{module_name}]:::service")
            elif kind == "library":
                lines.append(f"  {safe_name}[{module_name}]:::library")
            elif kind == "package":
                lines.append(f"  {safe_name}[{module_name}]:::package")
            else:
                lines.append(f"  {safe_name}[{module_name}]")
        
        # Add edges
        for module_name, module in self.modules.items():
            safe_name = self._safe_name(module_name)
            for dep in module.get("deps_internal", []):
                safe_dep = self._safe_name(dep)
                lines.append(f"  {safe_name} --> {safe_dep}")
        
        # Add styling
        lines.extend([
            "",
            "  classDef service fill:#e1f5fe,stroke:#0277bd,stroke-width:2px",
            "  classDef library fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px",
            "  classDef package fill:#e8f5e8,stroke:#388e3c,stroke-width:2px"
        ])
        
        return "\n".join(lines)
    
    def generate_layered_graph(self) -> str:
        """Generate graph showing architectural layers"""
        # Simple layering based on dependencies
        layers = self._calculate_layers()
        
        lines = ["graph TB"]
        
        # Create subgraphs for each layer
        for layer_num, modules in layers.items():
            lines.append(f"  subgraph Layer{layer_num} [\"Layer {layer_num}\"]")
            for module in modules:
                safe_name = self._safe_name(module)
                lines.append(f"    {safe_name}[{module}]")
            lines.append("  end")
        
        # Add dependencies between layers
        for module_name, module in self.modules.items():
            safe_name = self._safe_name(module_name)
            for dep in module.get("deps_internal", []):
                safe_dep = self._safe_name(dep)
                lines.append(f"  {safe_name} --> {safe_dep}")
        
        return "\n".join(lines)
    
    def generate_technology_graph(self) -> str:
        """Generate graph showing technology relationships"""
        lines = ["graph LR"]
        
        # Group modules by technology
        tech_groups = {}
        for module_name, module in self.modules.items():
            language = module.get("language", "unknown")
            if language not in tech_groups:
                tech_groups[language] = []
            tech_groups[language].append(module_name)
        
        # Create subgraphs for each technology
        for tech, modules in tech_groups.items():
            safe_tech = self._safe_name(tech)
            lines.append(f"  subgraph {safe_tech} [\"{tech.title()}\"]")
            for module in modules:
                safe_name = self._safe_name(module)
                lines.append(f"    {safe_name}[{module}]")
            lines.append("  end")
        
        return "\n".join(lines)
    
    def generate_api_graph(self) -> str:
        """Generate graph showing API relationships"""
        lines = ["graph TD"]
        
        # Show modules and their key APIs
        for module_name, module in self.modules.items():
            safe_name = self._safe_name(module_name)
            api = module.get("public_api", {})
            
            # Main module node
            lines.append(f"  {safe_name}[{module_name}]:::module")
            
            # Add key classes as connected nodes
            classes = api.get("classes", [])[:3]  # Limit to top 3
            for cls in classes:
                safe_cls = self._safe_name(f"{module_name}_{cls}")
                lines.append(f"  {safe_cls}[{cls}]:::class")
                lines.append(f"  {safe_name} --> {safe_cls}")
        
        # Add styling
        lines.extend([
            "",
            "  classDef module fill:#e3f2fd,stroke:#1976d2,stroke-width:2px",
            "  classDef class fill:#fff3e0,stroke:#f57c00,stroke-width:1px"
        ])
        
        return "\n".join(lines)
    
    def _calculate_layers(self) -> Dict[int, List[str]]:
        """Calculate architectural layers using topological sort"""
        from collections import defaultdict, deque
        
        graph = defaultdict(list)
        in_degree = defaultdict(int)
        
        # Build graph and calculate in-degrees
        for module_name, module in self.modules.items():
            in_degree[module_name] = 0
        
        for module_name, module in self.modules.items():
            for dep in module.get("deps_internal", []):
                graph[module_name].append(dep)
                in_degree[dep] += 1
        
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
            
            if current_layer:
                layers[layer] = current_layer
                layer += 1
        
        return layers
    
    def _safe_name(self, name: str) -> str:
        """Convert name to safe identifier for Mermaid"""
        import re
        return re.sub(r'[^a-zA-Z0-9_]', '_', name)


def main():
    generator = GraphGenerator("repo-map.json")
    graphs = generator.generate_all_graphs()
    
    # Save graphs to files
    for graph_type, graph_content in graphs.items():
        filename = f"{graph_type}_graph.mmd"
        with open(filename, 'w') as f:
            f.write(graph_content)
        print(f"Generated {filename}")
    
    # Create a combined graph document
    with open("architecture_graphs.md", 'w') as f:
        f.write("# Architecture Graphs\n\n")
        
        for graph_type, graph_content in graphs.items():
            title = graph_type.replace('_', ' ').title()
            f.write(f"## {title}\n\n")
            f.write("```mermaid\n")
            f.write(graph_content)
            f.write("\n```\n\n")
    
    print("Generated architecture_graphs.md")


if __name__ == "__main__":
    main()