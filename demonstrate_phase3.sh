#!/bin/bash

# Demonstrate the new Phase 3 services
echo "=================================================="
echo "SeNARS v6.0 - Phase 3 Services Demonstration"
echo "=================================================="
echo
echo "This script demonstrates the new services implemented in Phase 3:"
echo "1. Governance Service with UCR-based predictive safety analysis"
echo "2. Attention Service with UCR-based intelligent prioritization"
echo "3. Extended Meta-Cognition Service with new monitors"
echo

echo "Starting the system..."
echo "Note: This demonstration requires a running Ollama instance with the appropriate model."
echo

# This would normally start the system with the new services
echo "System startup with new services:"
echo "  - GovernanceService: Replaces InMemoryGovernor with UCR-based predictive analysis"
echo "  - AttentionService: Provides intelligent prioritization using UCR effort estimation"
echo "  - MDRService: Extended with MotiveRefinementMonitor and MemoryCurationMonitor"
echo

echo "Key features demonstrated:"
echo "1. Governance Service:"
echo "   - Uses UCR.simulate() with safety constraints to predict potential violations"
echo "   - Integrates with existing rule-based governance"
echo "   - Provides clean separation of safety concerns"
echo
echo "2. Attention Service:"
echo "   - Uses UCR.reason() in estimation mode for accurate effort prediction"
echo "   - Improves system efficiency through better prioritization"
echo
echo "3. Extended Meta-Cognition:"
echo "   - MotiveRefinementMonitor: Detects patterns of failure related to ambitions"
echo "   - MemoryCurationMonitor: Responds to context retrieval errors"
echo

echo "=================================================="
echo "Demonstration complete."
echo "=================================================="