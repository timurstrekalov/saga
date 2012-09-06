fileStatsRaw(fileStats) ::= <<
SF:$fileStats.fullName$
$fileStats.executableLineCoverageRecords:lineCoverageDataRaw(); separator="\n"$
end_of_record
>>