#!/bin/bash

# Exit on any error
set -e


# Install python if missing
if ! command -v python3 &> /dev/null; then
    echo "python3 not found. Installing..."
    sudo apt update
    sudo apt install python3 -y
fi

# Install pip if missing
if ! command -v pip3 &> /dev/null; then
    echo "pip3 not found. Installing..."
    sudo apt update
    sudo apt install python3-pip -y
fi

# Install curl if missing
if ! command -v curl &> /dev/null; then
    echo "curl not found. Installing..."
    sudo apt update
    sudo apt install curl -y
fi

# Install Poetry if missing
if ! command -v poetry &> /dev/null; then
    echo "Poetry not found. Installing..."
    curl -sSL https://install.python-poetry.org | python3 -

    # Add poetry to PATH for the current shell
    export PATH="$HOME/.local/bin:$PATH"

    # Add it permanently (optional)
    if ! grep -q 'export PATH="$HOME/.local/bin:$PATH"' ~/.bashrc; then
        echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
    fi
fi

# Ensure poetry is available
if ! command -v poetry &> /dev/null; then
    echo "Poetry still not available. Try restarting your terminal or sourcing ~/.bashrc"
    exit 1
fi

# Initialize poetry project if needed
if [ ! -f "pyproject.toml" ]; then
    echo "Initializing poetry project..."
    poetry init --name "krippendorff-project" --description "Analysis tool" --author "Your Name" --python "3.12.3" -n
fi

# Add required dependencies
echo "Adding dependencies..."
poetry add numpy scipy matplotlib seaborn krippendorff

poetry lock

# Run the script
echo "Running descriptive_statistics.py..."
poetry run python descriptive_statistics.py

echo "Running krippendorff_analysis.py..."
poetry run python krippendorff_analysis.py

echo "Running statistical_methods.py..."
poetry run python statistical_methods.py