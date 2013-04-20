fileStatsRaw(fileStats) ::= <<
SF:$fileStats.relativePath$
$fileStats.executableLineCoverageRecords:lineCoverageDataRaw(); separator="\n"$
end_of_record
>>