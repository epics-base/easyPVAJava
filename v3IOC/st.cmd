
epicsEnvSet("EPICS_CA_MAX_ARRAY_BYTES","450000")

## Load record instances
dbLoadRecords "test.db"

iocInit
