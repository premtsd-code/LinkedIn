# PowerShell script to fix OpenSearch permissions on Windows
# This script creates the necessary directories and sets appropriate permissions

Write-Host "Creating OpenSearch log directories..." -ForegroundColor Green

# Create directories if they don't exist
$directories = @(
    "logs\opensearch",
    "logs\opensearch-dashboards",
    "logs\opensearch-logstash"
)

foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force
        Write-Host "Created directory: $dir" -ForegroundColor Yellow
    } else {
        Write-Host "Directory already exists: $dir" -ForegroundColor Cyan
    }
}

# Set permissions for Everyone to have full control (Windows equivalent of chmod 777)
foreach ($dir in $directories) {
    try {
        $acl = Get-Acl $dir
        $accessRule = New-Object System.Security.AccessControl.FileSystemAccessRule("Everyone", "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
        $acl.SetAccessRule($accessRule)
        Set-Acl -Path $dir -AclObject $acl
        Write-Host "Set permissions for: $dir" -ForegroundColor Green
    } catch {
        Write-Host "Failed to set permissions for: $dir - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "OpenSearch permissions setup complete!" -ForegroundColor Green
Write-Host "You can now run: docker-compose -f docker-compose-new.yml up opensearch" -ForegroundColor Cyan
