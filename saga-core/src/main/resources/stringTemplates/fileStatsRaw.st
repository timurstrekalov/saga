fileStatsRaw(fileStats) ::= <<
SF:$fileStats.relativeName$
$fileStats.executableLineCoverageRecords:lineCoverageDataRaw(); separator="\n"$
end_of_record
>>