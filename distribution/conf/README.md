# Configurations

We keep per-tenant, per-environment configurations in this folder. The structure is `distribution/conf/<tenant>/<environment>`.

## noenv

Empty folder used by the build system to indicate that properties managed/provided externally .
  
## local

This is a set of properties to run comaas against locally run comaas-vagrant instance.   

## docker

This is a collection of properties to run with Docker.

## prod | sandbox

Producation and sandbox properties for given tenant.

## other (e.g. itqa)

Alternative sets of properties specific to each tenant.

# Migrations

For migrations a separate `distribution/conf/<tenant>/migration` folder can be created. The first tenant to completely migrate was Mobile.de. See https://gerrit.ecg.so/#/c/55152/ for all the migration-related properties.

