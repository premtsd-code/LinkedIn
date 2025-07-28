#!/bin/bash
# Script to fix OpenSearch permissions on Linux/Mac
# This script creates the necessary directories and sets appropriate permissions

echo "Creating OpenSearch log directories..."

# Create directories if they don't exist
directories=(
    "logs/opensearch"
    "logs/opensearch-dashboards"
    "logs/opensearch-logstash"
)

for dir in "${directories[@]}"; do
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        echo "Created directory: $dir"
    else
        echo "Directory already exists: $dir"
    fi
done

# Set ownership to user 1000:1000 (opensearch user in container)
# and permissions to 755
for dir in "${directories[@]}"; do
    # Set permissions to allow read/write/execute for owner and group
    chmod 755 "$dir"
    echo "Set permissions for: $dir"
    
    # If running as root, also set ownership
    if [ "$EUID" -eq 0 ]; then
        chown -R 1000:1000 "$dir"
        echo "Set ownership for: $dir"
    fi
done

echo "OpenSearch permissions setup complete!"
echo "You can now run: docker-compose -f docker-compose-new.yml up opensearch"
