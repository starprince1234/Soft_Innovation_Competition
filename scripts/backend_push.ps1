param(
  [string]$DockerHubUser = 'starprince123',
  [string]$Version = '0.1.0'
)

$ErrorActionPreference = 'Stop'

Write-Host "Tagging images to ${DockerHubUser}/jigu-java:${Version} and ${DockerHubUser}/jigu-python:${Version}"

docker tag jigu-java:$Version ${DockerHubUser}/jigu-java:$Version
docker tag jigu-python:$Version ${DockerHubUser}/jigu-python:$Version

docker push ${DockerHubUser}/jigu-java:$Version
docker push ${DockerHubUser}/jigu-python:$Version

Write-Host 'Push completed.'
