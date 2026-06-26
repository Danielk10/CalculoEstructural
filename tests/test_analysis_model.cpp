#include "AnalysisModel.hpp"
#include <iostream>
#include <assert.h>

void testAnalysisModel() {
    FEA::AnalysisModel model;
    
    // Add Nodes
    model.nodes[1] = {1, 0.0, 0.0, 0.0};
    model.nodes[2] = {2, 1.0, 0.0, 0.0};
    
    // Add Element (B31 Beam)
    FEA::Element el;
    el.id = 1;
    el.type = "B31";
    el.nodeIds = {1, 2};
    model.elements[1] = el;
    
    // Add Material
    FEA::Material mat = {"Steel", 210000.0, 0.3, 7850.0};
    model.materials.push_back(mat);
    
    // Add Constraint (Fixed at node 1)
    FEA::BoundaryCondition bc;
    bc.nodeId = 1;
    bc.dofs = {1, 2, 3, 4, 5, 6};
    bc.value = 0.0;
    model.constraints.push_back(bc);
    
    // Add Load (Point load at node 2)
    FEA::Load load = {2, 0.0, -10.0, 0.0};
    model.loads.push_back(load);
    
    std::string inp = model.toInpString();
    std::cout << "--- Generated INP ---\n" << inp << "\n---------------------\n";
    
    assert(inp.find("*NODE") != std::string::npos);
    assert(inp.find("*ELEMENT, TYPE=B31") != std::string::npos);
    assert(inp.find("*CLOAD") != std::string::npos);
    
    std::cout << "AnalysisModel Test Passed!\n";
}

int main() {
    try {
        testAnalysisModel();
        return 0;
    } catch (const std::exception& e) {
        std::cerr << "Test failed with exception: " << e.what() << "\n";
        return 1;
    }
}
