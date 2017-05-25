## Tool to verify data for Riak to Cassandra migration 

### Run 

Package 
`bin/build.sh -T migration -P ebaykprodComaasprod`

Unpack the tar

cd to unpacked distribution/ folder

Run 
`java -jar -DconfDir=conf/ lib/difftool-<githash>.jar`

for available options

For Riak to Cassandra core conversation migration verification on the given time slice run 
`java -DconfDir=conf/ -DlogDir=log/ -jar lib/difftool-<githash>.jar -v -rc -r2c -what conv -startDate 24-05-2017T22:00 -endDate 24-05-2017T23:00`

For Cassandra to Riak postbox verification on the given time slice run 
`java -DconfDir=conf/ -DlogDir=log/ -jar lib/difftool-<githash>.jar -v -rc -c2r -what mbox -startDate 04-09-2016T10:55 -endDate 04-09-2016T11:02`

Accurate Cassandra to Riak verification is only possible once all the data has been migrated due to Riak not strictly adhering to time bound.
 
To avoid non-sense results make sure that diffTool options corresponds to the options used for historic migration. 