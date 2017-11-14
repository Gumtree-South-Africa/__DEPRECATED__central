# Configurations
We keep per-tenant, per-environment configurations in this folder. The structure is `distribution/conf/<tenant>/<environment>`.

## docker
This is a collection of properties to run with Docker.

## prod
Production properties for given tenant.

Note that the production artifact will also be deployed to sandbox. 

## other (e.g. itqa)
Alternative sets of properties specific to each tenant.

# Migrations
For migrations a separate `distribution/conf/<tenant>/migration` folder can be created. The first tenant to completely migrate was Mobile.de. See https://github.corp.ebay.com/ecg-comaas/central/commit/01f5608bd3e67657351b8bc83310a43b77789fbc for all the migration-related properties.
